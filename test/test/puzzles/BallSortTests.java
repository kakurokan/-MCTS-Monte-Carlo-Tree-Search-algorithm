package test.puzzles;

import org.junit.jupiter.api.Test;
import puzzles.BallSortLayout;
import search.BestFirst;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BallSortTests {

    @Test
    public void testSample1() {
        // A string reflete apenas as linhas do tabuleiro, sem as dimensões iniciais.
        String config = """
                R B E
                B B E
                R R E
                """;

        BallSortLayout initialLayout = new BallSortLayout(config);
        BestFirst solver = new BestFirst();

        // O segundo argumento é null porque o layout valida o isGoal() internamente
        Iterator<BestFirst.State> path = solver.solve(initialLayout, null);

        assertNotNull(path, "O solver deveria encontrar uma solução para o sample 1.");

        int finalCost = 0;
        while (path.hasNext()) {
            BestFirst.State state = path.next();
            // Quando for o último estado do iterador, capturamos o custo total
            if (!path.hasNext()) {
                finalCost = (int) state.getG();
            }
        }

        assertEquals(5, finalCost, "O número de movimentos mínimos necessários é 5.");
    }

    @Test
    public void testSample2() {
        String config = """
                P R B E
                R B R E
                P B P E
                """;

        BallSortLayout initialLayout = new BallSortLayout(config);
        BestFirst solver = new BestFirst();

        Iterator<BestFirst.State> path = solver.solve(initialLayout, null);

        assertNotNull(path, "O solver deveria encontrar uma solução para o sample 2.");

        int finalCost = 0;
        while (path.hasNext()) {
            BestFirst.State state = path.next();
            if (!path.hasNext()) {
                finalCost = (int) state.getG();
            }
        }

        assertEquals(6, finalCost, "O número de movimentos mínimos necessários é 6.");
    }
}