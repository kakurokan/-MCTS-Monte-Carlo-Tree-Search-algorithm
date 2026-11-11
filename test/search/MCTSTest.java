package search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import puzzles.BallSortLayout;

import java.util.Iterator;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários e de integração do {@link MCTS} e da sua classe interna {@link MCTS.State}.
 * - validação de parâmetros, recompensa, atualização de custos, expansão e retropropagação.
 */
public class MCTSTest {

    @Nested
    class InstantiationTests {

        @Test
        public void shouldThrowException_whenMaxIterationsIsInvalid() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatThrownBy(() -> new MCTS(10.0, 0, sim, 20.0, 0.2, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");

            assertThatThrownBy(() -> new MCTS(10.0, -5, sim, 20.0, 0.2, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");
        }

        @Test
        public void shouldThrowException_whenCMaxIsInvalid() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatThrownBy(() -> new MCTS(0.0, 100, sim, 20.0, 0.2, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");

            assertThatThrownBy(() -> new MCTS(-10.0, 100, sim, 20.0, 0.2, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");
        }

        @Test
        public void shouldThrowException_whenMaxDepthIsInvalid() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatThrownBy(() -> new MCTS(10.0, 100, sim, 20.0, 0.2, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");
        }

        @Test
        public void shouldThrowException_whenCIsNegative() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatThrownBy(() -> new MCTS(10.0, 100, sim, 20.0, -0.1, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parâmetros inválidos");
        }

        @Test
        public void shouldThrowException_whenFMaxIsLessThanMaxDepthPlusCMax() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatThrownBy(() -> new MCTS(10.0, 100, sim, 5.0, 0.2, 5, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fMax inválido. fMax deve ser maior ou igual à maxDepth + cMax");
        }

        @Test
        public void shouldNotThrowException_whenFMaxIsExactlyMaxDepthPlusCMax() {
            Simulator sim = new Simulator(1, 10.0, new Random());

            assertThatCode(() -> new MCTS(10.0, 100, sim, 15.0, 0.2, 5, true))
                    .doesNotThrowAnyException();
        }

        @Test
        public void shouldThrowException_whenSimulatorIsNull() {
            assertThatThrownBy(() -> new MCTS(10.0, 100, null, 20.0, 0.2, 10, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("simulator não pode ser nulo");
        }
    }

    @Nested
    class StateUnitTests {

        @Test
        public void shouldInitializeRootStateCorrectly() {
            BallSortLayout layout = new BallSortLayout("A B E\nA B E");
            double cMax = 50.0;

            MCTS.State root = new MCTS.State(layout, null, cMax);

            assertThat(root.getG()).isEqualTo(0.0);
            assertThat(root.getVisitCount()).isEqualTo(0);
            assertThat(root.getSim()).isEqualTo(cMax);
            assertThat(root.getFather()).isNull();
            assertThat(root.isLeaf()).isTrue();
            assertThat(root.isExhausted()).isFalse();
            assertThat(root.getLayout()).isEqualTo(layout);
        }

        @Test
        public void shouldInitializeChildStateCorrectly() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            double cMax = 50.0;

            MCTS.State root = new MCTS.State(layout, null, cMax);
            MCTS.State child = new MCTS.State(layout, root, cMax);

            assertThat(child.getG()).isEqualTo(1.0);
            assertThat(child.getFather()).isSameAs(root);
            assertThat(child.getVisitCount()).isEqualTo(0);
            assertThat(child.getSim()).isEqualTo(cMax);
        }

        @Test
        public void shouldKeepLowestSimCost_whenUpdatingSim() {
            BallSortLayout layout = new BallSortLayout("A B E\nA B E");
            MCTS.State state = new MCTS.State(layout, null, 50.0);

            state.updateSim(30.0);
            assertThat(state.getSim()).isEqualTo(30.0);

            state.updateSim(40.0);
            assertThat(state.getSim()).isEqualTo(30.0);

            state.updateSim(15.0);
            assertThat(state.getSim()).isEqualTo(15.0);
        }

        @Test
        public void shouldCalculateCorrectReward_whenValidCostsAreGiven() {
            BallSortLayout layout = new BallSortLayout("A B E\nA B E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            MCTS.State child1 = new MCTS.State(layout, root, 50.0);
            MCTS.State child2 = new MCTS.State(layout, child1, 50.0);

            child2.updateSim(20.0);

            double reward = child2.reward(100.0);
            assertThat(reward).isCloseTo(0.78, within(0.0001));
        }

        @Test
        public void shouldCalculateCorrectUCBValue() {
            BallSortLayout layout = new BallSortLayout("A B E\nA B E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            root.addVisit();

            MCTS.State child = new MCTS.State(layout, root, 50.0);
            child.addVisit();
            child.updateSim(0.0);

            double ucbVal = child.ucb(10.0, 0.2, root.getVisitCount());
            assertThat(ucbVal).isCloseTo(0.9, within(0.0001));
        }

        @Test
        public void shouldStopExpanding_whenMaxDepthIsReached() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            MCTS.State child1 = new MCTS.State(layout, root, 50.0);
            MCTS.State child2 = new MCTS.State(layout, child1, 50.0);

            child2.expand(50.0, 2);

            assertThat(child2.getChildren()).isEmpty();
        }

        @Test
        public void shouldExpandAndGenerateChildren_whenDepthIsJustBelowMaxDepth() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            MCTS.State child1 = new MCTS.State(layout, root, 50.0);

            child1.expand(50.0, 2);

            assertThat(child1.getChildren()).isNotEmpty();
        }

        @Test
        public void shouldPropagateExhaustionToFather_whenAllChildrenAreExhausted() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);

            root.expand(50.0, 5);

            for (MCTS.State c : root.getChildren()) {
                c.markExhausted();
            }

            assertThat(root.isExhausted()).isTrue();
        }

        @Test
        public void shouldNotExhaustFather_whenAtLeastOneChildIsStillActive() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);

            root.expand(50.0, 5);
            assertThat(root.getChildren().size()).isGreaterThan(1);

            root.getChildren().get(0).markExhausted();

            assertThat(root.isExhausted()).isFalse();
        }
    }

    @Nested
    class MCTSBehaviorTests {

        @Test
        public void shouldUpdateAncestorVisitsAndCosts_whenBackpropagating() {
            Simulator sim = new Simulator(1, 50.0, new Random());
            MCTS mcts = new MCTS(50.0, 100, sim, 100.0, 0.2, 10, true);

            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            MCTS.State child = new MCTS.State(layout, root, 50.0);

            mcts.backpropagate(child, 10.0);

            assertThat(child.getVisitCount()).isEqualTo(1);
            assertThat(root.getVisitCount()).isEqualTo(1);
            assertThat(root.getSim()).isEqualTo(11.0);
        }

        @Test
        public void shouldSelectLeafNode_whenNavigatingTree() {
            Simulator sim = new Simulator(1, 50.0, new Random());
            MCTS mcts = new MCTS(50.0, 100, sim, 100.0, 0.2, 10, true);

            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            root.expand(50.0, 5);

            for (MCTS.State child : root.getChildren()) {
                mcts.backpropagate(child, 20.0);
            }

            MCTS.State selectedLeaf = mcts.select(root);

            assertThat(selectedLeaf).isNotNull();
            assertThat(selectedLeaf.isLeaf()).isTrue();
        }

        @Test
        public void shouldIgnoreExhaustedChildren_whenSelecting() {
            Simulator sim = new Simulator(1, 50.0, new Random());
            MCTS mcts = new MCTS(50.0, 100, sim, 100.0, 0.2, 10, true);

            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            MCTS.State root = new MCTS.State(layout, null, 50.0);
            root.expand(50.0, 5);

            for (MCTS.State child : root.getChildren()) {
                mcts.backpropagate(child, 10.0);
            }

            MCTS.State firstChild = root.getChildren().get(0);
            firstChild.markExhausted();

            MCTS.State selected = mcts.select(root);

            assertThat(selected).isNotEqualTo(firstChild);
            assertThat(selected.isExhausted()).isFalse();
        }

        @Test
        public void shouldSolvePuzzle_whenStartIsGoal() {
            Simulator sim = new Simulator(1, 50.0, new Random());
            MCTS mcts = new MCTS(50.0, 100, sim, 100.0, 0.2, 10, true);

            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            Iterator<MCTS.State> solution = mcts.solve(goalLayout, goalLayout);

            assertThat(solution).isNotNull();
            assertThat(solution.hasNext()).isTrue();
            MCTS.State firstState = solution.next();
            assertThat(firstState.getLayout()).isEqualTo(goalLayout);
        }

        @Test
        public void shouldReturnValidPathAndNeverExhaustRoot_whenUseExhaustedIsFalse() {
            Simulator sim = new Simulator(1, 50.0, new Random(42), 0.0);
            MCTS mcts = new MCTS(50.0, 50, sim, 100.0, 0.2, 5, false);

            BallSortLayout startLayout = new BallSortLayout("A B E\nA E B");
            BallSortLayout goalLayout = new BallSortLayout("A E B\nA E B");

            Iterator<MCTS.State> solution = mcts.solve(startLayout, goalLayout);

            assertThat(solution).isNotNull();
            java.util.List<MCTS.State> path = new java.util.ArrayList<>();
            while (solution.hasNext()) {
                path.add(solution.next());
            }

            assertThat(path).isNotEmpty();
            assertThat(path.get(0).getLayout()).isEqualTo(startLayout);
            assertThat(path.get(path.size() - 1).getLayout().isGoal(goalLayout)).isTrue();
            assertThat(mcts.getLastRoot()).isNotNull();
            assertThat(mcts.getLastRoot().isExhausted()).isFalse();
        }

        @Test
        public void shouldHaveRawCostGreaterOrEqualToFinalCost_afterSolving() {
            Simulator sim = new Simulator(1, 50.0, new Random(42), 0.0);
            MCTS mcts = new MCTS(50.0, 50, sim, 100.0, 0.2, 5, true);

            BallSortLayout startLayout = new BallSortLayout("A B E\nA E B");
            BallSortLayout goalLayout = new BallSortLayout("A E B\nA E B");

            assertThat(mcts.solve(startLayout, goalLayout)).isNotNull();

            assertThat(mcts.getLastRawCost()).isFinite();
            assertThat(mcts.getLastFinalCost()).isFinite();
            assertThat(mcts.getLastRawCost()).isGreaterThanOrEqualTo(mcts.getLastFinalCost());
        }

        @Test
        public void shouldReturnNull_whenNoSolutionIsFound() {
            BallSortLayout deadEnd = new BallSortLayout("A B C\nB C A");
            BallSortLayout goal = new BallSortLayout("A B C\nA B C");

            Simulator sim = new Simulator(1, 10.0, new Random(42));
            MCTS mcts = new MCTS(10.0, 10, sim, 20.0, 0.2, 2, true);

            Iterator<MCTS.State> solution = mcts.solve(deadEnd, goal);

            assertThat(solution).isNull();
        }

        @Test
        public void shouldSolvePuzzle_whenGoalIsReachableInOneMove() {
            Simulator sim = new Simulator(1, 50.0, new Random(42), 0.0);
            MCTS mcts = new MCTS(50.0, 50, sim, 100.0, 0.2, 5, true);

            BallSortLayout startLayout = new BallSortLayout("A B E\nA E B");
            BallSortLayout goalLayout = new BallSortLayout("A E B\nA E B");

            Iterator<MCTS.State> solution = mcts.solve(startLayout, goalLayout);

            assertThat(solution).isNotNull();

            java.util.List<MCTS.State> path = new java.util.ArrayList<>();
            while (solution.hasNext()) {
                path.add(solution.next());
            }

            assertThat(path).hasSize(2);

            assertThat(path.get(0).getLayout()).isEqualTo(startLayout);
            assertThat(path.get(1).getLayout().isGoal(goalLayout)).isTrue();

            java.util.Set<core.Ilayout> visitedLayouts = new java.util.HashSet<>();
            for (MCTS.State s : path) {
                assertThat(visitedLayouts.add(s.getLayout()))
                        .as("O caminho devolvido não deve conter ciclos/configurações repetidas")
                        .isTrue();
            }
        }
    }
}