package core;

import java.util.List;

public interface Ilayout {
    /**
     * Return the configurations reachable in one legal move.
     */
    List<Ilayout> children();

    /**
     * Return true if this configuration equals l.
     */
    boolean isGoal(Ilayout l);

    /**
     * Cost of moving into this configuration: 1 for this puzzle.
     */
    double getK();

    /**
     *
     * Cost of heuristic of the current to goal
     */
    double getHeuristic(Ilayout goal);
}
