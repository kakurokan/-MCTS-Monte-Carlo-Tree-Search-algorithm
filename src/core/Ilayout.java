package core;

import java.util.List;

/**
 * Representa uma configuração (estado) de um problema de pesquisa em espaço de
 * estados, definida pelos movimentos legais que permitem gerar os seus sucessores,
 * pelo custo de transição e por uma heurística até à configuração objetivo.
 *
 * @inv o custo de transição (getK()) têm que ser maior que zero
 */
public interface Ilayout {

    /**
     * Gera as configurações alcançáveis a partir desta com um único movimento legal.
     *
     * @return Lista de configurações sucessoras. Vazia se não existirem movimentos legais.
     * @pre this != null
     * @pos O resultado nunca é nulo.
     * @pos Cada elemento do resultado é alcançável a partir desta configuração com um único movimento legal.
     * @pos Esta configuração não é modificada.
     */
    List<Ilayout> children();

    /**
     * Verifica se esta configuração corresponde à configuração objetivo indicada.
     *
     * @param l A configuração objetivo a comparar com esta. Não pode ser nula.
     * @return {@code true} se esta configuração for igual a {@code l}; {@code false} caso contrário.
     * @pre l != null
     * @pos Esta configuração e {@code l} não são modificadas.
     */
    boolean isGoal(Ilayout l);

    /**
     * Devolve o custo de transitar para esta configuração a partir da sua antecessora.
     * Neste puzzle, cada movimento tem custo 1.
     *
     * @return O custo do movimento que originou esta configuração.
     * @pre this != null
     * @pos getK() = 1
     */
    double getK();

    /**
     * Estima o custo desta configuração até à configuração objetivo.
     *
     * @param goal A configuração objetivo. Não pode ser nula.
     * @return O valor da heurística (quanto menor, mais próximo do objetivo).
     * @pre goal != null
     * @pos getHeuristic(goal) >= 0
     * @pos Esta configuração e {@code goal} não são modificadas.
     */
    double getHeuristic(Ilayout goal);
}
