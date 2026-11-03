package search;

import core.Ilayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCTS {

    private final int cMax;

    public static class State {
        private final Ilayout layout;
        private final State father;
        private final double g;
        private final List<State> children;
        private int visitCount;
        private double sim;

        public State(Ilayout layout, State father, double cmax) {
            this.layout = layout;
            this.father = father;
            this.sim = cmax;

            g = (father == null) ? 0.0 : father.g + layout.getK();

            children = new ArrayList<>();

            visitCount = 0;
        }

        public double getG() {
            return g;
        }

        public void addVisit() {
            visitCount++;
        }

        public List<State> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public List<State> expand(double cMax) {
            for (Ilayout l : layout.children()) {
                children.add(new State(l, this, cMax));
            }
            return getChildren();
        }

        public void updateSim(double candidateSim) {
            if (candidateSim < sim) {
                sim = candidateSim;
            }
        }

        public double reward(double fMax) {
            return 1 - (getG() + sim) / fMax;
        }

        public double ucb(double fMax, double c, int totalSimulation) {
            return reward(fMax) + c * Math.sqrt(Math.log(totalSimulation) / visitCount);
        }
    }


}
