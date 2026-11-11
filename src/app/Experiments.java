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
 * Suporta duas experiências, escolhidas pelo primeiro argumento de {@link #main(String[])}:
 * <ul>
 *   <li>{@code epsilon} (por omissão): para cada instância e cada valor de epsilon, corre o MCTS
 *   com as sementes 1 a 30 e escreve uma linha por execução em {@code mcts_experiments_results.csv}
 *   (custo obtido, ótimo conhecido, tempo em ms e se foi encontrada solução);</li>
 *   <li>{@code exaustos}: compara o MCTS com e sem rastreio de nós exaustos na instância
 *   {@code muito_dificil}, com as sementes 1 a 30, e escreve os resultados em {@code exp_exaustos.csv}.</li>
 * </ul>
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
     * Ponto de entrada: seleciona e corre a experiência pedida, escrevendo os resultados num CSV.
     * Se não for possível abrir ou ler algum ficheiro, escreve o erro em {@code System.err}.
     *
     * @param args {@code args[0]} (opcional) é o modo: {@code "exaustos"} corre a experiência de
     *             nós exaustos; qualquer outro valor, ou ausência de argumentos, corre a experiência de epsilon.
     */
    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0] : "epsilon";
        if (modo.equalsIgnoreCase("exaustos")) {
            experienciaExaustos();
        } else {
            experienciaEpsilon();
        }
    }

    /**
     * Compara o MCTS com e sem rastreio de nós exaustos na instância {@code muito_dificil},
     * com as sementes 1 a 30 por configuração, e escreve uma linha por execução em
     * {@code exp_exaustos.csv} (custo bruto e final, ótimo, tempo em ms e se foi resolvido).
     */
    private static void experienciaExaustos() {
        String csvFileName = "exp_exaustos.csv";

        double eps = 0.1;
        double c = 0.3;
        double cMax = 500.0;
        int n = 1;
        int maxDepth = 100;
        int maxIterations = 15000;
        double fMax = cMax + maxDepth;
        boolean[] opcoes = {true, false};

        String file = "instancias/muito_dificil_opt65.txt";
        String label = "muito_dificil";
        double otimo = 65.0;

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            writer.println("instancia,configuracao,seed,epsilon,c,cMax,N,maxDepth,iteracoes,exaustos,custo_bruto,custo,otimo,tempo_ms,resolvido");

            BallSortLayout startNode = new BallSortLayout(lerFicheiro(file));

            for (boolean exaustos : opcoes) {
                String configLabel = "exaustos" + exaustos;

                for (int seed = 1; seed <= 30; seed++) {
                    Simulator sim = new Simulator(n, cMax, new Random(seed), eps);
                    MCTS mcts = new MCTS(cMax, maxIterations, sim, fMax, c, maxDepth, exaustos);

                    long start = System.currentTimeMillis();
                    Iterator<MCTS.State> sol = mcts.solve(startNode, null);
                    long elapsed = System.currentTimeMillis() - start;

                    boolean solved = (sol != null);
                    double custoBruto = solved ? mcts.getLastRawCost() : -1.0;
                    double custo = solved ? mcts.getLastFinalCost() : -1.0;

                    writer.printf(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.1f,%d,%d,%d,%b,%.1f,%.1f,%.1f,%d,%b%n",
                            label, configLabel, seed, eps, c, cMax, n, maxDepth, maxIterations,
                            exaustos, custoBruto, custo, otimo, elapsed, solved);
                    writer.flush();

                    System.out.printf(Locale.ROOT, "Concluído: %s | seed=%d | custo=%.0f (bruto %.0f) | tempo=%dms%n",
                            configLabel, seed, custo, custoBruto, elapsed);
                }
            }
            System.out.println("Experiência concluída. Resultados em: " + csvFileName);
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

    /**
     * Para cada instância e cada valor de epsilon, corre o MCTS com as sementes 1 a 30 e escreve
     * os resultados em {@code mcts_experiments_results.csv}.
     */
    private static void experienciaEpsilon() {
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
                        MCTS mcts = new MCTS(cMax, maxIterations, sim, fMax, c, maxDepth, true);

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
}