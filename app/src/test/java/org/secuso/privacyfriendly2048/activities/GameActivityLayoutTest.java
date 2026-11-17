package org.secuso.privacyfriendly2048.activities;

import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.secuso.privacyfriendly2048.R;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class)
public class GameActivityLayoutTest {
    @Test public void classicTilesHaveMeasuredBodiesAfterFirstLayoutAndRecreation() {
        for(int size=4;size<=7;size++) {
            Intent intent=new Intent(RuntimeEnvironment.getApplication(),GameActivity.class).putExtra("n",size);
            try(ActivityController<GameActivity> controller=Robolectric.buildActivity(GameActivity.class,intent).setup().visible()) {
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                check(controller.get(),size);
                controller.recreate();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                check(controller.get(),size);
            }
        }
    }
    private static void check(GameActivity activity,int size) {
        RelativeLayout board=activity.findViewById(R.id.number_field);
        assertEquals(size*size,board.getChildCount());
        for(int k=0;k<board.getChildCount();k++) {
            View tile=board.getChildAt(k);
            assertTrue("Tile must have width at size "+size,tile.getWidth()>0);
            assertTrue("Tile must have height at size "+size,tile.getHeight()>0);
            assertTrue(tile.getLeft()>=0 && tile.getTop()>=0);
            assertTrue(tile.getRight()<=board.getWidth() && tile.getBottom()<=board.getHeight());
        }
    }
}
