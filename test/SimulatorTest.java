import org.junit.jupiter.api.Test;
import puzzles.BallSortLayout;
import search.Simulator;
import search.SimResult;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

public class SimulatorTest {

    @Test
    public void shouldThrowException_whenInstantiatingWithInvalidParameters() {
        Random rnd = new Random();

        assertThatThrownBy(() -> new Simulator(0, 50.0, rnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n tem de ser n >=1");

        assertThatThrownBy(() -> new Simulator(10, -5.0, rnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cMax tem de ser n > 0");
    }

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
        BallSortLayout deadEndLayout = new BallSortLayout("A B \nB A");
        BallSortLayout goalLayout = new BallSortLayout("A B E\nA B E");

        SimResult result = simulator.simulate(deadEndLayout, goalLayout);

        assertThat(result.isSolved()).isFalse();
        assertThat(result.getCost()).isEqualTo(50.0);
        assertThat(result.getPath()).isNull();
    }

    @Test
    public void shouldThrowNullPointerException_whenRandomIsNull() {
        Simulator simulator = new Simulator(1, 10.0, null);
        BallSortLayout layout = new BallSortLayout("A B E\nB A E");

        assertThatThrownBy(() -> simulator.simulate(layout, layout))
                .isInstanceOf(NullPointerException.class);
    }
}