package org.secuso.privacyfriendly2048.challenges;

import java.util.Arrays;

/** Rules for the additional modes, independent of Android views and storage. */
public final class ChallengeGame {
    public static final int STONE = -1;
    private static final int[] FIBONACCI = fibonacci();

    public enum Mode { SPRINT, PUZZLE, OBSTACLES, FIBONACCI, DAILY }
    public enum Direction { LEFT, RIGHT, UP, DOWN }
    public enum Goal { TILE, SPACE, SCORE }
    public enum Result { PLAYING, SOLVED, STUCK, OUT_OF_MOVES }

    public static final class Setup {
        public final Mode mode;
        public final String key;
        public final int size;
        public final int limit;
        public final Goal goal;
        public final int target;
        private final int[] opening;
        private final long seed;

        Setup(Mode mode, String key, int size, int limit, Goal goal, int target,
              int[] opening, long seed) {
            this.mode = mode;
            this.key = key;
            this.size = size;
            this.limit = limit;
            this.goal = goal;
            this.target = target;
            this.opening = opening.clone();
            this.seed = seed;
        }

        public boolean isPuzzle() { return limit > 0; }
        int[] opening() { return opening.clone(); }
        boolean validTile(int index, int tile) {
            if (opening[index] == STONE) return tile == STONE;
            if (tile < 0) return false;
            if (tile == 0) return true;
            return mode == Mode.FIBONACCI ? Arrays.binarySearch(FIBONACCI, tile) >= 0
                    : tile >= 2 && (tile & (tile - 1)) == 0;
        }
    }

    public static final class State {
        private final int[] cells;
        public final long score;
        public final int moves;
        final long random;

        State(int[] cells, long score, int moves, long random) {
            this.cells = cells;
            this.score = score;
            this.moves = moves;
            this.random = random;
        }

        public int tile(int index) { return cells[index]; }
        public int cellCount() { return cells.length; }
        public int largest() {
            int max = 0;
            for (int cell : cells) max = Math.max(max, cell);
            return max;
        }
        public int empty() {
            int count = 0;
            for (int cell : cells) if (cell == 0) count++;
            return count;
        }
    }

    private final Setup setup;
    private State state;
    private State previous;
    private Result result;

    public ChallengeGame(Setup setup) {
        this.setup = setup;
        restart();
    }

    ChallengeGame(Setup setup, State state, State previous) {
        this.setup = setup;
        this.state = state;
        this.previous = previous;
        result = evaluate();
    }

    public State state() { return state; }
    State previous() { return previous; }
    public Result result() { return result; }
    public boolean canUndo() { return previous != null; }

    public void restart() {
        long seed = state == null || setup.isPuzzle() ? setup.seed : mix(state.random);
        state = new State(setup.opening(), 0, 0, seed);
        if (!setup.isPuzzle()) {
            state = spawn(state);
            state = spawn(state);
        }
        previous = null;
        result = evaluate();
    }

    public boolean move(Direction direction) {
        if (result != Result.PLAYING) return false;
        State shifted = slide(state, setup, direction);
        if (shifted == null) return false;
        previous = state;
        state = setup.isPuzzle() ? shifted : spawn(shifted);
        result = evaluate();
        return true;
    }

    public boolean undo() {
        if (previous == null) return false;
        state = previous;
        previous = null;
        result = evaluate();
        return true;
    }

    private Result evaluate() {
        if (setup.isPuzzle()) {
            if (goalMet(state, setup)) return Result.SOLVED;
            if (state.moves >= setup.limit) return Result.OUT_OF_MOVES;
        }
        for (Direction direction : Direction.values()) {
            if (slide(state, setup, direction) != null) return Result.PLAYING;
        }
        return Result.STUCK;
    }

    static boolean goalMet(State state, Setup setup) {
        switch (setup.goal) {
            case TILE: return state.largest() >= setup.target;
            case SPACE: return state.empty() >= setup.target;
            case SCORE: return state.score >= setup.target;
            default: throw new AssertionError(setup.goal);
        }
    }

    static State slide(State state, Setup setup, Direction direction) {
        int n = setup.size;
        int[] board = state.cells.clone();
        int[] values = new int[n];
        long earned = 0;
        for (int line = 0; line < n; line++) {
            int start = 0;
            for (int end = 0; end <= n; end++) {
                if (end < n && state.tile(index(n, direction, line, end)) != STONE) continue;
                int count = 0;
                for (int offset = start; offset < end; offset++) {
                    int tile = state.tile(index(n, direction, line, offset));
                    if (tile != 0) values[count++] = tile;
                }
                int target = start;
                for (int i = 0; i < count; i++) {
                    int tile = values[i];
                    int pair = i + 1 < count ? merge(tile, values[i + 1], setup.mode) : 0;
                    if (pair != 0) {
                        tile = pair;
                        earned += pair;
                        i++;
                    }
                    board[index(n, direction, line, target++)] = tile;
                }
                while (target < end) board[index(n, direction, line, target++)] = 0;
                start = end + 1;
            }
        }
        if (Arrays.equals(board, state.cells)) return null;
        return new State(board, state.score + earned, state.moves + 1, state.random);
    }

    private static int index(int n, Direction direction, int line, int offset) {
        switch (direction) {
            case LEFT: return line * n + offset;
            case RIGHT: return line * n + n - 1 - offset;
            case UP: return offset * n + line;
            case DOWN: return (n - 1 - offset) * n + line;
            default: throw new AssertionError(direction);
        }
    }

    static int merge(int a, int b, Mode mode) {
        if (a <= 0 || b <= 0 || a > Integer.MAX_VALUE - b) return 0;
        if (mode != Mode.FIBONACCI) return a == b ? a + b : 0;
        if (a == 1 && b == 1) return 2;
        int first = Arrays.binarySearch(FIBONACCI, a);
        int second = Arrays.binarySearch(FIBONACCI, b);
        return first >= 0 && second >= 0 && Math.abs(first - second) == 1 ? a + b : 0;
    }

    private State spawn(State current) {
        int empty = current.empty();
        if (empty == 0) return current;
        long next = current.random + 0x9e3779b97f4a7c15L;
        long roll = mix(next);
        int pick = (int) ((roll >>> 1) % empty);
        int[] board = current.cells.clone();
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 0 && pick-- == 0) {
                int base = setup.mode == Mode.FIBONACCI ? 1 : 2;
                board[i] = ((roll >>> 33) % 10 == 0) ? base * 2 : base;
                break;
            }
        }
        return new State(board, current.score, current.moves, next);
    }

    static long mix(long value) {
        long z = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int[] fibonacci() {
        int[] buffer = new int[46];
        buffer[0] = 1;
        buffer[1] = 2;
        int count = 2;
        while (count < buffer.length && buffer[count - 1] <= Integer.MAX_VALUE - buffer[count - 2]) {
            buffer[count] = buffer[count - 1] + buffer[count - 2];
            count++;
        }
        return Arrays.copyOf(buffer, count);
    }
}
