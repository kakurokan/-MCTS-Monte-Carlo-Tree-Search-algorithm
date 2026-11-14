# Ball Sort com Monte Carlo Tree Search

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
| `cMax`       | Custo máximo de um rollout                      | 500     |
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
(echo "$(wc -l < $f) $(head -1 $f | wc -w)"; cat $f) | java -cp out/production app.Main
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

Os testes usam JUnit 5 e AssertJ. Correm-se pelo IDE (IntelliJ) ou com um executor JUnit à
escolha, tendo as bibliotecas no classpath.

## Documentação

O código está documentado com Javadoc. As tags `@pre`, `@pos` e `@inv` são personalizadas, por
isso têm de ser registadas ao gerar a documentação:

```bash
javadoc -private -d docs -sourcepath src -subpackages app:core:puzzles:search \
  -tag pre:a:"Pré-condição:" -tag pos:a:"Pós-condição:" -tag inv:a:"Invariante:"
```

## Licença

Ver [LICENSE](LICENSE).
