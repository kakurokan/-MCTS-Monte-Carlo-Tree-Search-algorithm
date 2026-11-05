package search;

import core.Ilayout;

import java.util.*;

/**
 * Pesquisa A* sobre configurações {@link Ilayout}, com f(n) = g(n) + h(n).
 */
public class AStarSearch {
    /** Fila de prioridade dos estados por expandir, ordenada por f. */
    protected Queue<State> abertos;

    /**
     * Gera os estados sucessores de {@code n}, excluindo o que regressa ao seu pai.
     *
     * @param n    estado a expandir.
     * @param goal configuração objetivo, usada no cálculo da heurística.
     * @return a lista de sucessores.
     */
    private List<State> sucessores(State n, Ilayout goal) {
        List<State> sucs = new ArrayList<>();
        List<Ilayout> children = n.layout.children();
        for (Ilayout e : children) {
            if (n.father == null || !e.equals(n.father.layout)) {
                State nn = new State(e, n, goal);
                sucs.add(nn);
            }
        }
        return sucs;
    }

    /**
     * Procura o caminho de menor custo entre duas configurações.
     *
     * @param s    configuração inicial.
     * @param goal configuração objetivo.
     * @return um iterador sobre os estados do caminho, do inicial ao objetivo;
     *         {@code null} se não existir solução.
     * @pre s != null &amp;&amp; goal != null
     */
    final public Iterator<State> solve(Ilayout s, Ilayout goal) {
        abertos = new PriorityQueue<>(10, Comparator.comparingDouble(State::getF));
        Map<Ilayout, State> fechados = new HashMap<>();
        abertos.add(new State(s, null, goal));
        List<State> sucs;

        while (!abertos.isEmpty()) {
            State atual = abertos.poll();

            if (atual.layout.isGoal(goal)) {
                List<State> path = new ArrayList<>();
                State temp = atual;
                while (temp != null) {
                    path.add(temp);
                    temp = temp.father;
                }
                Collections.reverse(path);
                return path.iterator();
            } else {
                fechados.put(atual.layout, atual);
                sucs = this.sucessores(atual, goal);
                for (State child : sucs) {
                    State foundState = fechados.get(child.layout);

                    if (foundState == null || child.getG() < foundState.getG()) {
                        fechados.put(child.layout, child);
                        abertos.add(child);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Nó da pesquisa: uma configuração, o seu antecessor e os custos g e f.
     *
     * @inv g &gt;= 0 &amp;&amp; f &gt;= g
     */
    public static class State {
        private final Ilayout layout;
        private final double g;
        private final double f;
        /** Estado antecessor; {@code null} na raiz. */
        public State father;

        /**
         * Cria um estado, calculando g (custo acumulado) e f (g mais heurística).
         *
         * @param l      configuração do estado.
         * @param father estado antecessor, ou {@code null} se for a raiz.
         * @param goal   configuração objetivo para a heurística.
         */
        public State(Ilayout l, State father, Ilayout goal) {
            layout = l;
            this.father = father;
            if (father != null) {
                g = father.g + l.getK();
            } else {
                g = 0.0;
            }
            f = g + l.getHeuristic(goal);
        }

        /** @return a representação textual da configuração do estado. */
        public String toString() {
            return layout.toString();
        }

        /** @return o custo acumulado desde a configuração inicial. */
        public double getG() {
            return g;
        }

        /** @return o custo estimado total, g + h. */
        public double getF() {
            return f;
        }

        /** @return o hash da configuração do estado. */
        public int hashCode() {
            return layout.hashCode();
        }

        /**
         * Dois estados são iguais se tiverem configurações iguais.
         *
         * @param o objeto a comparar.
         * @return {@code true} se {@code o} for um estado com a mesma configuração.
         */
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            State n = (State) o;
            return this.layout.equals(n.layout);
        }
    }
}
