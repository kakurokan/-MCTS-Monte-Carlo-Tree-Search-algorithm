# Ball Sort with Monte Carlo Tree Search

![CI](https://github.com/kakurokan/ballsort-mcts/actions/workflows/ci.yml/badge.svg) ![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)

A **Ball Sort** puzzle solver in Java using **Monte Carlo Tree Search (MCTS)** with
heuristic-guided ε-greedy rollouts. It also includes a reference **A\*** search and a set of
experiments that measure the influence of the parameters.

## The puzzle

There are several tubes (stacks) of equal capacity, filled with balls of various colors.
A move transfers the top ball of one tube to another tube that is not full and that is either
empty or has a ball of the same color on top. The goal is for every tube to be empty or full
with a single color. Each move costs 1, so the cost of a solution is its number of moves.

## Project structure

```
src/
├── core/Ilayout.java            Interface for a configuration (successors, cost, heuristic, goal)
├── puzzles/BallSortLayout.java  Ball Sort configuration
├── search/
│   ├── MCTS.java                MCTS (UCB selection, expansion, simulation, backpropagation)
│   ├── Simulator.java           ε-greedy rollouts; estimates the cost from a node
│   ├── SimResult.java           Result of a simulation (cost and path)
│   └── AStarSearch.java         Reference A* search
└── app/
    ├── Main.java                Solves an instance read from standard input
    └── Experiments.java         Benchmarks that write CSV files
test/                            JUnit 5 + AssertJ tests
instancias/                      Test instances (the _optN suffix is the optimal cost N)
docs/Relatorio.pdf               Project report (in Portuguese)
```

## Results

`muito_dificil` instance (known optimal cost: 65 moves), 30 seeds per configuration,
15,000 iterations, `epsilon = 0.1`, `c = 0.3`, `maxDepth = 100`. Every run found a solution.

| Configuration   | Mean cost | Best | Mean time |
|-----------------|-----------|------|-----------|
| `cMax=130, n=1` | 67.8      | 67   | 45 s      |
| `cMax=200, n=1` | 68.1      | 66   | 45 s      |
| `cMax=325, n=1` | 69.6      | 67   | 51 s      |
| `cMax=500, n=1` | 70.6      | 68   | 62 s      |
| `cMax=500, n=3` | 70.5      | 69   | 72 s      |
| `cMax=500, n=5` | 70.6      | 69   | 79 s      |

Takeaways: a lower `cMax` gives better and faster solutions (long rollouts add noise), and
more rollouts per simulation (`n`) only increase the running time. On the `dificil` instance
(optimal cost 37) MCTS finds the optimum with the default parameters. The full CSV files are
produced by the `parametros` experiment mode (see below).

## How it works

Each MCTS iteration:

1. **Selection**: descend from the root to a leaf, picking the non-exhausted child with the highest UCB.
2. **Expansion**: create the leaf's children (up to `maxDepth`).
3. **Simulation**: for each child, the `Simulator` runs `n` rollouts and returns the cheapest one.
   At each step, with probability `epsilon` the move is random; otherwise the successor with
   the lowest heuristic value is chosen.
4. **Backpropagation**: update the visit counts and the best estimated cost of the node and its ancestors.

The best solution found is kept, stripped of cycles and returned at the end. With
**exhausted-node tracking** enabled, fully explored branches are no longer selected and the
search stops early once the whole tree has been explored.

## Build

Requires JDK 17 or later (the code uses `record`).

```bash
mkdir -p out/production
javac -d out/production $(find src -name '*.java')
```

Alternatively, with Maven: `mvn package` (compiles and runs the tests).

## Run

### Solving an instance

`Main` reads the **height**, the **number of tubes** and then one line per level
(top to bottom) from standard input, with balls separated by spaces and `E` for empty slots:

```
4 3
A B E
B A E
A B E
B A E
```

```bash
java -cp out/production app.Main [cMax] [n] [c] [iterations] [maxDepth] [seed] [epsilon] < input.txt
```

| Argument     | Meaning                                         | Default |
|--------------|-------------------------------------------------|---------|
| `cMax`       | Maximum cost of a rollout                       | 130     |
| `n`          | Rollouts per simulation                         | 1       |
| `c`          | UCB exploration constant                        | 0.3     |
| `iterations` | Maximum number of iterations                    | 15000   |
| `maxDepth`   | Maximum tree depth                              | 60      |
| `seed`       | Random number generator seed                    | 42      |
| `epsilon`    | Probability of a random step in a rollout       | 0.1     |

The output is the cost (number of moves) of the solution, or `no solution found`.

The files in `instancias/` do not have the first line with the height and number of tubes; to
use them with `Main`, prepend it:

```bash
f=instancias/dificil_opt37.txt
(echo "$(grep -c '' $f) $(head -1 $f | wc -w)"; cat $f) | java -cp out/production app.Main
# 37  (optimal cost, ~20 s with the default parameters)
```

### Experiments

Run from the repository root (instances are read from `instancias/`):

```bash
java -cp out/production app.Experiments [epsilon|exaustos|parametros] [numSeeds]
```

| Mode                | What it does                                                          | Output                         |
|---------------------|-----------------------------------------------------------------------|--------------------------------|
| `epsilon` (default) | Varies epsilon (0.0, 0.1, 0.3, 1.0) on two instances, seeds 1–30      | `mcts_experiments_results.csv` |
| `exaustos`          | Compares MCTS with and without exhausted-node tracking                | `exp_exaustos.csv`             |
| `parametros`        | Varies `cMax` (130–500) and `n` (1, 3, 5); `numSeeds` defaults to 30  | `exp_parametros.csv`           |

## Tests

The tests (41 cases, JUnit 5 + AssertJ) cover `BallSortLayout`, `Simulator` and `MCTS`.

```bash
mvn test
```

Continuous integration (GitHub Actions) runs them on every push.

## Documentation

The code is documented with Javadoc (in Portuguese). The `@pre`, `@pos` and `@inv` tags are
custom, so they must be registered when generating the documentation:

```bash
javadoc -private -d docs/api -sourcepath src -subpackages app:core:puzzles:search \
  -tag pre:a:"Pré-condição:" -tag pos:a:"Pós-condição:" -tag inv:a:"Invariante:"
```

## Report

The project report, with the analysis of the experiments, is in
[docs/Relatorio.pdf](docs/Relatorio.pdf) (written in Portuguese).

## License

See [LICENSE](LICENSE).
