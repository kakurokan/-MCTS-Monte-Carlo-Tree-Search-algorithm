package search;

import core.Ilayout;

import java.util.List;

public class SimResult {
    private List<Ilayout> path;
    private double minCost;

    public SimResult(List<Ilayout> path, double minCost) {
        this.path = (path == null) ? null : List.copyOf(path);
        this.minCost = minCost;
    }


    public List<Ilayout> getPath() {
        return path == null ? null : this.path;
    }

    public double getCost() {
        return this.minCost;
    }

    public boolean isSolved() {
        return this.path != null;
    }

    
}
