package search;

import core.Ilayout;

import java.util.*;

/**
 * Implementa a fase de simulação do MCTS. A partir de uma configuração, faz N
 * simulações (rollouts) com movimentos aleatórios e devolve a de menor custo,
 * que corresponde a sim(i).
 * <p>
 * Cada rollout termina numa de três situações:
 * <ul>
 *   <li>chega à configuração objetivo;</li>
 *   <li>fica sem movimentos válidos (beco sem saída);</li>
 *   <li>o próximo movimento faria o custo acumulado ultrapassar Cmax.</li>
 * </ul>
 * Nos dois últimos casos, o rollout vale Cmax.
 * <p>
 * Os rollouts nunca desfazem o movimento imediatamente anterior. Se voltarem a uma
 * configuração já visitada, o ciclo é eliminado do caminho, e o custo devolvido é o
 * do caminho sem ciclos. A truncagem em Cmax conta todos os movimentos aplicados,
 * incluindo os que foram eliminados em ciclos, o que garante que cada rollout termina.
 *
 * @inv n >= 1
 * @inv cMax > 0
 * @inv rnd != null
 */
public class Simulator {
    private final int n;
    private final double cMax;
    private final Random rnd;

    /**
     * Cria um simulador com os parâmetros indicados.
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
        if (n <= 0) throw new IllegalArgumentException("n tem de ser n >=1");
        if (cMax <= 0) throw new IllegalArgumentException("cMax tem de ser n > 0");
        this.n = n;
        this.cMax = cMax;
        this.rnd = rnd;
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
     * Faz um único rollout: aplica movimentos aleatórios a partir de start, sem
     * desfazer o movimento anterior, até chegar ao objetivo, ficar sem movimentos
     * ou o próximo movimento fazer o custo acumulado ultrapassar cMax.
     * <p>
     * Ao longo do rollout é mantido o caminho sem ciclos, juntamente com a posição
     * de cada configuração nesse caminho. Quando o rollout chega a uma configuração
     * que já está no caminho, todas as configurações posteriores a ela são removidas.
     * O custo devolvido é a soma dos custos do caminho resultante.
     *
     * @param start A configuração onde começa o rollout. Não pode ser nula.
     * @param goal  A configuração objetivo. Não pode ser nula.
     * @return O custo e o caminho até ao objetivo, se o rollout o alcançou;
     * custo cMax e caminho nulo caso contrário.
     * @pre start != null
     * @pre goal != null
     * @pos O resultado nunca é nulo.
     * @pos 0 &lt;= resultado.getCost() &lt;= cMax
     * @pos Se resultado.isSolved(), o caminho não tem configurações repetidas nem contém start.
     * @pos As configurações start e goal não são modificadas.
     */
    private SimResult rollout(Ilayout start, Ilayout goal) {
        List<Ilayout> path = new ArrayList<>();
        Map<Ilayout, Integer> position = new HashMap<>();
        Ilayout current = start;
        position.put(current, 0);
        Ilayout previous = null;
        double spent = 0;

        while (true) {
            if (current.isGoal(goal)) {
                double cost = 0;
                for (Ilayout l : path) cost += l.getK();
                return new SimResult(path, cost);
            }
            if (spent >= cMax) {
                return new SimResult(null, cMax);
            }

            List<Ilayout> succ = current.children();
            int r = this.chooseMovement(succ, previous);
            if (r < 0) {
                return new SimResult(null, cMax);
            }
            if(spent + succ.get(r).getK()  > cMax){
                return new SimResult(null,cMax);
            }
            previous = current;
            current = succ.get(r);
            spent += current.getK();

            Integer p = position.get(current);
            if (p != null) {
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
     * Sorteia o índice de um sucessor, uniformemente entre os que não desfazem o
     * movimento anterior. Começa por sortear entre todos os sucessores. Se calhar a
     * configuração anterior, sorteia outro entre os restantes, com a mesma
     * probabilidade para todos. Assim, só é preciso uma comparação com
     * {@code equals}, em vez de uma por sucessor.
     *
     * @param succ     Sucessores da configuração atual. Não pode ser nula.
     * @param previous Configuração anterior, ou {@code null} no primeiro passo.
     * @return Índice do sucessor escolhido, ou -1 se não houver nenhum válido
     * (lista vazia ou o único sucessor é a configuração anterior).
     * @pos succ não é modificada.
     */

    private int chooseMovement(List<Ilayout> succ, Ilayout previous) {
        if (succ.isEmpty()) {
            return -1;
        }

        int r = rnd.nextInt(succ.size());

        if (succ.get(r).equals(previous)) {
            if (succ.size() == 1) return -1;
            int other = rnd.nextInt(succ.size() - 1);
            r = (other >= r) ? other + 1 : other;

        }
        return r;
    }

}