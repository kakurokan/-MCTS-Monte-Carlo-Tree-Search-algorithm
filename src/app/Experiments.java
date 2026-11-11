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
 * Suporta três experiências, escolhidas pelo primeiro argumento de {@link #main(String[])}:
 * <ul>
 *   <li>{@code epsilon} (por omissão): para cada instância e cada valor de epsilon, corre o MCTS
 *   com as sementes 1 a 30 e escreve uma linha por execução em {@code mcts_experiments_results.csv}
 *   (custo obtido, ótimo conhecido, tempo em ms e se foi encontrada solução);</li>
 *   <li>{@code exaustos}: compara o MCTS com e sem rastreio de nós exaustos na instância
 *   {@code muito_dificil}, com as sementes 1 a 30, e escreve os resultados em {@code exp_exaustos.csv};</li>
 *   <li>{@code parametros}: varia cMax (130, 200, 325, 500) e N (1, 3, 5) na instância
 *   {@code muito_dificil}, com o mesmo número total de rollouts em cada N, e escreve os resultados
 *   em {@code exp_parametros.csv}, incluindo quantos rollouts foram cortados em cMax. O segundo
 *   argumento (opcional) é o número de sementes, por omissão 30.</li>
 * </ul>
 * <p>
 * Parâmetros utilizados (valores base):
 * <ul>
 *   <li>epsilon: probabilidade de passo aleatório no rollout; só a experiência {@code epsilon}
 *   varia o valor (0.0, 0.1, 0.3, 1.0), as restantes usam 0.1;</li>
 *   <li>c: constante de exploração do UCB (0.3);</li>
 *   <li>cMax: custo máximo permitido por simulação (500.0 como valor base; a experiência
 *   {@code parametros} varia-o);</li>
 *   <li>maxDepth: profundidade máxima da árvore (100);</li>
 *   <li>n: número de rollouts por simulação (1 como valor base; a experiência
 *   {@code parametros} varia-o);</li>
 *   <li>maxIterations: número máximo de iterações do MCTS (15000);</li>
 *   <li>fMax: cMax + maxDepth.</li>
 * </ul>
 * NOTA DE EXECUÇÃO: esta classe deve ser executada a partir da raiz do repositório
 * para que os caminhos da diretoria "instancias/" sejam lidos corretamente.
 */

public class Experiments {

    /** Classe utilitária apenas com métodos estáticos; não deve ser instanciada. */
    private Experiments() {
    }

    /**
     * Ponto de entrada: seleciona e corre a experiência pedida, escrevendo os resultados num CSV.
     * Se não for possível abrir ou ler algum ficheiro, escreve o erro em {@code System.err}.
     *
     * @param args {@code args[0]} (opcional) é o modo: {@code "exaustos"} corre a experiência de
     *             nós exaustos; {@code "parametros"} corre a experiência de cMax e N, e aceita em
     *             {@code args[1]} o número de sementes (por omissão 30); qualquer outro valor, ou
     *             ausência de argumentos, corre a experiência de epsilon.
     */
    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0] : "epsilon";
        if (modo.equalsIgnoreCase("exaustos")) {
            experienciaExaustos();
        } else if (modo.equalsIgnoreCase("parametros")) {
            int numSeeds = 30;
            if (args.length > 1) {
                try {
                    numSeeds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    numSeeds = 0;
                }
                if (numSeeds < 1) {
                    System.err.println("Número de sementes inválido: \"" + args[1]
                            + "\". Indique um inteiro >= 1.");
                    return;
                }
            }
            experienciaParametros(numSeeds);
        } else {
            experienciaEpsilon();
        }
    }

    /** Parâmetros comuns a todas as experiências. */
    private static final double C = 0.3;
    private static final double CMAX = 500.0;
    private static final double EPSILON = 0.1;
    private static final int N = 1;
    private static final int MAX_DEPTH = 100;
    private static final int MAX_ITERATIONS = 15000;
    private static final String FICHEIRO_MUITO_DIFICIL = "instancias/muito_dificil_opt65.txt";
    private static final String LABEL_MUITO_DIFICIL = "muito_dificil";
    private static final double OTIMO_MUITO_DIFICIL = 65.0;

    /** Resultado de uma execução do MCTS: o simulador usado (para as suas métricas), custos, tempo e sucesso. */
    private record Execucao(Simulator sim, double custoBruto, double custo, long tempoMs, boolean resolvido) {
    }

    /** Corpo de uma experiência: recebe o escritor do CSV já aberto e com o cabeçalho escrito. */
    @FunctionalInterface
    private interface CorpoExperiencia {
        void correr(PrintWriter writer) throws IOException;
    }

    /**
     * Abre o CSV, escreve o cabeçalho, executa o corpo da experiência e trata os erros de I/O.
     *
     * @param csvFileName nome do ficheiro CSV a criar.
     * @param cabecalho   linha de cabeçalho do CSV.
     * @param corpo       código que escreve as linhas de dados.
     */
    private static void executarExperiencia(String csvFileName, String cabecalho, CorpoExperiencia corpo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            writer.println(cabecalho);
            corpo.correr(writer);
            System.out.println("Experiência concluída. Resultados em: " + csvFileName);
        } catch (IOException e) {
            System.err.println("Erro ao abrir ou ler ficheiros: " + e.getMessage());
        }
    }

    /**
     * Corre o MCTS uma vez com a semente dada e mede o tempo.
     * Os custos valem -1 se não foi encontrada solução.
     *
     * @param startNode     estado inicial do puzzle.
     * @param seed          semente do gerador aleatório do simulador.
     * @param eps           probabilidade de passo aleatório no rollout.
     * @param cMax          custo máximo permitido por simulação.
     * @param n             número de rollouts por simulação.
     * @param maxIterations número máximo de iterações do MCTS.
     * @param exaustos      se {@code true}, ativa o rastreio de nós exaustos.
     * @return o resultado da execução (simulador, custos, tempo e se foi resolvido).
     */
    private static Execucao correr(BallSortLayout startNode, int seed, double eps, double cMax, int n,
                                   int maxIterations, boolean exaustos) {
        Simulator sim = new Simulator(n, cMax, new Random(seed), eps);
        MCTS mcts = new MCTS(cMax, maxIterations, sim, cMax + MAX_DEPTH, C, MAX_DEPTH, exaustos);

        long start = System.currentTimeMillis();
        // O objetivo é nulo porque isGoal() de BallSortLayout avalia as próprias cores
        Iterator<MCTS.State> sol = mcts.solve(startNode, null);
        long elapsed = System.currentTimeMillis() - start;

        boolean solved = (sol != null);
        return new Execucao(sim,
                solved ? mcts.getLastRawCost() : -1.0,
                solved ? mcts.getLastFinalCost() : -1.0,
                elapsed, solved);
    }

    /**
     * Compara o MCTS com e sem rastreio de nós exaustos na instância {@code muito_dificil},
     * com as sementes 1 a 30 por configuração, e escreve uma linha por execução em
     * {@code exp_exaustos.csv} (custo bruto e final, ótimo, tempo em ms e se foi resolvido).
     */
    private static void experienciaExaustos() {
        executarExperiencia("exp_exaustos.csv",
                "instancia,configuracao,seed,epsilon,c,cMax,N,maxDepth,iteracoes,exaustos,custo_bruto,custo,otimo,tempo_ms,resolvido",
                writer -> {
                    BallSortLayout startNode = new BallSortLayout(lerFicheiro(FICHEIRO_MUITO_DIFICIL));

                    for (boolean exaustos : new boolean[]{true, false}) {
                        String configLabel = "exaustos" + exaustos;

                        for (int seed = 1; seed <= 30; seed++) {
                            Execucao r = correr(startNode, seed, EPSILON, CMAX, N, MAX_ITERATIONS, exaustos);

                            writer.printf(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.1f,%d,%d,%d,%b,%.1f,%.1f,%.1f,%d,%b%n",
                                    LABEL_MUITO_DIFICIL, configLabel, seed, EPSILON, C, CMAX, N, MAX_DEPTH,
                                    MAX_ITERATIONS, exaustos, r.custoBruto(), r.custo(), OTIMO_MUITO_DIFICIL,
                                    r.tempoMs(), r.resolvido());
                            writer.flush();

                            System.out.printf(Locale.ROOT, "Concluído: %s | seed=%d | custo=%.0f (bruto %.0f) | tempo=%dms%n",
                                    configLabel, seed, r.custo(), r.custoBruto(), r.tempoMs());
                        }
                    }
                });
    }

    /**
     * Varia cMax e N na instância {@code muito_dificil}, com epsilon = 0.1, c = 0.3, maxDepth = 100
     * e nós exaustos ligados, e escreve uma linha por execução em {@code exp_parametros.csv}.
     * <p>
     * Configurações (cMax, N, iterações): (130, 1, 15000), (200, 1, 15000), (325, 1, 15000),
     * (500, 1, 15000), (500, 3, 5000) e (500, 5, 3000). Nas variações de N, o número de iterações
     * é 15000 / N, para que o número total de rollouts seja o mesmo em todas as configurações.
     * Além do custo e do tempo, regista quantos rollouts foram feitos, quantos foram cortados
     * em cMax e quantos acabaram num beco sem saída.
     *
     * @param numSeeds Número de sementes por configuração (sementes 1 a numSeeds).
     * @pre numSeeds &gt;= 1
     */
    private static void experienciaParametros(int numSeeds) {
        // {cMax, N, iterações}
        int[][] configs = {
                {130, 1, 15000},
                {200, 1, 15000},
                {325, 1, 15000},
                {500, 1, 15000},
                {500, 3, 5000},
                {500, 5, 3000}
        };

        executarExperiencia("exp_parametros.csv",
                "instancia,configuracao,seed,epsilon,c,cMax,N,maxDepth,iteracoes,custo_bruto,custo,otimo,tempo_ms,resolvido,rollouts,cortados,becos",
                writer -> {
                    BallSortLayout startNode = new BallSortLayout(lerFicheiro(FICHEIRO_MUITO_DIFICIL));

                    for (int[] cfg : configs) {
                        double cMax = cfg[0];
                        int n = cfg[1];
                        int maxIterations = cfg[2];
                        String configLabel = "cmax" + cfg[0] + "_n" + n;

                        for (int seed = 1; seed <= numSeeds; seed++) {
                            Execucao r = correr(startNode, seed, EPSILON, cMax, n, maxIterations, true);
                            Simulator sim = r.sim();

                            writer.printf(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.1f,%d,%d,%d,%.1f,%.1f,%.1f,%d,%b,%d,%d,%d%n",
                                    LABEL_MUITO_DIFICIL, configLabel, seed, EPSILON, C, cMax, n, MAX_DEPTH, maxIterations,
                                    r.custoBruto(), r.custo(), OTIMO_MUITO_DIFICIL, r.tempoMs(), r.resolvido(),
                                    sim.getRolloutCount(), sim.getCutCount(), sim.getDeadEndCount());
                            writer.flush();

                            System.out.printf(Locale.ROOT, "Concluído: %s | seed=%d | custo=%.0f | cortados=%d/%d | tempo=%dms%n",
                                    configLabel, seed, r.custo(), sim.getCutCount(), sim.getRolloutCount(), r.tempoMs());
                        }
                    }
                });
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
        double[] epsilons = {0.0, 0.1, 0.3, 1.0};
        String[] files = {"instancias/dificil_opt37.txt", FICHEIRO_MUITO_DIFICIL};
        String[] labels = {"dificil", LABEL_MUITO_DIFICIL};
        double[] otimos = {37.0, OTIMO_MUITO_DIFICIL};

        executarExperiencia("mcts_experiments_results.csv",
                "instancia,configuracao,seed,epsilon,c,cMax,N,maxDepth,iteracoes,custo,otimo,tempo_ms,resolvido",
                writer -> {
                    for (int f = 0; f < files.length; f++) {
                        BallSortLayout startNode = new BallSortLayout(lerFicheiro(files[f]));

                        for (double eps : epsilons) {
                            String configLabel = "eps" + eps;

                            for (int seed = 1; seed <= 30; seed++) {
                                Execucao r = correr(startNode, seed, eps, CMAX, N, MAX_ITERATIONS, true);

                                writer.printf(Locale.ROOT, "%s,%s,%d,%.2f,%.2f,%.1f,%d,%d,%d,%.1f,%.1f,%d,%b%n",
                                        labels[f], configLabel, seed, eps, C, CMAX, N, MAX_DEPTH,
                                        MAX_ITERATIONS, r.custo(), otimos[f], r.tempoMs(), r.resolvido());
                                writer.flush();

                                System.out.printf(Locale.ROOT, "Concluído: %s | %s | seed=%d | tempo=%dms%n",
                                        labels[f], configLabel, seed, r.tempoMs());
                            }
                        }
                    }
                });
    }
}
