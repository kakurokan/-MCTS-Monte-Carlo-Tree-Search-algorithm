package search;

import core.Ilayout;

import java.util.*;

/**
 * Implementação do algoritmo Monte Carlo Tree Search (MCTS) para encontrar um caminho
 * de custo mínimo entre uma configuração inicial e uma configuração objetivo.
 * <p>
 * Em cada iteração:
 * <ol>
 *   <li><b>Seleção</b>: desce da raiz até uma folha, escolhendo em cada nível o filho
 *       não esgotado com maior UCB;</li>
 *   <li>se a folha for o objetivo, regista a solução, retropropaga custo 0 e marca-a como esgotada;</li>
 *   <li><b>Expansão</b>: cria os filhos da folha; se não houver filhos (profundidade máxima
 *       ou sem sucessores), retropropaga cMax e marca-a como esgotada;</li>
 *   <li><b>Simulação</b>: para cada filho, usa o {@link Simulator} para estimar o custo até ao objetivo;</li>
 *   <li><b>Retropropagação</b>: atualiza as visitas e o custo estimado do filho e dos seus antecessores.</li>
 * </ol>
 * A melhor solução encontrada (caminho na árvore mais, eventualmente, o caminho de um rollout)
 * é guardada ao longo das iterações. A pesquisa termina ao fim de maxIterations ou quando a
 * raiz fica esgotada (toda a árvore até maxDepth foi explorada).
 *
 * @inv maxIterations > 0, cMax > 0, maxDepth > 0, c >= 0
 * @inv fMax >= maxDepth + cMax
 * @inv Cada movimento tem custo 1 (getK() == 1), pelo que g <= maxDepth e a
 * recompensa fica em [0, 1] quando fMax >= maxDepth + cMax.
 */
public class MCTS {
    private final double cMax;
    private final int maxIterations;
    private final Simulator simulator;
    private final double fMax;
    private final double c;
    private final int maxDepth;
    private final boolean useExhausted;

    private State lastRoot;
    private double lastRawCost = Double.POSITIVE_INFINITY;
    private double lastFinalCost = Double.POSITIVE_INFINITY;

    /**
     * Cria uma instância do MCTS com os parâmetros indicados.
     *
     * @param cMax          Custo atribuído a uma simulação sem solução e custo estimado inicial
     *                      de cada nó. Tem de ser positivo e deve coincidir com o cMax do simulador.
     * @param maxIterations Número máximo de iterações do algoritmo. Tem de ser positivo.
     * @param simulator     O simulador usado para estimar o custo a partir de um nó. Não pode ser nulo.
     * @param fMax          Custo máximo usado para normalizar a recompensa. Tem de ser >= maxDepth + cMax.
     * @param c             Constante de exploração do UCB. Não pode ser negativa.
     * @param maxDepth      Profundidade máxima da árvore de pesquisa. Tem de ser positiva.
     * @throws IllegalArgumentException se algum dos parâmetros for inválido.
     * @pre maxIterations > 0 && cMax > 0 && maxDepth > 0 && c >= 0
     * @pre fMax >= maxDepth + cMax
     * @pre simulator != null
     */
    public MCTS(double cMax, int maxIterations, Simulator simulator, double fMax, double c, int maxDepth, boolean useExhausted) {
        if (maxIterations <= 0 || cMax <= 0 || maxDepth <= 0 || c < 0) {
            throw new IllegalArgumentException("parâmetros inválidos");
        }

        if (simulator == null) {
            throw new IllegalArgumentException("simulator não pode ser nulo");
        }

        if (fMax < maxDepth + cMax) {
            throw new IllegalArgumentException("fMax inválido. fMax deve ser maior ou igual à maxDepth + cMax");
        }

        this.cMax = cMax;
        this.maxIterations = maxIterations;
        this.simulator = simulator;
        this.fMax = fMax;
        this.c = c;
        this.maxDepth = maxDepth;
        this.useExhausted = useExhausted;
    }

    /**
     * Remove ciclos de uma sequência ordenada de configurações, eliminando os estados
     * entre duas ocorrências da mesma configuração.
     * <p>
     * Sempre que uma configuração já presente no caminho limpo volta a aparecer, todos
     * os elementos posteriores à sua primeira ocorrência mantida são removidos. A
     * configuração repetida não é adicionada novamente.
     *
     * @param ilayouts A sequência de configurações a limpar. Não pode ser nula.
     * @return Uma nova lista com os ciclos removidos, preservando a ordem dos elementos
     * restantes.
     * @pre ilayouts != null
     */
    private static List<Ilayout> removeCyles(List<Ilayout> ilayouts) {
        List<Ilayout> clean = new ArrayList<>();
        Map<Ilayout, Integer> pos = new HashMap<>();

        for (Ilayout l : ilayouts) {
            Integer p = pos.get(l);
            if (p != null) {
                for (int k = clean.size() - 1; k > p; k--) {
                    pos.remove(clean.remove(k));
                }
            } else {
                clean.add(l);
                pos.put(l, clean.size() - 1);
            }
        }

        return clean;
    }

    /**
     * Executa o MCTS a partir da configuração inicial, procurando o caminho de menor custo
     * até à configuração objetivo encontrado durante as iterações.
     * <p>
     * Uma solução pode ser encontrada de duas formas: um nó da árvore que é o objetivo
     * (custo g), ou um rollout bem-sucedido a partir de um filho (custo g + custo do rollout).
     * No segundo caso, o caminho devolvido é o caminho na árvore até ao filho seguido das
     * configurações percorridas pelo rollout ({@link SimResult#getPath()}).
     * <p>
     * Antes de ser devolvido, o caminho é limpo de ciclos e reconstruído com estados novos
     * (encadeados entre si), pelo que estes não pertencem à árvore de pesquisa.
     *
     * @param start A configuração inicial. Não pode ser nula.
     * @param goal  A configuração objetivo. Pode ser nula se a implementação de {@link Ilayout} a ignorar
     *              (como {@code BallSortLayout}).
     * @return Iterador sobre os estados do melhor caminho encontrado, sem configurações repetidas,
     * da raiz até ao objetivo, ou {@code null} se nenhuma solução tiver sido encontrada.
     * @pre start != null; goal != null, exceto se a implementação de Ilayout ignorar o objetivo
     * @pos Se o resultado não for nulo, o primeiro estado corresponde a {@code start} e o último satisfaz isGoal(goal).
     * @pos Se o resultado não for nulo, cada estado é sucessor do anterior por um único movimento legal.
     * @pos Se o resultado não for nulo, não há configurações repetidas no caminho.
     * @pos {@code start} e {@code goal} não são modificadas.
     * @pos Termina antes de maxIterations se a árvore ficar totalmente explorada (raiz esgotada).
     */
    public Iterator<State> solve(Ilayout start, Ilayout goal) {
        State root = new State(start, null, cMax);
        lastRoot = root;
        lastRawCost = Double.POSITIVE_INFINITY;
        lastFinalCost = Double.POSITIVE_INFINITY;

        double bestCost = Double.POSITIVE_INFINITY;
        State bestNode = null;
        List<Ilayout> bestTail = List.of();

        for (int i = 0; i < maxIterations; i++) {
            if (root.isExhausted()) break;

            State leaf = select(root);

            if (leaf.getLayout().isGoal(goal)) {
                if (leaf.getG() < bestCost) {
                    bestNode = leaf;
                    bestCost = leaf.getG();
                    bestTail = List.of();
                }

                backpropagate(leaf, 0);
                if (useExhausted) {
                    leaf.markExhausted();
                }
                continue;
            }

            List<State> children = leaf.expand(cMax, maxDepth);
            if (children.isEmpty()) {
                backpropagate(leaf, cMax);
                if (useExhausted) {
                    leaf.markExhausted();
                }
                continue;
            }

            for (State child : children) {
                SimResult result = simulator.simulate(child.getLayout(), goal);
                double total = child.getG() + result.getCost();
                if (result.isSolved() && total < bestCost) {
                    bestNode = child;
                    bestCost = total;
                    bestTail = result.getPath();
                }

                backpropagate(child, result.getCost());
            }
        }

        if (bestNode == null) {
            return null;
        }

        State end = bestNode;
        for (Ilayout l : bestTail) {
            end = new State(l, end, cMax);
        }

        LinkedList<State> path = new LinkedList<>();
        State atual = end;
        while (atual != null) {
            path.addFirst(atual);
            atual = atual.getFather();
        }

        List<Ilayout> ilayouts = new ArrayList<>();
        for (State s : path) {
            ilayouts.add(s.layout);
        }
        ilayouts = removeCyles(ilayouts);

        List<State> finalPath = new ArrayList<>();
        State prev = null;
        for (Ilayout l : ilayouts) {
            State s = new State(l, prev, cMax);
            finalPath.add(s);
            prev = s;
        }

        lastRawCost = bestCost;
        lastFinalCost = prev.getG();

        return finalPath.iterator();
    }

    /**
     * @return A raiz da árvore criada pelo último {@link #solve}, ou {@code null} se ainda não foi chamado.
     */
    public State getLastRoot() {
        return lastRoot;
    }

    /**
     * @return O custo da melhor solução do último {@link #solve} antes da remoção de ciclos,
     * ou {@code +infinito} se não houve solução.
     */
    public double getLastRawCost() {
        return lastRawCost;
    }

    /**
     * @return O custo do caminho devolvido pelo último {@link #solve} (após remoção de ciclos),
     * ou {@code +infinito} se não houve solução.
     */
    public double getLastFinalCost() {
        return lastFinalCost;
    }

    /**
     * Propaga o resultado de uma simulação desde o nó indicado até à raiz, incrementando
     * o número de visitas e atualizando o melhor custo estimado de cada antecessor.
     *
     * @param child   O nó a partir do qual foi feita a simulação. Não pode ser nulo.
     * @param simCost O custo estimado de {@code child} até ao objetivo: o custo do rollout,
     *                0 se {@code child} for o objetivo, ou cMax se não houver solução.
     * @pre child != null && simCost >= 0
     * @pos O número de visitas de {@code child} e de todos os seus antecessores é incrementado em 1.
     * @pos Para cada antecessor a, a.getSim() <= child.getG() - a.getG() + simCost.
     */
    public void backpropagate(State child, double simCost) {
        State atual = child;

        while (atual != null) {
            atual.addVisit();
            atual.updateSim(child.getG() - atual.getG() + simCost);
            atual = atual.getFather();
        }
    }

    /**
     * Desce na árvore a partir da raiz, escolhendo em cada nível o melhor filho segundo o UCB,
     * até atingir uma folha.
     *
     * @param root A raiz da árvore de pesquisa. Não pode ser nula.
     * @return A folha selecionada.
     * @pre root != null && !root.isExhausted()
     * @pos O resultado nunca é nulo.
     * @pos result.isLeaf() == true && !result.isExhausted()
     * @pos A árvore não é modificada.
     */
    public State select(State root) {
        State atual = root;
        while (!atual.isLeaf()) {
            atual = bestChild(atual);
        }
        return atual;
    }

    /**
     * Escolhe o filho de um nó a explorar: o filho não esgotado com maior valor UCB.
     * Em caso de empate, fica o primeiro na ordem de {@code getChildren()}.
     * Todos os filhos já foram visitados, pois cada filho é simulado e retropropagado
     * logo após a expansão.
     *
     * @param atual O nó cujos filhos são avaliados. Não pode ser nulo, folha nem esgotado.
     * @return O filho escolhido.
     * @throws IllegalStateException se todos os filhos estiverem esgotados (viola a invariante de esgotamento).
     * @pre atual != null && !atual.isLeaf() && !atual.isExhausted()
     * @pre Todos os filhos de {@code atual} têm getVisitCount() > 0.
     * @pos O resultado pertence a atual.getChildren() e não está esgotado.
     */
    private State bestChild(State atual) {
        State best = null;

        double bestValue = Double.NEGATIVE_INFINITY;

        for (State child : atual.getChildren()) {
            if (child.isExhausted()) continue;

            if (child.getVisitCount() == 0) return child;

            double value = child.ucb(fMax, c, atual.getVisitCount());

            if (value > bestValue) {
                best = child;
                bestValue = value;
            }
        }

        if (best == null) {
            throw new IllegalStateException("Nó não esgotado com todos os filhos esgotados");
        }

        return best;
    }

    /**
     * Representa um nó da árvore de pesquisa do MCTS, associado a uma configuração do problema,
     * guardando o custo acumulado desde a raiz, o número de visitas e o melhor custo
     * estimado (sim) obtido nas simulações a partir deste nó.
     *
     * @inv getG() >= 0 e getG() = 0 se e só se o nó for a raiz (porque getK() = 1)
     * @inv getVisitCount() >= 0
     * @inv depth = profundidade do pai + 1 (0 na raiz)
     * @inv Um nó não-folha está esgotado se e só se todos os seus filhos estiverem esgotados.
     */
    public static class State {
        private final Ilayout layout;
        private final State father;
        private final double g;
        private final List<State> children;
        private final int depth;
        private int visitCount;
        private double sim;
        private boolean exhausted = false;

        /**
         * Cria um nó da árvore.
         *
         * @param layout A configuração associada a este nó. Não pode ser nula.
         * @param father O nó pai, ou {@code null} se este nó for a raiz.
         * @param cmax   Valor inicial do custo estimado (sim) deste nó.
         * @pre layout != null && cmax > 0
         * @pos getG() = father.getG() + layout.getK(), ou 0 se father == null.
         * @pos getVisitCount() = 0 && isLeaf() == true && getSim() = cmax
         */
        public State(Ilayout layout, State father, double cmax) {
            this.layout = layout;
            this.father = father;
            this.sim = cmax;

            depth = (father == null) ? 0 : father.depth + 1;
            g = (father == null) ? 0.0 : father.g + layout.getK();

            children = new ArrayList<>();

            visitCount = 0;
        }

        /**
         * Devolve o custo acumulado desde a raiz até este nó.
         *
         * @return O custo g deste nó.
         * @pos getG() >= 0
         */
        public double getG() {
            return g;
        }

        /**
         * Incrementa o número de visitas deste nó.
         *
         * @pos getVisitCount() = old getVisitCount() + 1
         */
        public void addVisit() {
            visitCount++;
        }

        /**
         * Devolve a configuração associada a este nó.
         *
         * @return A configuração deste nó.
         * @pos O resultado nunca é nulo.
         */
        public Ilayout getLayout() {
            return layout;
        }

        /**
         * Devolve os filhos deste nó.
         *
         * @return Lista não modificável dos filhos. Vazia se o nó ainda não tiver sido expandido.
         * @pos O resultado nunca é nulo.
         */
        public List<State> getChildren() {
            return Collections.unmodifiableList(children);
        }

        /**
         * Devolve o melhor custo estimado, a partir deste nó até ao objetivo, obtido nas simulações.
         *
         * @return O valor sim deste nó; cMax se ainda não houve nenhuma simulação melhor.
         * @pos 0 <= getSim() <= cMax
         */
        public double getSim() {
            return sim;
        }

        /**
         * Devolve o número de vezes que este nó foi visitado.
         *
         * @return O número de visitas.
         * @pos getVisitCount() >= 0
         */
        public int getVisitCount() {
            return visitCount;
        }

        /**
         * Verifica se este nó é uma folha (não tem filhos).
         *
         * @return {@code true} se o nó não tiver filhos; {@code false} caso contrário.
         */
        public boolean isLeaf() {
            return children.isEmpty();
        }

        /**
         * Expande este nó, criando um filho para cada configuração sucessora, exceto a do pai.
         * Se a profundidade máxima tiver sido atingida, o nó não é expandido.
         *
         * @param cMax     Valor inicial do custo estimado dos novos filhos.
         * @param maxDepth Profundidade máxima permitida na árvore.
         * @return Lista não modificável dos filhos deste nó (vazia se não foi possível expandir).
         * @pre cMax > 0 && maxDepth > 0
         * @pre isLeaf(), para não duplicar filhos.
         * @pos Se depth >= maxDepth, os filhos não são alterados.
         * @pos Nenhum filho tem configuração igual à do pai deste nó.
         * @pos O resultado nunca é nulo.
         */
        public List<State> expand(double cMax, int maxDepth) {
            if (depth >= maxDepth) {
                return getChildren();
            }

            for (Ilayout l : layout.children()) {
                if (father == null || !l.equals(father.layout)) {
                    children.add(new State(l, this, cMax));
                }
            }
            return getChildren();
        }

        /**
         * Indica se não há mais nada a explorar abaixo deste nó.
         *
         * @return {@code true} se o nó for uma folha terminal (objetivo ou não expansível)
         * ou se todos os seus filhos estiverem esgotados; {@code false} caso contrário.
         */
        public boolean isExhausted() {
            return exhausted;
        }

        /**
         * Marca este nó como esgotado e propaga a marcação para cima: um antecessor
         * fica esgotado quando todos os seus filhos estão esgotados.
         *
         * @pos isExhausted() == true
         * @pos Todo o antecessor com todos os filhos esgotados fica esgotado.
         */
        public void markExhausted() {
            State atual = this;
            atual.exhausted = true;
            while (atual.father != null) {
                State pai = atual.father;
                for (State f : pai.children) {
                    if (!f.exhausted) return;
                }
                pai.exhausted = true;
                atual = pai;
            }
        }

        /**
         * Atualiza o custo estimado deste nó caso o candidato seja menor.
         *
         * @param candidateSim O novo custo estimado candidato.
         * @pos getSim() = min(old getSim(), candidateSim)
         */
        public void updateSim(double candidateSim) {
            if (candidateSim < sim) {
                sim = candidateSim;
            }
        }

        /**
         * Calcula a recompensa normalizada deste nó, com base no custo total estimado (g + sim).
         *
         * @param fMax Custo máximo usado para normalização. Tem de ser positivo.
         * @return 1 - (getG() + getSim()) / fMax (quanto maior, melhor).
         * @pre fMax > 0
         */
        public double reward(double fMax) {
            return 1 - (getG() + sim) / fMax;
        }

        /**
         * Calcula o valor UCB (Upper Confidence Bound) deste nó, combinando a recompensa
         * (aproveitamento) com um termo de exploração.
         *
         * @param fMax            Custo máximo usado para normalizar a recompensa.
         * @param c               Constante de exploração.
         * @param totalSimulation Número de visitas do nó pai.
         * @return reward(fMax) + c * sqrt(ln(totalSimulation) / getVisitCount()).
         * @pre fMax > 0 && c >= 0 && totalSimulation >= 1 && getVisitCount() > 0
         */
        public double ucb(double fMax, double c, int totalSimulation) {
            return reward(fMax) + c * Math.sqrt(Math.log(totalSimulation) / visitCount);
        }

        /**
         * Devolve o nó pai deste nó.
         *
         * @return O nó pai, ou {@code null} se este nó for a raiz.
         */
        public State getFather() {
            return father;
        }
    }


}
