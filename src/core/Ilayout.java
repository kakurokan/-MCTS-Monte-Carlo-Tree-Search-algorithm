package core;

import java.util.List;
import java.util.Random;

/**
 * Representa uma configuração (estado) de um problema de pesquisa em espaço de
 * estados, definida pelos movimentos legais que permitem gerar os seus sucessores,
 * pelo custo de transição e por uma heurística até à configuração objetivo.
 *
 * @inv o custo de transição (getK()) tem que ser maior que zero
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
     * @param l A configuração objetivo a comparar com esta. Pode ser {@code null} se a
     *          implementação decidir o objetivo apenas a partir da própria configuração
     *          (como {@code BallSortLayout}).
     * @return {@code true} se esta configuração for igual a {@code l} (ou satisfizer o objetivo
     *         do problema); {@code false} caso contrário.
     * @pre l != null, exceto se a implementação ignorar {@code l}
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
     * @param goal A configuração objetivo. Pode ser {@code null} se a implementação a ignorar.
     * @return O valor da heurística (quanto menor, mais próximo do objetivo).
     * @pre goal != null, exceto se a implementação ignorar {@code goal}
     * @pos getHeuristic(goal) >= 0
     * @pos Esta configuração e {@code goal} não são modificadas.
     */
    double getHeuristic(Ilayout goal);

    /**
     * Gera e devolve uma configuração sucessora válida de forma aleatória.
     *
     * @param rnd   Gerador de números aleatórios a utilizar para escolher a transição.
     * @param avoid Configuração a evitar (tipicamente o estado pai), para prevenir
     *              ciclos de tamanho 2 (ping-pong). Pode ser {@code null}.
     * @return Um estado filho aleatório, ou {@code null} se não houver movimentos válidos.
     */
    Ilayout randomChild(Random rnd, Ilayout avoid);
}