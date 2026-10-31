package search;

import core.Ilayout;

import java.util.*;

public class AStarSearch {
    protected Queue<State> abertos;

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

    public static class State {
        private final Ilayout layout;
        private final double g;
        private final double f;
        public State father;

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

        public String toString() {
            return layout.toString();
        }

        public double getG() {
            return g;
        }

        public double getF() {
            return f;
        }

        public int hashCode() {
            return layout.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            State n = (State) o;
            return this.layout.equals(n.layout);
        }
    }
}
