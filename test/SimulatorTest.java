import search.Simulator;
import search.SimResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import puzzles.BallSortLayout;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários do {@link Simulator}: validação de parâmetros de instanciação
 * e desfechos de simulação (objetivo, limite cMax, beco sem saída e política epsilon-greedy).
 */
public class SimulatorTest {

    @Nested
    class InstantiationTests {

        @Test
        public void shouldThrowException_whenNIsInvalid() {
            Random rnd = new Random();

            assertThatThrownBy(() -> new Simulator(0, 50.0, rnd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("n tem de ser n >=1");

            assertThatThrownBy(() -> new Simulator(-5, 50.0, rnd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("n tem de ser n >=1");
        }

        @Test
        public void shouldThrowException_whenCMaxIsInvalid() {
            Random rnd = new Random();

            assertThatThrownBy(() -> new Simulator(10, 0.0, rnd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cMax tem de ser n > 0");

            assertThatThrownBy(() -> new Simulator(10, -10.0, rnd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cMax tem de ser n > 0");
        }

        @Test
        public void shouldThrowException_whenEpsilonIsOutOfRange() {
            Random rnd = new Random();

            assertThatThrownBy(() -> new Simulator(10, 50.0, rnd, -0.1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("epsilon tem de estar em [0, 1]");

            assertThatThrownBy(() -> new Simulator(10, 50.0, rnd, 1.05))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("epsilon tem de estar em [0, 1]");
        }

        @Test
        public void shouldThrowException_whenEpsilonIsNaN() {
            Random rnd = new Random();

            assertThatThrownBy(() -> new Simulator(10, 50.0, rnd, Double.NaN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("epsilon não pode ser NaN");
        }

        @Test
        public void shouldInstantiateSuccessfully_whenEpsilonIsAtBoundaries() {
            Random rnd = new Random();

            assertThatCode(() -> new Simulator(5, 50.0, rnd, 0.0))
                    .doesNotThrowAnyException();

            assertThatCode(() -> new Simulator(5, 50.0, rnd, 1.0))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class RolloutBehaviorTests {

        @Test
        public void shouldReturnZeroCostAndEmptyPath_whenStartIsGoal() {
            Simulator simulator = new Simulator(5, 50.0, new Random());
            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(goalLayout, goalLayout);

            assertThat(result.isSolved()).isTrue();
            assertThat(result.getCost()).isEqualTo(0.0);
            assertThat(result.getPath()).isEmpty();
        }

        @Test
        public void shouldReturnCMaxAndNullPath_whenSimulationReachesMaxCostWithoutSolving() {
            Simulator simulator = new Simulator(1, 2.0, new Random(42));
            BallSortLayout startLayout = new BallSortLayout("A B E E\nB A E E");
            BallSortLayout goalLayout = new BallSortLayout("A B E E\nA B E E");

            SimResult result = simulator.simulate(startLayout, goalLayout);

            assertThat(result.isSolved()).isFalse();
            assertThat(result.getCost()).isEqualTo(2.0);
            assertThat(result.getPath()).isNull();
        }

        @Test
        public void shouldReturnCMaxAndNullPath_whenStuckInDeadEnd() {
            Simulator simulator = new Simulator(1, 50.0, new Random());
            BallSortLayout deadEndLayout = new BallSortLayout("A B\nB A");
            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(deadEndLayout, goalLayout);

            assertThat(result.isSolved()).isFalse();
            assertThat(result.getCost()).isEqualTo(50.0);
            assertThat(result.getPath()).isNull();
        }

        @Test
        public void shouldSolveSuccessfully_whenUsingPureGreedyPolicy() {
            Simulator simulator = new Simulator(1, 50.0, new Random(42), 0.0);
            BallSortLayout startLayout = new BallSortLayout("A B E\nB A E");
            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(startLayout, goalLayout);

            assertThat(result.isSolved()).isTrue();
            assertThat(result.getCost()).isLessThan(50.0);
            assertThat(result.getPath()).isNotNull().isNotEmpty();
        }

        @Test
        public void shouldThrowNullPointerException_whenRandomIsNull() {
            Simulator simulator = new Simulator(1, 10.0, null);
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");

            assertThatThrownBy(() -> simulator.simulate(layout, layout))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldPreferSolvedRollout_whenCostsAreEqual() {
            Simulator simulator = new Simulator(100, 2.0, new Random(42));
            BallSortLayout startLayout = new BallSortLayout("E E A\nA B B");
            BallSortLayout goalLayout  = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(startLayout, goalLayout);

            assertThat(result.getCost()).isEqualTo(2.0);
            assertThat(result.isSolved()).isTrue();
            assertThat(result.getPath()).hasSize(2);
        }

        @Test
        public void shouldPerformPureRandomRollouts_whenEpsilonIsOne() {
            Simulator simulator = new Simulator(5, 50.0, new Random(42), 1.0);
            BallSortLayout startLayout = new BallSortLayout("A B E\nB A E");
            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(startLayout, goalLayout);
            assertThat(result).isNotNull();
            assertThat(result.getCost()).isGreaterThan(0.0);
        }

        @Test
        public void shouldReturnCMax_whenOnlyMovementIsToUndoPreviousStep() {
            Simulator simulator = new Simulator(1, 50.0, new Random(42));
            BallSortLayout deadEndLayout = new BallSortLayout("A B\nB A");
            BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

            SimResult result = simulator.simulate(deadEndLayout, goalLayout);

            assertThat(result.isSolved()).isFalse();
            assertThat(result.getCost()).isEqualTo(50.0);
            assertThat(result.getPath()).isNull();
        }
    }
}