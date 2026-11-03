package search;

import core.Ilayout;

import java.util.*;

public class MCTS {
    private final double cMax;
    private final int maxIterations;
    private final Simulator simulator;
    private final double fMax;
    private final double c;
    private final int maxDepth;

    public MCTS(double cMax, int maxIterations, Simulator simulator, double fMax, double c, int maxDepth) {
        if (maxIterations <= 0 || cMax <= 0 || maxDepth <= 0 || c < 0) {
            throw new IllegalArgumentException("parâmetros inválidos");
        }

        if (fMax < maxDepth + cMax) {
            throw new IllegalArgumentException("fMax inválido. fMax deve ser maior ou igual à maxDepth + cMax");
        }

        this.cMax = cMax;
        this.maxIterations = maxIterations;
        this.simulator = simulator;
        this.fMax = fMax;
        this.c = c;
        this.maxDepth = maxDepth;
    }

    public Iterator<State> solve(Ilayout start, Ilayout goal) {
        State root = new State(start, null, cMax);

        double bestCost = Double.POSITIVE_INFINITY;
        State bestNode = null;
        List<Ilayout> bestTail = List.of();

        for (int i = 0; i < maxIterations; i++) {
            State leaf = select(root);

            if (leaf.getLayout().isGoal(goal)) {
                if (leaf.getG() < bestCost) {
                    bestNode = leaf;
                    bestCost = leaf.getG();
                    bestTail = List.of();
                }

                backpropagate(leaf, 0);
                if (bestCost == 0) break;
                continue;
            }

            List<State> children = leaf.expand(cMax, maxDepth);
            if (children.isEmpty()) {
                backpropagate(leaf, cMax);
                continue;
            }

            for (State child : children) {
                SimResult result = simulator.simulate(child.getLayout(), goal);
                double total = child.getG() + result.getCost();
                if (result.isSolved() && total < bestCost) {
                    bestNode = child;
                    bestCost = total;
                    bestTail = result.getPath();
                }

                backpropagate(child, result.getCost());
            }
        }

        if (bestNode == null) {
            return null;
        }

        State end = bestNode;
        for (Ilayout l : bestTail) {
            end = new State(l, end, cMax);
        }

        LinkedList<State> path = new LinkedList<>();
        State atual = end;
        while (atual != null) {
            path.addFirst(atual);
            atual = atual.getFather();
        }

        return path.iterator();
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
        private final int depth;
        private int visitCount;
        private double sim;

        public State(Ilayout layout, State father, double cmax) {
            this.layout = layout;
            this.father = father;
            this.sim = cmax;

            depth = (father == null) ? 0 : father.depth + 1;
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

        public Ilayout getLayout() {
            return layout;
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

        public List<State> expand(double cMax, int maxDepth) {
            if (depth >= maxDepth) {
                return getChildren();
            }

            for (Ilayout l : layout.children()) {
                if (father == null || !l.equals(father.layout)) {
                    children.add(new State(l, this, cMax));
                }
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
