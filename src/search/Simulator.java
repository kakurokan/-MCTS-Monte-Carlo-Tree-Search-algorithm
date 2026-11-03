package search;

import core.Ilayout;

import java.util.Random;

public class Simulator {
    private final int n;
    private final double cMax;
    private final Random rnd;
    public Simulator(int n, double cMax, Random rnd) {
        if(n<=0)  throw new IllegalArgumentException("n tem de ser n >=1");
        if(cMax<=0)  throw new IllegalArgumentException("cMax tem de ser n > 0");
        this.n = n;
        this.cMax = cMax;
        this.rnd = rnd;
    }

    public SimResult simulate(Ilayout start,  Ilayout goal) {
        return new SimResult(null,cMax);
    }
}
