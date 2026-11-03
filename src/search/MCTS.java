package search;

import core.Ilayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MCTS {
    private final double cMax;
    private final int maxIterations;
    private final Simulator simulator;
    private final double fMax;
    private final double c;
    private final int maxDepth;

    public MCTS(double cMax, int maxIterations, Simulator simulator, double fMax, double c, int maxDepth) {
        this.cMax = cMax;
        this.maxIterations = maxIterations;
        this.simulator = simulator;
        this.fMax = fMax;
        this.c = c;
        this.maxDepth = maxDepth;
    }

    public Iterator<State> solve(Ilayout start, Ilayout goal) {
        State root = new State(start, null, cMax);

        for (int i = 0; i < maxIterations; i++) {
            State leaf = select(root);
            List<State> children = leaf.expand(cMax);

            for (State child : children) {
                SimResult result = simulator.simulate(child.layout, goal);
                backpropagate(child, result.getCost());
            }
        }
    }

    public void backpropagate(State child, double simCost) {
        State atual = child;

        while (atual != null) {
            atual.addVisit();
            atual.updateSim(child.getG() - atual.getG() + simCost);
            atual = atual.getFather();
        }
    }

    public State select(State root) {
        State atual = root;
        while (!atual.isLeaf()) {
            atual = bestChild(atual);
        }
        return atual;
    }

    private State bestChild(State atual) {
        State best = null;

        double bestValue = Double.NEGATIVE_INFINITY;

        for (State child : atual.getChildren()) {
            if (child.getVisitCount() == 0) return child;

            double value = child.ucb(fMax, c, atual.getVisitCount());

            if (value > bestValue) {
                best = child;
                bestValue = value;
            }
        }

        return best;
    }

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

        public double getSim() {
            return sim;
        }

        public int getVisitCount() {
            return visitCount;
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

        public State getFather() {
            return father;
        }
    }


}
