package org.secuso.privacyfriendly2048.activities;

import android.app.Application;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.challenges.ChallengeGame.Mode;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class ChallengeActivityTest {
    @Test
    public void touchSolvesPuzzleAndNextBoardKeepsItsOwnStateAfterRecreation() {
        Intent intent = ChallengeActivity.intent(RuntimeEnvironment.getApplication(), Mode.PUZZLE, 0, 0);
        try (ActivityController<ChallengeActivity> activity = Robolectric.buildActivity(ChallengeActivity.class, intent).setup()) {
            ChallengeActivity screen = activity.get();
            View board = screen.findViewById(R.id.challenge_board);
            long now = SystemClock.uptimeMillis();
            send(board, now, 0, MotionEvent.ACTION_DOWN, 100, 180);
            send(board, now, 80, MotionEvent.ACTION_MOVE, 100, 100);
            send(board, now, 140, MotionEvent.ACTION_UP, 100, 50);
            assertEquals(View.VISIBLE, screen.findViewById(R.id.challenge_status).getVisibility());
            assertEquals("1", value(screen, R.id.challenge_record));
            screen.findViewById(R.id.challenge_next_level).performClick();
            assertEquals("5", value(screen, R.id.challenge_moves));
            activity.recreate();
            assertEquals("5", value(activity.get(), R.id.challenge_moves));
            assertEquals(View.GONE, activity.get().findViewById(R.id.challenge_status).getVisibility());
        }
    }

    private static void send(View view, long start, long offset, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(start, start + offset, action, x, y, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }

    private static String value(ChallengeActivity screen, int id) {
        return ((TextView) screen.findViewById(id)).getText().toString();
    }
}
