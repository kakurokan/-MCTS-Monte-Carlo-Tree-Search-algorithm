package puzzles;

import core.Ilayout;

import java.util.*;

/**
 * Configuração do puzzle Ball Sort: uma lista de tubos (pilhas) de bolas
 * representadas por caracteres, todos com a mesma capacidade. Um movimento
 * transfere a bola do topo de um tubo para outro tubo vazio ou cujo topo
 * tenha a mesma cor. O objetivo é que cada tubo fique vazio ou cheio com uma só cor.
 *
 * @inv getK() == 1
 * @inv nenhum tubo excede maxTubeSize bolas
 */
public class BallSortLayout implements Ilayout, Cloneable {
    /** Custo de um movimento (sempre 1). */
    private final int K;
    /** Capacidade de cada tubo (número de linhas da configuração). */
    private final int maxTubeSize;
    /** Tubos da configuração; cada pilha tem a base no índice 0. */
    private List<Stack<Character>> tubes;
    /** Hash em cache; 0 significa ainda não calculado. */
    private int hash;

    /**
     * Cria uma configuração a partir da sua representação textual.
     *
     * @param configuration uma linha por nível, do topo para a base, com as bolas separadas
     *                      por espaços; {@code 'E'} representa uma posição vazia.
     * @pre configuration != null e todas as linhas têm o mesmo número de colunas
     * @pos o número de tubos é o número de colunas e a capacidade de cada tubo é o número de linhas.
     */
    public BallSortLayout(String configuration) {
        K = 1;
        hash = 0;
        tubes = new ArrayList<>();
        String[] lines = configuration.trim().split("\n");
        maxTubeSize = lines.length;

        int numTubes = lines[0].split(" ").length;

        for (int i = 0; i < numTubes; i++) {
            tubes.add(new Stack<>());
        }

        for (int i = maxTubeSize - 1; i >= 0; i--) {
            String[] line = lines[i].split(" ");
            for (int j = 0; j < numTubes; j++) {
                char ball = line[j].charAt(0);
                if (ball != 'E') {
                    tubes.get(j).push(ball);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Um sucessor resulta de mover a bola do topo de um tubo (não vazio e ainda não
     * resolvido) para outro tubo que não esteja cheio e que esteja vazio ou tenha a mesma cor no topo.
     */
    @Override
    public List<Ilayout> children() {
        List<Ilayout> children = new ArrayList<>();

        for (int i = 0; i < tubes.size(); i++) {
            for (int j = 0; j < tubes.size(); j++) {
                if (isValidMove(i, j)) {
                    children.add(applyMove(i, j));
                }
            }
        }

        return children;
    }

    /**
     * Duas configurações são iguais se tiverem os mesmos tubos com as mesmas bolas pela mesma ordem.
     *
     * @param o objeto a comparar.
     * @return {@code true} se {@code o} for um {@code BallSortLayout} equivalente a este.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BallSortLayout that = (BallSortLayout) o;
        if (hashCode() != that.hashCode()) return false;
        return Objects.equals(tubes, that.tubes);
    }

    /**
     * Devolve o hash da configuração, calculado uma única vez e guardado em cache.
     *
     * @return o hash consistente com {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = tubes.hashCode();
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Esta configuração é objetivo se todos os tubos estiverem vazios ou cheios com uma só cor;
     * o parâmetro {@code l} é ignorado.
     */
    @Override
    public boolean isGoal(Ilayout l) {
        for (Stack<Character> tube : tubes) {
            if (!isValidTube(tube)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return sempre 1.
     */
    @Override
    public double getK() {
        return K;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Conta as bolas que não pertencem ao maior bloco de cor contínuo assente na base de
     * algum tubo, para cada cor; o parâmetro {@code goal} é ignorado.
     */
    @Override
    public double getHeuristic(Ilayout goal) {
        int total = 0;
        Map<Character, Integer> colorBlocks = new HashMap<>();
        for (Stack<Character> tube : tubes) {
            if (tube.isEmpty()) {
                continue;
            }

            char base = tube.get(0);
            total += tube.size();

            colorBlocks.merge(base, 1, Math::max);

            for (int i = 1; i < tube.size(); i++) {
                char actualBall = tube.get(i);
                if (actualBall != base) {
                    break;
                }
                colorBlocks.merge(base, i + 1, Math::max);
            }
        }

        int totalColorBlocks = 0;
        for (Integer v : colorBlocks.values()) {
            totalColorBlocks += v;
        }

        return total - totalColorBlocks;
    }

    /**
     * Cria uma cópia profunda desta configuração (os tubos são copiados).
     *
     * @return uma nova configuração igual a esta e independente dela.
     */
    @Override
    public BallSortLayout clone() {
        try {
            BallSortLayout clone = (BallSortLayout) super.clone();
            clone.hash = 0;
            clone.tubes = new ArrayList<>(this.tubes.size());

            for (Stack<Character> tube : this.tubes) {
                @SuppressWarnings("unchecked")
                Stack<Character> cloneTube = (Stack<Character>) tube.clone();
                clone.tubes.add(cloneTube);
            }

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Verifica se um tubo está resolvido.
     *
     * @param tube o tubo a verificar.
     * @return {@code true} se o tubo estiver vazio ou cheio com bolas da mesma cor.
     */
    private boolean isValidTube(Stack<Character> tube) {
        if (tube.isEmpty()) {
            return true;
        }

        if (tube.size() != maxTubeSize) {
            return false;
        }

        char firstBall = tube.get(0);
        for (char ball : tube) {
            if (ball != firstBall) {
                return false;
            }
        }

        return true;
    }

    /**
     * Devolve a representação textual no mesmo formato aceite pelo construtor.
     *
     * @return uma linha por nível, do topo para a base, com {@code 'E'} nas posições vazias.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = maxTubeSize - 1; i >= 0; i--) {
            for (int j = 0; j < tubes.size(); j++) {
                Stack<Character> tube = tubes.get(j);

                if (i < tube.size()) {
                    sb.append(tube.get(i));
                } else {
                    sb.append("E");
                }

                if (j < tubes.size() - 1) {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Verifica se é possível mover a bola do topo do tubo de origem para o tubo de destino.
     *
     * @param from índice do tubo de origem
     * @param to   índice do tubo de destino
     * @return {@code true} se o movimento for válido segundo as regras do jogo, caso contrário {@code false}.
     */
    private boolean isValidMove(int from, int to) {
        if (to == from) return false;
        Stack<Character> src = tubes.get(from);
        if (isValidTube(src)) return false;
        if (src.isEmpty()) return false;
        Stack<Character> dst = tubes.get(to);
        if (dst.size() == maxTubeSize) return false;
        char ball = src.peek();
        return dst.isEmpty() || dst.peek() == ball;
    }

    /**
     * Aplica um movimento criando uma nova configuração resultante da transferência da bola.
     *
     * @param from índice do tubo de origem
     * @param to   índice do tubo de destino
     * @return uma nova instância de {@code BallSortLayout} refletindo o estado após o movimento.
     */
    private BallSortLayout applyMove(int from, int to) {
        BallSortLayout child = clone();
        char ball = child.tubes.get(from).pop();
        child.tubes.get(to).push(ball);
        return child;
    }

    /**
     * Sorteia uma jogada válida aleatória otimizada, evitando gerar todos os vizinhos.
     * Tenta sortear cegamente até um máximo de tentativas para maior rapidez;
     * se falhar, recorre à enumeração exaustiva.
     *
     * @param rnd   gerador de números aleatórios
     * @param avoid estado anterior (pai) a tentar evitar para prevenir ciclos de tamanho 2 (ping-pong)
     * @return um estado filho válido, ou {@code null} se não existirem jogadas possíveis.
     */
    @Override
    public Ilayout randomChild(Random rnd, Ilayout avoid) {
        int n = tubes.size();
        if (n < 2) return null;
        int maxAttempts = 2 * n * (n - 1);

        for (int i = 0; i < maxAttempts; i++) {
            int from = rnd.nextInt(n);
            int to = rnd.nextInt(n - 1);
            if (to >= from) to++;
            if (!isValidMove(from, to)) {
                continue;
            }
            BallSortLayout child = applyMove(from, to);
            if (child.equals(avoid)) {
                continue;
            }
            return child;
        }
        return randomChildByEnumeration(rnd, avoid);
    }

    /**
     * Sorteia uma jogada válida através da enumeração de todas as transições possíveis.
     * Utilizado como alternativa de *fallback* quando a seleção cega falha repetidamente.
     *
     * @param rnd   gerador de números aleatórios
     * @param avoid estado a evitar (geralmente o estado de onde esta configuração derivou)
     * @return um estado filho aleatório válido, ou {@code null} se não houver jogadas possíveis
     *         ou se a única jogada possível levar ao estado {@code avoid}.
     */
    private Ilayout randomChildByEnumeration(Random rnd, Ilayout avoid) {
        int n = tubes.size();
        int[] moves = new int[n * (n - 1)];
        int count = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (isValidMove(i, j)) {
                    moves[count++] = i * n + j;
                }
            }
        }
        if (count == 0) {
            return null;
        }

        int r = rnd.nextInt(count);
        BallSortLayout child = applyMove(moves[r] / n, moves[r] % n);

        if (child.equals(avoid)) {
            if (count == 1) {
                return null;
            }
            int other = rnd.nextInt(count - 1);
            r = (other >= r) ? other + 1 : other;
            child = applyMove(moves[r] / n, moves[r] % n);
        }
        return child;
    }
}