package app;

import puzzles.BallSortLayout;
import search.MCTS;
import search.Simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class Experiments {

    public static void main(String[] args) {
        String csvFileName = "mcts_experiments_results.csv";

        // Parâmetros do benchmark
        double[] epsilons = {0.0, 0.1, 0.3, 1.0};
        double c = 0.3;
        double cMax = 500.0;
        int n = 1;
        int maxDepth = 100;
        int maxIterations = 15000;

        // Seeds de 1 a 30
        int[] seeds = new int[30];
        for (int i = 0; i < 30; i++) seeds[i] = i + 1;

        String[] files = {"instancias/dificil_opt37.txt", "instancias/muito_dificil_opt65.txt"};
        String[] labels = {"dificil", "muito_dificil"};
        double[] otimos = {37.0, 65.0};

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            writer.println("instancia,configuracao,seed,epsilon,c,cMax,N,maxDepth,iteracoes,custo,otimo,tempo_ms,resolvido");

            for (int f = 0; f < files.length; f++) {
                // Ler os txt
                String fileContent = lerFicheiro(files[f]);
                BallSortLayout startNode = new BallSortLayout(fileContent);

                // Definimos o goal como nulo, porque a função isGoal() de BallSort avalia as próprias cores,
                // sem precisar de comparar com um tabuleiro externo
                BallSortLayout goal = null;

                for (double eps : epsilons) {
                    String configLabel = "eps" + eps;

                    for (int seed : seeds) {
                        Random rnd = new Random(seed);
                        Simulator sim = new Simulator(n, cMax, rnd, eps);

                        double fMax = cMax + maxDepth;

                        // Inicializar o MCTS
                        MCTS mcts = new MCTS(cMax, maxIterations, sim, fMax, c, maxDepth);

                        long start = System.currentTimeMillis();
                        Iterator<MCTS.State> sol = mcts.solve(startNode, goal);
                        long elapsed = System.currentTimeMillis() - start;

                        boolean solved = (sol != null);
                        double cost = cMax; // Se falhar, regista o custo máximo

                        // Se encontrou a solução, vai até ao último passo para ver o custo final
                        if (solved) {
                            MCTS.State lastState = null;
                            while (sol.hasNext()) {
                                lastState = sol.next();
                            }
                            if (lastState != null) cost = lastState.getG();
                        }

                        // Imprime no CSV
                        writer.printf(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.1f,%d,%d,%d,%.1f,%.1f,%d,%b%n",
                                labels[f], configLabel, seed, eps, c, cMax, n, maxDepth,
                                maxIterations, cost, otimos[f], elapsed, solved);
                        writer.flush(); // Salva a linha no cvsfile

                        System.out.printf(Locale.ROOT, "Concluído: %s | %s | seed=%d | tempo=%dms%n",
                                labels[f], configLabel, seed, elapsed);
                    }
                }
            }
            System.out.println("Experiências concluídas. Resultados guardados em: " + csvFileName);

        } catch (IOException e) {
            System.err.println("Erro ao abrir ou ler ficheiros: " + e.getMessage());
        }
    }

    private static String lerFicheiro(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}