package search;

import core.Ilayout;

import java.util.*;

public class BestFirst {
    protected Queue<State> abertos;
    private Map<Ilayout, State> fechados;
    private State actual;
    private Ilayout objective;

    private List<State> sucessores(State n) {
        List<State> sucs = new ArrayList<>();
        List<Ilayout> children = n.layout.children();
        for (Ilayout e : children) {
            if (n.father == null || !e.equals(n.father.layout)) {
                State nn = new State(e, n);
                sucs.add(nn);
            }
        }
        return sucs;
    }

    final public Iterator<State> solve(Ilayout s, Ilayout goal) {
        objective = goal;
        abertos = new PriorityQueue<>(10,
                (s1, s2) -> (int) Math.signum(s1.getG() - s2.getG()));
        fechados = new HashMap<>();
        abertos.add(new State(s, null));
        List<State> sucs;

        while (!abertos.isEmpty()) {
            actual = abertos.poll();

            if (actual.layout.isGoal(this.objective)) {
                List<State> path = new ArrayList<>();
                State temp = actual;
                while (temp != null) {
                    path.add(temp);
                    temp = temp.father;
                }
                Collections.reverse(path);
                return path.iterator();
            } else {
                fechados.put(actual.layout, actual);
                sucs = this.sucessores(actual);
                for (State child : sucs) {
                    if (!fechados.containsKey(child.layout) && !abertos.contains(child)) {
                        abertos.add(child);
                    }
                }
            }
        }
        return null;
    }

    public static class State {
        final Ilayout layout;
        final State father;
        private final double g;

        public State(Ilayout l, State n) {
            layout = l;
            father = n;
            if (father != null)
                g = father.g + l.getK();
            else g = 0.0;
        }

        public String toString() {
            return layout.toString();
        }

        public double getG() {
            return g;
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            State n = (State) o;
            return this.layout.equals(n.layout);
        }
    }
}
