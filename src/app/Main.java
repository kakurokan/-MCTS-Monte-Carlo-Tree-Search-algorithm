package app;

import puzzles.BallSortLayout;
import search.AStarSearch;

import java.util.Iterator;
import java.util.Scanner;

public class Main {
    static void main(String[] args) throws Exception {
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
        AStarSearch s = new AStarSearch();
        Iterator<AStarSearch.State> it =
                s.solve(new BallSortLayout(configuration.toString()), null);
        if (it == null) System.out.println("no solution found");
        else while (it.hasNext()) {
            AStarSearch.State i = it.next();
            if (!it.hasNext()) System.out.println((int) i.getG());
        }
        sc.close();
    }
}
