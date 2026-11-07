package search;

import core.Ilayout;

import java.util.*;

/**
 * Implementa a fase de simulação do MCTS. A partir de uma configuração, faz N
 * simulações (rollouts) com movimentos escolhidos por uma política e-greedy e devolve a de menor custo,
 * que corresponde a sim(i).
 * <p>
 * Cada rollout termina numa de três situações:
 * <ul>
 *   <li>Chega à configuração objetivo;</li>
 *   <li>Fica sem movimentos válidos (beco);</li>
 *   <li>O próximo movimento faria o custo acumulado ultrapassar Cmax.</li>
 * </ul>
 * Nos dois últimos casos, o rollout vale Cmax.
 * <p>
 * Os rollouts nunca desfazem o movimento imediatamente anterior. Se voltarem a uma
 * configuração já visitada, o ciclo é eliminado do caminho, e o custo devolvido é o
 * do caminho sem ciclos. A truncagem em Cmax conta todos os movimentos aplicados,
 * incluindo quais foram eliminados em ciclos, o que garante que cada rollout termina.
 *
 * @inv n >= 1
 * @inv cMax > 0
 * @inv rnd != null
 */
public class Simulator {
    private final int n;
    private final double cMax;
    private final Random rnd;
    private final double epsilon;

    /**
     * Cria um simulador com rollouts totalmente aleatórios (equivale a epsilon = 1).
     *
     * @param n    Número de rollouts feitos em cada chamada a {@link #simulate}.
     * @param cMax Custo máximo de um rollout. Um rollout cujo próximo movimento faria
     *             o custo acumulado ultrapassar este valor é cortado e vale cMax.
     * @param rnd  Gerador de números aleatórios, criado fora para controlar a semente
     *             e tornar os resultados reproduzíveis. Não pode ser nulo.
     * @throws IllegalArgumentException se n &lt; 1 ou cMax &lt;= 0.
     * @pre rnd != null
     * @pos Os parâmetros ficam fixos durante toda a vida do simulador.
     */
    public Simulator(int n, double cMax, Random rnd) {
        this(n, cMax, rnd, 1.0);
    }

    /**
     * Cria um simulador com rollouts ε-greedy guiados pela heurística: em cada passo,
     * com probabilidade {@code epsilon} faz um movimento aleatório; caso contrário escolhe
     * o sucessor com menor {@link Ilayout#getHeuristic} (empates desfeitos ao acaso).
     *
     * @param n       Número de rollouts feitos em cada chamada a {@link #simulate}.
     * @param cMax    Custo máximo de um rollout.
     * @param rnd     Gerador de números aleatórios. Não pode ser nulo.
     * @param epsilon Probabilidade de um passo ser aleatório, em [0, 1]. Com 1, os
     *                rollouts são totalmente aleatórios.
     * @throws IllegalArgumentException se n &lt; 1, cMax &lt;= 0 ou epsilon fora de [0, 1].
     * @pre rnd != null
     */
    public Simulator(int n, double cMax, Random rnd, double epsilon) {
        if (n <= 0) throw new IllegalArgumentException("n tem de ser n >=1");
        if (cMax <= 0) throw new IllegalArgumentException("cMax tem de ser n > 0");
        if(Double.isNaN(epsilon))  throw new IllegalArgumentException("epsilon não pode ser NaN");
        if (epsilon < 0 || epsilon > 1) throw new IllegalArgumentException("epsilon tem de estar em [0, 1]");
        if (rnd == null) throw new IllegalArgumentException("rnd não pode ser nulo");
        this.n = n;
        this.cMax = cMax;
        this.rnd = rnd;
        this.epsilon = epsilon;
    }

    /**
     * Faz n rollouts a partir de uma configuração e devolve o de menor custo.
     * Em caso de empate no custo, prefere um rollout que tenha chegado ao objetivo.
     *
     * @param start A configuração onde começa a simulação. Não pode ser nula.
     * @param goal  A configuração objetivo. Não pode ser nula.
     * @return O resultado de menor custo entre os n rollouts; custo Cmax e caminho
     * nulo se nenhum rollout chegou ao objetivo.
     * @pre start != null
     * @pre goal != null
     * @pos O resultado nunca é nulo.
     * @pos 0 &lt;= resultado.getCost() &lt;= cMax
     * @pos Se resultado.isSolved(), o caminho não tem configurações repetidas.
     * @pos As configurações start e goal não são modificadas.
     */
    public SimResult simulate(Ilayout start, Ilayout goal) {
        SimResult result = new SimResult(null, cMax);
        for (int i = 0; i < n; i++) {
            SimResult temp = this.rollout(start, goal);
            boolean cheaper = temp.getCost() < result.getCost();
            boolean sameCostSolved = temp.getCost() == result.getCost() && temp.isSolved() && !result.isSolved();
            if (cheaper || sameCostSolved) {
                result = temp;
            }
        }
        return result;
    }

    /**
     * Faz um único rollout: aplica movimentos a partir de start, sem
     * desfazer o movimento anterior, até chegar ao objetivo, ficar sem movimentos
     * ou o próximo movimento fazer o custo acumulado ultrapassar cMax.
     * <p>
     * Em cada passo, o sucessor é obtido pela política epsilon-greedy, que opta por um estado aleatório
     * usando {@link Ilayout#randomChild} ou pelo estado de menor heurística. Ao longo do rollout
     * é mantido o caminho sem ciclos, juntamente com a posição de cada configuração nesse caminho.
     * Quando o rollout chega a uma configuração que já está no caminho, todas as configurações
     * posteriores a ela são removidas. O custo devolvido é a soma dos custos do
     * caminho resultante.
     *
     * @param start A configuração onde começa o rollout. Não pode ser nula.
     * @param goal  A configuração objetivo. Não pode ser nula.
     * @return O custo e o caminho sem ciclos até ao objetivo (sem incluir start), se o
     *         rollout o alcançou; custo cMax e caminho nulo caso contrário.
     * @pre start != null
     * @pre goal != null
     * @pos O resultado nunca é nulo.
     * @pos 0 &lt;= resultado.getCost() &lt;= cMax
     * @pos Se resultado.isSolved(), o caminho não tem configurações repetidas nem contém start.
     * @pos As configurações start e goal não são modificadas.
     */
    private SimResult rollout(Ilayout start, Ilayout goal) {
        List<Ilayout> path = new ArrayList<>();            // caminho sem ciclos (sem incluir start)
        Map<Ilayout, Integer> position = new HashMap<>();  // configuração -> posição no caminho (start = 0)
        position.put(start, 0);
        Ilayout current = start;
        Ilayout previous = null;
        double spent = 0;  // custo de todos os movimentos aplicados, usado na truncagem em Cmax

        while (true) {
            if (current.isGoal(goal)) {
                double cost = 0;
                for (Ilayout l : path) cost += l.getK();
                return new SimResult(path, cost);
            }

            Ilayout next = nextMove(current, previous, goal, position);
            if (next == null) {
                return new SimResult(null, cMax);           // beco sem saída
            }
            if (spent + next.getK() > cMax) {
                return new SimResult(null, cMax);           // o próximo movimento ultrapassaria Cmax
            }

            previous = current;
            current = next;
            spent += current.getK();

            Integer p = position.get(current);
            if (p != null) {
                // voltou a uma configuração já visitada: o ciclo é eliminado do caminho
                for (int k = path.size() - 1; k >= p; k--) {
                    position.remove(path.remove(k));
                }
            } else {
                path.add(current);
                position.put(current, path.size());
            }
        }
    }

    /**
     * Escolhe o próximo movimento do rollout de acordo com a política epsilon-greedy.
     * Com probabilidade epsilon, sorteia um sucessor com randomChild (fase de exploração).
     * Caso contrário, escolhe o sucessor de menor heurística que ainda não esteja no caminho
     * (fase de explotação), com empates desfeitos ao acaso. Se todos os sucessores avaliados
     * já estiverem no caminho, reverte para um passo aleatório.
     *
     * @param current Configuração atual no rollout.
     * @param previous Configuração anterior, para prevenir ping-pong.
     * @param goal A configuração objetivo.
     * @param visited Mapa com as configurações já visitadas no caminho atual e as respetivas posições.
     * @return O sucessor escolhido, ou null se for um beco sem saída.
     */
    private Ilayout nextMove(Ilayout current, Ilayout previous, Ilayout goal,
                             Map<Ilayout, Integer> visited) {
        if (epsilon >= 1 || rnd.nextDouble() < epsilon) {
            return current.randomChild(rnd, previous);          // exploração
        }

        Ilayout best = null;
        double bestH = Double.POSITIVE_INFINITY;
        int ties = 0;
        for (Ilayout child : current.children()) {               // aproveitamento
            if (visited.containsKey(child)) continue;            // evita ciclos (inclui previous)
            double h = child.getHeuristic(goal);
            if (h < bestH) {
                bestH = h;
                best = child;
                ties = 1;
            } else if (h == bestH && rnd.nextInt(++ties) == 0) {
                best = child;                                    // empate escolhido uniformemente
            }
        }
        return best != null ? best : current.randomChild(rnd, previous);
    }
}