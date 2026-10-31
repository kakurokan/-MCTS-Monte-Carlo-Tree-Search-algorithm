package puzzles;

import core.Ilayout;

import java.util.*;

public class BallSortLayout implements Ilayout, Cloneable {
    private final int K;
    private final int maxTubeSize;
    private List<Stack<Character>> tubes;

    public BallSortLayout(String configuration) {
        K = 1;
        tubes = new ArrayList<>();
        String[] lines = configuration.trim().split("\n");
        maxTubeSize = lines.length;

        int numTubes = lines[0].split(" ").length;

        for (int i = 0; i < numTubes; i++) {
            tubes.add(new Stack<>());
        }

        for (int i = maxTubeSize - 1; i >= 0; i--) {
            String[] line = lines[i].split(" ");
            for (int j = 0; j < numTubes; j++) {
                char ball = line[j].charAt(0);
                if (ball != 'E') {
                    tubes.get(j).push(ball);
                }
            }
        }
    }

    @Override
    public List<Ilayout> children() {
        List<Ilayout> children = new ArrayList<>();

        for (int i = 0; i < tubes.size(); i++) {
            Stack<Character> sourceTube = tubes.get(i);

            if (sourceTube.isEmpty()) {
                continue;
            }

            char ball = sourceTube.peek();

            for (int j = 0; j < this.tubes.size(); j++) {
                if (i == j) {
                    continue;
                }

                Stack<Character> currentDestTube = this.tubes.get(j);

                if (currentDestTube.size() == maxTubeSize) {
                    continue;
                }
                if (!currentDestTube.isEmpty() && currentDestTube.peek() != ball) {
                    continue;
                }

                BallSortLayout child = this.clone();
                child.tubes.get(i).pop();
                child.tubes.get(j).push(ball);
                children.add(child);
            }
        }

        return children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BallSortLayout that = (BallSortLayout) o;
        return Objects.equals(tubes, that.tubes);
    }

    @Override
    public int hashCode() {
        return tubes.hashCode();
    }

    @Override
    public boolean isGoal(Ilayout l) {
        for (Stack<Character> tube : tubes) {
            if (!isValidTube(tube)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public double getK() {
        return K;
    }

    @Override
    public double getHeuristic(Ilayout goal) {
        int total = 0;
        Map<Character, Integer> colorBlocks = new HashMap<>();
        for (Stack<Character> tube : tubes) {
            if (tube.isEmpty()) {
                continue;
            }

            char base = tube.get(0);
            total += tube.size();

            colorBlocks.merge(base, 1, Math::max);

            for (int i = 1; i < tube.size(); i++) {
                char actualBall = tube.get(i);
                if (actualBall != base) {
                    break;
                }
                colorBlocks.merge(base, i + 1, Math::max);
            }
        }

        int totalColorBlocks = 0;
        for (Integer v : colorBlocks.values()) {
            totalColorBlocks += v;
        }

        return total - totalColorBlocks;
    }

    @Override
    public BallSortLayout clone() {
        try {
            BallSortLayout clone = (BallSortLayout) super.clone();
            clone.tubes = new ArrayList<>(this.tubes.size());

            for (Stack<Character> tube : this.tubes) {
                @SuppressWarnings("unchecked")
                Stack<Character> cloneTube = (Stack<Character>) tube.clone();
                clone.tubes.add(cloneTube);
            }

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private boolean isValidTube(Stack<Character> tube) {
        if (tube.isEmpty()) {
            return true;
        }

        if (tube.size() != maxTubeSize) {
            return false;
        }

        char firstBall = tube.get(0);
        for (char ball : tube) {
            if (ball != firstBall) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = maxTubeSize - 1; i >= 0; i--) {
            for (int j = 0; j < tubes.size(); j++) {
                Stack<Character> tube = tubes.get(j);

                if (i < tube.size()) {
                    sb.append(tube.get(i));
                } else {
                    sb.append("E");
                }

                if (j < tubes.size() - 1) {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
