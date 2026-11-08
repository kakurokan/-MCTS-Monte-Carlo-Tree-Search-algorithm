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

/**
 * Classe responsável por correr os benchmarks do algoritmo MCTS.
 * <p>
 * Para cada instância e cada valor de epsilon, corre o MCTS com as sementes 1 a 30 e
 * escreve uma linha por execução no ficheiro CSV {@code mcts_experiments_results.csv}
 * (custo obtido, óptimo conhecido, tempo em ms e se foi encontrada solução).
 * <p>
 * Parâmetros utilizados:
 * <ul>
 *   <li>epsilons: probabilidades de passo aleatório no rollout testadas (0.0, 0.1, 0.3, 1.0);</li>
 *   <li>c: constante de exploração do UCB (0.3);</li>
 *   <li>cMax: custo máximo permitido por simulação (500.0);</li>
 *   <li>maxDepth: profundidade máxima da árvore (100);</li>
 *   <li>n: número de rollouts por simulação (1);</li>
 *   <li>maxIterations: número máximo de iterações do MCTS (15000);</li>
 *   <li>fMax: cMax + maxDepth.</li>
 * </ul>
 * NOTA DE EXECUÇÃO: esta classe deve ser executada a partir da raiz do repositório
 * para que os caminhos da diretoria "instancias/" sejam lidos corretamente.
 */

public class Experiments {

    /**
     * Corre todas as experiências e escreve os resultados no CSV. Não usa argumentos.
     * Se não for possível abrir ou ler algum ficheiro, escreve o erro em {@code System.err}.
     *
     * @param args ignorado.
     */
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
        for (int i = 0; i < 30; i++) {
            seeds[i] = i + 1;
        }

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
                        double cost = -1.0;

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

    /**
     * Lê o conteúdo completo de um ficheiro de texto.
     *
     * @param path caminho do ficheiro.
     * @return o conteúdo do ficheiro.
     * @throws IOException se o ficheiro não existir ou não puder ser lido.
     */
    private static String lerFicheiro(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}