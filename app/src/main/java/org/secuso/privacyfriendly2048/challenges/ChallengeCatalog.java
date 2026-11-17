package org.secuso.privacyfriendly2048.challenges;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

public final class ChallengeCatalog {
    private static final long DAY_MILLIS = 86_400_000L;

    private static final Setup[] PUZZLES = {
            puzzle(1, 4, 2, Goal.TILE, 16,
                    2, 0, 2, 4,  4, 4, 0, 8,  0, 0, 0, 8,  0, 0, 0, 0),
            puzzle(2, 3, 5, Goal.TILE, 32,
                    4, 0, 2,  16, 4, 0,  0, 8, 2),
            puzzle(3, 3, 5, Goal.SPACE, 5,
                    2, 8, 2,  2, 4, 8,  4, 4, 2),
            puzzle(4, 4, 4, Goal.SCORE, 96,
                    8, 0, 8, 16,  16, 2, 2, 16,  4, 4, 0, 8,  0, 8, 8, 0),
            puzzle(5, 4, 4, Goal.TILE, 128,
                    2, 64, 8, 4,  4, 16, 16, 2,  8, 0, 32, 8,  0, 0, 0, 4),
            puzzle(6, 4, 6, Goal.SPACE, 5,
                    2, 4, 16, 2,  4, 2, 8, 4,  16, 8, 8, 16,  2, 16, 32, 2),
            puzzle(7, 4, 6, Goal.TILE, 64,
                    4, STONE, 4, 8,  8, 8, 16, STONE,  4, 16, 2, 8,  4, 0, 0, 4),
            puzzle(8, 5, 5, Goal.SCORE, 224,
                    16, 8, 8, 16, 4,  4, 16, 0, 4, 4,  32, 16, 8, 8, 16,
                    4, 2, 16, 2, 2,  4, 4, 0, 32, 4),
            puzzle(9, 4, 11, Goal.TILE, 256,
                    64, 16, 4, 8,  32, 0, 32, 2,  8, 8, 2, 8,  4, 4, 4, 64)
    };

    private ChallengeCatalog() {}

    public static int puzzleCount() { return PUZZLES.length; }
    public static Setup puzzle(int index) { return PUZZLES[index]; }

    private static Setup puzzle(int id, int size, int limit, Goal goal, int target, int... layout) {
        if (layout.length != size * size) throw new IllegalArgumentException("Invalid board");
        return new Setup(Mode.PUZZLE, "puzzle_" + id, size, limit, goal, target, layout, 0);
    }

    public static Setup endless(Mode mode, long seed) {
        switch (mode) {
            case SPRINT: return new Setup(mode, "sprint", 3, 0, Goal.TILE, 256, new int[9], seed);
            case OBSTACLES:
                int[] board = new int[16];
                board[5] = STONE;
                board[10] = STONE;
                return new Setup(mode, "obstacles", 4, 0, Goal.TILE, 1024, board, seed);
            case FIBONACCI:
                return new Setup(mode, "fibonacci", 4, 0, Goal.TILE, 2584, new int[16], seed);
            default: throw new IllegalArgumentException("Not an open-ended mode");
        }
    }

    public static long localDay() {
        Calendar now = Calendar.getInstance();
        Calendar date = utcCalendar();
        date.clear();
        date.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        return date.getTimeInMillis() / DAY_MILLIS;
    }

    public static long dateMillis(long day) { return day * DAY_MILLIS; }
    public static TimeZone utc() { return TimeZone.getTimeZone("UTC"); }

    private static Calendar utcCalendar() {
        return new GregorianCalendar(utc());
    }

    public static Setup daily(long day) {
        // Alternate between seven vetted puzzles and rotate or reflect their boards.
        int count = PUZZLES.length - 2;
        int index = (int) ((day % count + count) % count);
        int symmetry = (int) (ChallengeGame.mix((day - index) / count) & 7L);
        Setup base = PUZZLES[2 + index];
        int n = base.size;
        int[] original = base.opening();
        int[] opening = new int[original.length];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int y = row;
                int x = (symmetry & 4) == 0 ? col : n - 1 - col;
                for (int r = 0; r < (symmetry & 3); r++) {
                    int oldY = y;
                    y = x;
                    x = n - 1 - oldY;
                }
                opening[y * n + x] = original[row * n + col];
            }
        }
        return new Setup(Mode.DAILY, "daily_" + day, n, base.limit, base.goal, base.target, opening, 0);
    }
}
