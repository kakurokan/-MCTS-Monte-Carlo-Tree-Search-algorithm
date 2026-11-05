import search.Simulator;
import search.MCTS;
import org.junit.jupiter.api.Test;
import puzzles.BallSortLayout;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários do {@link MCTS} e do seu estado interno {@link MCTS.State}:
 * validação de parâmetros, recompensa, atualização de custos, expansão e retropropagação.
 */
public class MCTSTest {

    @Test
    public void shouldThrowException_whenFMaxIsLessThanMaxDepthPlusCMax() {
        Simulator sim = new Simulator(1, 10.0, new Random());

        assertThatThrownBy(() -> new MCTS(10.0, 100, sim, 5.0, 0.2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fMax inválido. fMax deve ser maior ou igual à maxDepth + cMax");
    }

    @Test
    public void shouldCalculateCorrectReward_whenValidCostsAreGiven() {
        BallSortLayout layout = new BallSortLayout("A B E\nA B E");
        MCTS.State root = new MCTS.State(layout, null, 50.0);

        root.updateSim(0.0);

        double reward = root.reward(100.0);
        assertThat(reward).isEqualTo(1.0);
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
    public void shouldStopExpanding_whenMaxDepthIsReached() {
        BallSortLayout layout = new BallSortLayout("A B E\nB A E");
        MCTS.State root = new MCTS.State(layout, null, 50.0);
        MCTS.State child1 = new MCTS.State(layout, root, 50.0);
        MCTS.State child2 = new MCTS.State(layout, child1, 50.0);

        child2.expand(50.0, 2);

        assertThat(child2.getChildren()).isEmpty();
    }

    @Test
    public void shouldUpdateAncestorVisitsAndCosts_whenBackpropagating() {
        Simulator sim = new Simulator(1, 50.0, new Random());
        MCTS mcts = new MCTS(50.0, 100, sim, 100.0, 0.2, 10);

        BallSortLayout layout = new BallSortLayout("A B E\nB A E");
        MCTS.State root = new MCTS.State(layout, null, 50.0);
        MCTS.State child = new MCTS.State(layout, root, 50.0);

        mcts.backpropagate(child, 10.0);

        assertThat(child.getVisitCount()).isEqualTo(1);
        assertThat(root.getVisitCount()).isEqualTo(1);
        assertThat(root.getSim()).isEqualTo(11.0);
    }

    @Test
    public void shouldCalculateCorrectReward_whenCostsAreGreaterThanZero() {
        BallSortLayout layout = new BallSortLayout("A B E\nA B E");
        MCTS.State root = new MCTS.State(layout, null, 50.0);

        MCTS.State child1 = new MCTS.State(layout, root, 50.0);
        MCTS.State child2 = new MCTS.State(layout, child1, 50.0);

        child2.updateSim(20.0);

        double reward = child2.reward(100.0);

        assertThat(reward).isCloseTo(0.78, within(0.0001));
    }

    @Test
    public void shouldNotThrowException_whenFMaxIsExactlyMaxDepthPlusCMax() {
        Simulator sim = new Simulator(1, 10.0, new Random());

        assertThatCode(() -> new MCTS(10.0, 100, sim, 15.0, 0.2, 5))
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldExpandAndGenerateChildren_whenDepthIsJustBelowMaxDepth() {
        BallSortLayout layout = new BallSortLayout("A B E\nB A E");
        MCTS.State root = new MCTS.State(layout, null, 50.0);
        MCTS.State child1 = new MCTS.State(layout, root, 50.0);

        child1.expand(50.0, 2);

        assertThat(child1.getChildren()).isNotEmpty();
    }
}