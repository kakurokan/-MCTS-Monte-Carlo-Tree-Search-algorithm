package search;

import core.Ilayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementa a fase de simulação do MCTS. A partir de uma configuração, faz N
 * simulações (rollouts) com movimentos aleatórios e devolve a de menor custo,
 * que corresponde a sim(i). Cada rollout termina ao chegar à configuração
 * objetivo, ao ficar sem movimentos (beco sem saída) ou ao atingir o custo
 * máximo Cmax; nos dois últimos casos o rollout vale Cmax. Os rollouts nunca
 * desfazem o movimento imediatamente anterior.
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
     * @param cMax Custo máximo de um rollout; ao atingi-lo, o rollout é cortado.
     * @param rnd  Gerador de números aleatórios, criado fora para controlar a semente
     *             e tornar os resultados reproduzíveis. Não pode ser nulo.
     * @throws IllegalArgumentException se n &lt; 1 ou cMax &lt;= 0.
     * @pre rnd != null
     * @pos Os parâmetros ficam fixos durante toda a vida do simulador.
     */
    public Simulator(int n, double cMax, Random rnd) {
        if(n<=0)  throw new IllegalArgumentException("n tem de ser n >=1");
        if(cMax<=0)  throw new IllegalArgumentException("cMax tem de ser n > 0");
        this.n = n;
        this.cMax = cMax;
        this.rnd = rnd;
    }

    /**
     * Faz n rollouts a partir de uma configuração e devolve o de menor custo.
     *
     * @param start A configuração onde começa a simulação. Não pode ser nula.
     * @param goal  A configuração objetivo. Não pode ser nula.
     * @return O resultado de menor custo entre os n rollouts; custo Cmax e caminho
     *         nulo se nenhum rollout chegou ao objetivo.
     * @pre start != null
     * @pre goal != null
     * @pos O resultado nunca é nulo.
     * @pos 0 &lt;= resultado.getCost() &lt;= cMax
     * @pos As configurações start e goal não são modificadas.
     */
    public SimResult simulate(Ilayout start,  Ilayout goal) {
        SimResult result = new SimResult(null, cMax);
        for(int i=0; i<n;i++){
            SimResult temp = this.rollout(start,goal);
            boolean cheaper = temp.getCost() <  result.getCost();
            boolean sameCostSolved =  temp.getCost() == result.getCost() && temp.isSolved() && !result.isSolved();
            if(cheaper || sameCostSolved){
                result = temp;
            }
        }
        return result;
    }

    /**
     * Faz um único rollout: aplica movimentos aleatórios a partir de start, sem
     * desfazer o movimento anterior, até chegar ao objetivo, ficar sem movimentos
     * ou atingir o custo cMax.
     *
     * @param start A configuração onde começa o rollout. Não pode ser nula.
     * @param goal  A configuração objetivo. Não pode ser nula.
     * @return O custo e o caminho até ao objetivo, se o rollout o alcançou;
     *         custo cMax e caminho nulo caso contrário.
     * @pre start != null
     * @pre goal != null
     * @pre children() devolve listas modificáveis (são filtradas com removeIf).
     * @pos O resultado nunca é nulo.
     * @pos As configurações start e goal não são modificadas.
     */
    private SimResult rollout(Ilayout start, Ilayout goal) {
        List<Ilayout> path = new ArrayList<>();
        Ilayout current = start, prev = null;
        double cost = 0;

        while(cost < cMax){
            if(current.isGoal(goal)) {return new SimResult(path,cost);}
            List<Ilayout> succ = current.children();

            if(prev !=null){
                final Ilayout previous = prev;
                succ.removeIf(s -> s.equals(previous));
            }
            if(succ.isEmpty()){
                return new SimResult(null,cMax);
            }
            prev = current;
            current = succ.get(rnd.nextInt(succ.size()));

            cost+= current.getK();
            path.add(current);
        }

        if(cost <= cMax && current.isGoal(goal)){
            return new SimResult(path,cMax);
        }
        return new SimResult(null,cMax);
    }
}