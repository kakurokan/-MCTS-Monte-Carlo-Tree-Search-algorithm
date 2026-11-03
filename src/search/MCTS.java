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
            visitCount = 0;
        }
    }


}
