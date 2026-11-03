package search;

import core.Ilayout;

import java.util.ArrayList;
import java.util.List;

public class MCTS {
    public static class State {
        private final Ilayout layout;
        private final State father;
        private final double g;
        private final List<State> children;
        private int visitCount;
        private double sim;
        private List<Ilayout> unexpanded;

        public State(Ilayout layout, State father, int cmax) {
            this.layout = layout;
            this.father = father;
            this.sim = cmax;

            g = (father == null) ? 0.0 : father.g + layout.getK();

            children = new ArrayList<>();
            unexpanded = new ArrayList<>(layout.children());

            visitCount = 0;
        }

        public double getG() {
            return g;
        }

        public void addVisit() {
            visitCount++;
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
