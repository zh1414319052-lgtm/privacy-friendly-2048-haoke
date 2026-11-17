package org.secuso.privacyfriendly2048.challenges;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class ChallengeStoreTest {
    @Test
    public void resumeRestoresUndoAndKeepsPuzzleAndDailyRecordsIndependent() {
        ChallengeStore store = new ChallengeStore(RuntimeEnvironment.getApplication());
        Setup first = ChallengeCatalog.puzzle(0);
        Setup second = ChallengeCatalog.puzzle(1);
        ChallengeGame game = store.load(first);
        assertTrue(game.move(Direction.UP));
        assertEquals(Result.SOLVED, game.result());
        assertTrue(store.save(first, game));
        assertEquals(1, store.record(first));
        assertEquals(0, store.record(second));
        assertEquals(0, store.record(ChallengeCatalog.daily(20_770)));

        ChallengeGame resumed = new ChallengeStore(RuntimeEnvironment.getApplication()).load(first);
        assertEquals(Result.SOLVED, resumed.result());
        assertTrue(resumed.undo());
        assertEquals(0, resumed.state().moves);
        assertTrue(store.save(first, resumed));
        assertEquals(1, store.record(first));
        assertFalse(store.load(first).canUndo());
    }

    @Test
    public void openingBoardSurvivesRecreationAndKeepsTheSameRandomSequence() {
        ChallengeStore store = new ChallengeStore(RuntimeEnvironment.getApplication());
        Setup setup = ChallengeCatalog.endless(Mode.SPRINT, 123);
        ChallengeGame game = store.load(setup);
        assertTrue(store.save(setup, game));
        Setup anotherSeed = ChallengeCatalog.endless(Mode.SPRINT, 987);
        ChallengeGame loaded = store.load(anotherSeed);
        for (int i = 0; i < game.state().cellCount(); i++) assertEquals(game.state().tile(i), loaded.state().tile(i));
        for (Direction direction : Direction.values()) {
            if (!game.move(direction)) continue;
            assertTrue(loaded.move(direction));
            for (int i = 0; i < game.state().cellCount(); i++) assertEquals(game.state().tile(i), loaded.state().tile(i));
            break;
        }
    }
}
