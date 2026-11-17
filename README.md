# Ball Sort com Monte Carlo Tree Search

![CI](https://github.com/kakurokan/ballsort-mcts/actions/workflows/ci.yml/badge.svg) ![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)

Solucionador do puzzle **Ball Sort** em Java, usando **Monte Carlo Tree Search (MCTS)**
com rollouts ε-greedy guiados por heurística. Inclui também uma pesquisa **A\*** de
referência e um conjunto de experiências para avaliar a influência dos parâmetros.

## O puzzle

Existem vários tubos (pilhas) com a mesma capacidade, preenchidos com bolas de várias cores.
Um movimento transfere a bola do topo de um tubo para outro tubo que não esteja cheio e que
esteja vazio ou tenha uma bola da mesma cor no topo. O objetivo é que cada tubo fique vazio ou
cheio com uma só cor. Cada movimento custa 1, pelo que o custo de uma solução é o seu número de
movimentos.

## Estrutura do projeto

```
src/
├── core/Ilayout.java            Interface de uma configuração (sucessores, custo, heurística, objetivo)
├── puzzles/BallSortLayout.java  Configuração do Ball Sort
├── search/
│   ├── MCTS.java                MCTS (seleção UCB, expansão, simulação, retropropagação)
│   ├── Simulator.java           Rollouts ε-greedy; estima o custo a partir de um nó
│   ├── SimResult.java           Resultado de uma simulação (custo e caminho)
│   └── AStarSearch.java         Pesquisa A* de referência
└── app/
    ├── Main.java                Resolve uma instância lida do standard input
    └── Experiments.java         Benchmarks que escrevem ficheiros CSV
test/                            Testes JUnit 5 + AssertJ
instancias/                      Instâncias de teste (o sufixo _optN indica o custo ótimo N)
```

## Resultados

Instância `muito_dificil` (custo ótimo conhecido: 65 movimentos), 30 sementes por configuração,
15 000 iterações, `epsilon = 0.1`, `c = 0.3`, `maxDepth = 100`. Todas as execuções encontraram solução.

| Configuração    | Custo médio | Melhor | Tempo médio |
|-----------------|-------------|--------|-------------|
| `cMax=130, n=1` | 67.8        | 67     | 45 s        |
| `cMax=200, n=1` | 68.1        | 66     | 45 s        |
| `cMax=325, n=1` | 69.6        | 67     | 51 s        |
| `cMax=500, n=1` | 70.6        | 68     | 62 s        |
| `cMax=500, n=3` | 70.5        | 69     | 72 s        |
| `cMax=500, n=5` | 70.6        | 69     | 79 s        |

Conclusões: um `cMax` mais baixo dá soluções melhores e mais rápidas (os rollouts longos
acrescentam ruído), e mais rollouts por simulação (`n`) só aumentam o tempo. Na instância `dificil`
(ótimo 37) o MCTS encontra o ótimo com os parâmetros por omissão. Os CSV completos são gerados
pelo modo `parametros` das experiências (ver abaixo).

## Como funciona

Em cada iteração do MCTS:

1. **Seleção**: desce da raiz até uma folha, escolhendo o filho não esgotado com maior UCB.
2. **Expansão**: cria os filhos da folha (até `maxDepth`).
3. **Simulação**: para cada filho, o `Simulator` faz `n` rollouts e devolve o de menor custo.
   Em cada passo, com probabilidade `epsilon` o movimento é aleatório; caso contrário
   escolhe-se o sucessor de menor heurística.
4. **Retropropagação**: atualiza visitas e o melhor custo estimado do nó e dos seus antecessores.

A melhor solução encontrada é guardada, limpa de ciclos e devolvida no fim. Com o rastreio de
**nós esgotados** ativo, os ramos totalmente explorados deixam de ser selecionados e a pesquisa
termina cedo quando a árvore fica toda explorada.

## Compilar

Requer JDK 17 ou superior (o código usa `record`).

```bash
mkdir -p out/production
javac -d out/production $(find src -name '*.java')
```

Em alternativa, com Maven: `mvn package` (compila e corre os testes).

## Executar

### Resolver uma instância

O `Main` lê do standard input a **altura**, o **número de tubos** e depois uma linha por nível
(do topo para a base), com as bolas separadas por espaços e `E` nas posições vazias:

```
4 3
A B E
B A E
A B E
B A E
```

```bash
java -cp out/production app.Main [cMax] [n] [c] [iterations] [maxDepth] [seed] [epsilon] < entrada.txt
```

| Argumento    | Significado                                     | Omissão |
|--------------|-------------------------------------------------|---------|
| `cMax`       | Custo máximo de um rollout                      | 130     |
| `n`          | Rollouts por simulação                          | 1       |
| `c`          | Constante de exploração do UCB                  | 0.3     |
| `iterations` | Número máximo de iterações                      | 15000   |
| `maxDepth`   | Profundidade máxima da árvore                   | 60      |
| `seed`       | Semente do gerador aleatório                    | 42      |
| `epsilon`    | Probabilidade de passo aleatório no rollout     | 0.1     |

A saída é o custo (número de movimentos) da solução, ou `no solution found`.

Os ficheiros em `instancias/` não têm a primeira linha com a altura e o número de tubos; para os
usar com o `Main`, acrescenta-a:

```bash
f=instancias/dificil_opt37.txt
(echo "$(grep -c '' $f) $(head -1 $f | wc -w)"; cat $f) | java -cp out/production app.Main
# 37  (custo ótimo, ~20 s com os parâmetros por omissão)
```

### Experiências

Executar a partir da raiz do repositório (as instâncias são lidas de `instancias/`):

```bash
java -cp out/production app.Experiments [epsilon|exaustos|parametros] [numSementes]
```

| Modo                    | O que faz                                                         | Saída                           |
|-------------------------|-------------------------------------------------------------------|---------------------------------|
| `epsilon` (por omissão) | Varia epsilon (0.0, 0.1, 0.3, 1.0) em duas instâncias, sementes 1–30 | `mcts_experiments_results.csv` |
| `exaustos`              | Compara o MCTS com e sem rastreio de nós esgotados                | `exp_exaustos.csv`              |
| `parametros`            | Varia `cMax` (130–500) e `n` (1, 3, 5); `numSementes` por omissão 30 | `exp_parametros.csv`        |

## Testes

Os testes (41 casos, JUnit 5 + AssertJ) cobrem o `BallSortLayout`, o `Simulator` e o `MCTS`.

```bash
mvn test
```

A integração contínua (GitHub Actions) corre-os em cada push.

## Documentação

O código está documentado com Javadoc. As tags `@pre`, `@pos` e `@inv` são personalizadas, por
isso têm de ser registadas ao gerar a documentação:

```bash
javadoc -private -d docs -sourcepath src -subpackages app:core:puzzles:search \
  -tag pre:a:"Pré-condição:" -tag pos:a:"Pós-condição:" -tag inv:a:"Invariante:"
```

## Relatório

O relatório do trabalho, com a análise das experiências, está em [docs/Relatorio.pdf](docs/Relatorio.pdf).

## Licença

Ver [LICENSE](LICENSE).
