import puzzles.BallSortLayout;
import search.MCTS;
import search.MCTS.State;
import search.Simulator;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Random;

public class Experiments {

    public static void main(String[] args) {
        String csvFileName = "mcts_experiments_results.csv";

        // Instâncias de teste
        BallSortLayout startHard = new BallSortLayout("...");
        BallSortLayout goal = new BallSortLayout("...");

        double[] epsilons = {0.0, 0.1, 0.3, 1.0};
        int[] seeds = new int[30];
        for (int i = 0; i < 30; i++) seeds[i] = i + 1;

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            // Cabeçalho do CSV
            writer.println("instancia,epsilon,c,cMax,N,maxDepth,seed,custo,tempo_ms,resolvido");

            for (double eps : epsilons) {
                for (int seed : seeds) {
                    Random rnd = new Random(seed);
                    Simulator sim = new Simulator(1, 50.0, rnd, eps);
                    MCTS mcts = new MCTS(50.0, 500, sim, 100.0, 0.2, 10);

                    long start = System.currentTimeMillis();
                    Iterator<State> sol = mcts.solve(startHard, goal);
                    long elapsed = System.currentTimeMillis() - start;

                    boolean solved = sol != null;
                    double cost = 50.0;

                    if (solved) {
                        State lastState = null;
                        while (sol.hasNext()) {
                            lastState = sol.next();
                        }
                        if (lastState != null) {
                            cost = lastState.getG();
                        }
                    }

                    writer.printf("dificil,%.2f,0.2,50.0,1,10,%d,%.2f,%d,%b%n",
                            eps, seed, cost, elapsed, solved);
                    writer.flush();

                    System.out.printf("Concluído: eps=%.2f, seed=%d | resolvido=%b, tempo=%d ms%n", eps, seed, solved, elapsed);
                }
            }
            System.out.println("Experiências concluídas com sucesso. CSV gerado: " + csvFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}