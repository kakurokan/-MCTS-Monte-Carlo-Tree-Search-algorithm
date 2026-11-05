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
    private final int K;
    private final int maxTubeSize;
    private List<Stack<Character>> tubes;
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
            Stack<Character> sourceTube = tubes.get(i);

            if (sourceTube.isEmpty()) {
                continue;
            }

            if (isValidTube(sourceTube)) {
                continue;
            }

            char ball = sourceTube.peek();

            for (int j = 0; j < this.tubes.size(); j++) {
                if (i == j) {
                    continue;
                }

                Stack<Character> currentDestTube = this.tubes.get(j);

                if (currentDestTube.size() == maxTubeSize) {
                    continue;
                }
                if (!currentDestTube.isEmpty() && currentDestTube.peek() != ball) {
                    continue;
                }

                BallSortLayout child = this.clone();
                child.tubes.get(i).pop();
                child.tubes.get(j).push(ball);
                children.add(child);
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
}
