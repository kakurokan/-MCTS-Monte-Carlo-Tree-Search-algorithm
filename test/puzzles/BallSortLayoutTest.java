package puzzles;

import core.Ilayout;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class BallSortLayoutTest {

    @Nested
    class RandomChildTests {

        @Test
        public void shouldReturnValidChild() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            Random rnd = new Random(42);
            List<Ilayout> allChildren = layout.children();

            for (int i = 0; i < 50; i++) {
                Ilayout child = layout.randomChild(rnd, null);

                assertThat(allChildren)
                        .as("O filho gerado aleatoriamente tem de pertencer à lista de sucessores válidos")
                        .contains(child);
            }
        }

        @Test
        public void shouldNeverReturnAvoidState_whenOtherOptionsExist() {
            BallSortLayout layout = new BallSortLayout("A B E\nB A E");
            Random rnd = new Random(42);

            Ilayout avoidState = layout.children().get(0);

            for (int i = 0; i < 50; i++) {
                Ilayout child = layout.randomChild(rnd, avoidState);

                assertThat(child)
                        .as("Não deve fazer ping-pong se houver outras jogadas")
                        .isNotEqualTo(avoidState);
            }
        }

        @Test
        public void shouldReturnNull_whenAvoidStateIsTheOnlyOption() {
            BallSortLayout layout = new BallSortLayout("E B\nA A");
            BallSortLayout avoidState = new BallSortLayout("B E\nA A");

            Random rnd = new Random(42);
            Ilayout child = layout.randomChild(rnd, avoidState);

            assertThat(child)
                    .as("Deve devolver null quando a única jogada legal é voltar para o avoidState (sinalizando beco sem saída)")
                    .isNull();
        }
    }
}