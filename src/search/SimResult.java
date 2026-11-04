package search;

import core.Ilayout;

import java.util.List;

/**
 * Representa o resultado de uma simulação (rollout) do MCTS: o custo obtido e,
 * se a simulação chegou à configuração objetivo, a sequência de configurações
 * percorridas até lá. Uma simulação falhada (cortada em Cmax ou presa num beco
 * sem saída) tem caminho nulo e custo Cmax.
 *
 * @inv getCost() >= 0
 * @inv isSolved() == (getPath() != null)
 * @inv Se isSolved(), getPath() é uma lista não modificável.
 */
public class SimResult {
    private List<Ilayout> path;
    private double minCost;

    /**
     * Cria o resultado de uma simulação.
     *
     * @param path    Configurações percorridas depois da configuração inicial, por ordem
     *                e sem a incluir; {@code null} se a simulação não chegou ao objetivo.
     * @param minCost Custo da simulação, já truncado em Cmax.
     * @pre minCost >= 0
     * @pre path == null || nenhum elemento de path é nulo
     * @pos getCost() == minCost
     * @pos getPath() é uma cópia não modificável de path, ou {@code null} se path == null.
     * @pos A lista path recebida não é modificada.
     */
    public SimResult(List<Ilayout> path, double minCost) {
        this.path = (path == null) ? null : List.copyOf(path);
        this.minCost = minCost;
    }

    /**
     * Devolve as configurações percorridas pela simulação até à configuração objetivo.
     *
     * @return Lista não modificável das configurações percorridas, sem a inicial;
     *         {@code null} se a simulação não chegou ao objetivo.
     * @pos Este resultado não é modificado.
     */
    public List<Ilayout> getPath() {
        return path == null ? null : this.path;
    }

    /**
     * Devolve o custo da simulação.
     *
     * @return O custo acumulado até ao objetivo, ou Cmax se a simulação falhou.
     * @pos getCost() >= 0
     * @pos Este resultado não é modificado.
     */
    public double getCost() {
        return this.minCost;
    }

    /**
     * Verifica se a simulação chegou à configuração objetivo.
     *
     * @return {@code true} se a simulação resolveu o problema; {@code false} caso contrário.
     * @pos isSolved() == (getPath() != null)
     * @pos Este resultado não é modificado.
     */
    public boolean isSolved() {
        return this.path != null;
    }


}