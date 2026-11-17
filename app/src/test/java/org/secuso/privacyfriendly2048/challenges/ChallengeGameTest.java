package org.secuso.privacyfriendly2048.challenges;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

public class ChallengeGameTest {
    @Test
    public void aTileMergesOnceAndOnlyWithinItsStoneSegment() {
        Setup setup = ChallengeCatalog.endless(Mode.OBSTACLES, 6);
        State state = new State(new int[]{
                2, 2, 4, 4,
                2, STONE, 0, 2,
                4, 4, STONE, 8,
                0, 0, 0, 0
        }, 0, 0, 6);
        State left = slide(state, setup, Direction.LEFT);
        assertArrayEquals(new int[]{
                4, 8, 0, 0,
                2, STONE, 2, 0,
                8, 0, STONE, 8,
                0, 0, 0, 0
        }, cells(left));
        assertEquals(20, left.score);

        State acrossStone = new State(new int[]{
                0, 2, 0, 0,
                0, STONE, 0, 0,
                0, 2, STONE, 0,
                0, 2, 0, 0
        }, 0, 0, 6);
        State up = slide(acrossStone, setup, Direction.UP);
        assertEquals(2, up.tile(1));
        assertEquals(STONE, up.tile(5));
        assertEquals(4, up.tile(9));
        assertEquals(0, up.tile(13));
    }

    @Test
    public void fibonacciMergesOnlyTheOpeningPairOrSequenceNeighbors() {
        assertEquals(2, merge(1, 1, Mode.FIBONACCI));
        assertEquals(3, merge(2, 1, Mode.FIBONACCI));
        assertEquals(5, merge(2, 3, Mode.FIBONACCI));
        assertEquals(8, merge(5, 3, Mode.FIBONACCI));
        assertEquals(0, merge(3, 3, Mode.FIBONACCI));
        assertEquals(0, merge(2, 5, Mode.FIBONACCI));
        assertEquals(0, merge(1, 3, Mode.FIBONACCI));
        State row = new State(new int[]{1, 1, 3, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 0, 0, 2);
        State left = slide(row, ChallengeCatalog.endless(Mode.FIBONACCI, 2), Direction.LEFT);
        assertArrayEquals(new int[]{2, 8, 0, 0}, Arrays.copyOf(cells(left), 4));
        assertEquals(10, left.score);
    }

    @Test
    public void sprintContinuesAfter256AndUndoRestoresSpawnAndMoves() {
        Setup setup = ChallengeCatalog.endless(Mode.SPRINT, 12345);
        State start = new State(new int[]{128, 128, 0, 0, 0, 0, 0, 0, 0}, 0, 0, 12345);
        ChallengeGame game = new ChallengeGame(setup, start, null);
        assertTrue(game.move(Direction.LEFT));
        int[] result = cells(game.state());
        assertEquals(256, game.state().largest());
        assertEquals(Result.PLAYING, game.result());
        assertTrue(game.undo());
        assertArrayEquals(cells(start), cells(game.state()));
        assertEquals(0, game.state().moves);
        assertFalse(game.undo());
        assertTrue(game.move(Direction.LEFT));
        assertArrayEquals(result, cells(game.state()));
    }

    @Test
    public void puzzleInvalidSwipeCostsNothingAndNoTilesSpawn() {
        Setup setup = new Setup(Mode.PUZZLE, "chain", 4, 4, Goal.TILE, 32,
                new int[]{4, 4, 8, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 0);
        ChallengeGame game = new ChallengeGame(setup);
        assertFalse(game.move(Direction.UP));
        assertEquals(0, game.state().moves);
        assertTrue(game.move(Direction.LEFT));
        assertEquals(32, Arrays.stream(cells(game.state())).sum());
        assertEquals(13, game.state().empty());

        assertTrue(game.move(Direction.LEFT));
        assertTrue(game.move(Direction.LEFT));
        assertEquals(Result.SOLVED, game.result());
        assertEquals(3, game.state().moves);
        assertFalse(game.move(Direction.RIGHT));
        assertTrue(game.undo());
        assertEquals(Result.PLAYING, game.result());
    }

    @Test
    public void stepLimitStopsAPlayableBoardAndUndoReopensIt() {
        Setup setup = new Setup(Mode.PUZZLE, "limit", 3, 1, Goal.TILE, 16,
                new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0}, 0);
        ChallengeGame game = new ChallengeGame(setup);
        assertTrue(game.move(Direction.RIGHT));
        assertEquals(Result.OUT_OF_MOVES, game.result());
        assertFalse(game.move(Direction.DOWN));
        assertTrue(game.undo());
        assertEquals(Result.PLAYING, game.result());
    }

    @Test
    public void everyAuthoredPuzzleHasAPathWithinItsMoveLimit() {
        int[] shortest = {1, 4, 4, 3, 3, 4, 5, 4, 10};
        assertEquals(shortest.length, ChallengeCatalog.puzzleCount());
        for (int i = 0; i < shortest.length; i++) {
            Setup setup = ChallengeCatalog.puzzle(i);
            assertEquals(setup.key, shortest[i], solve(setup));
        }
    }

    @Test
    public void dailyBoardsAreFixedForADateAndSolvableAcrossRotations() {
        for (long day = 20_770; day < 20_826; day++) {
            Setup today = ChallengeCatalog.daily(day);
            Setup repeated = ChallengeCatalog.daily(day);
            Setup tomorrow = ChallengeCatalog.daily(day + 1);
            assertArrayEquals(today.opening(), repeated.opening());
            assertNotEquals(today.key, tomorrow.key);
            assertFalse(Arrays.equals(today.opening(), tomorrow.opening()) && today.goal == tomorrow.goal
                    && today.target == tomorrow.target);
            assertTrue(today.key, solve(today) > 0);
        }
    }

    private static int solve(Setup setup) {
        ArrayDeque<State> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new ChallengeGame(setup).state());
        while (!queue.isEmpty()) {
            State state = queue.remove();
            if (goalMet(state, setup)) return state.moves;
            if (state.moves >= setup.limit) continue;
            for (Direction direction : Direction.values()) {
                State next = slide(state, setup, direction);
                if (next == null) continue;
                String key = Arrays.toString(cells(next)) + ":" + (setup.goal == Goal.SCORE ? next.score : 0);
                if (visited.add(key)) queue.add(next);
            }
        }
        return -1;
    }

    private static int[] cells(State state) {
        int[] result = new int[state.cellCount()];
        for (int i = 0; i < result.length; i++) result[i] = state.tile(i);
        return result;
    }
}
