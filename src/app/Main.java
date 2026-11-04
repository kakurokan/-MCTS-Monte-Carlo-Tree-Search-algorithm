package app;

import core.Ilayout;
import puzzles.BallSortLayout;
import search.AStarSearch;
import search.MCTS;
import search.Simulator;

import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final double DEFAULT_CMAX = 500;    // era 200
    private static final int    DEFAULT_N = 1;         // era 10
    private static final double DEFAULT_C = 0.3;       // era 0.05
    private static final int    DEFAULT_ITERATIONS = 15000; // era 5000
    private static final int    DEFAULT_MAX_DEPTH = 60;     // era 200
    private static final long   DEFAULT_SEED = 42;
    // fmax = cMax + maxDepth (mantém-se)
    static void main(String[] args) throws Exception {

        double cMax     = args.length > 0 ? Double.parseDouble(args[0]) : DEFAULT_CMAX;
        int n           = args.length > 1 ? Integer.parseInt(args[1])   : DEFAULT_N;
        double c        = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_C;
        int iterations  = args.length > 3 ? Integer.parseInt(args[3])   : DEFAULT_ITERATIONS;
        int maxDepth    = args.length > 4 ? Integer.parseInt(args[4])   : DEFAULT_MAX_DEPTH;
        long seed       = args.length > 5 ? Long.parseLong(args[5])     : DEFAULT_SEED;
        double fmax = cMax+maxDepth;
        Random random = new Random(seed);
        Simulator sim = new Simulator(n, cMax, random);
        Scanner sc = new Scanner(System.in);
        int height = sc.nextInt();
        int tubes = sc.nextInt();
        sc.nextLine();
        StringBuilder configuration = new StringBuilder();
        for (int row = 0; row < height; row++) {
            String line = sc.nextLine();
            if (line.split(" ", -1).length != tubes)
                throw new IllegalArgumentException();
            configuration.append(line).append('\n');
        }

        Ilayout start = new BallSortLayout(configuration.toString());


        MCTS s = new MCTS(cMax,iterations,sim,fmax,c,maxDepth);
        Iterator<MCTS.State> it =
                s.solve(start, null);
        if (it == null) System.out.println("no solution found");
        else while (it.hasNext()) {
            MCTS.State i = it.next();
            if (!it.hasNext()) System.out.println((int) i.getG());
        }
        sc.close();
    }
}
