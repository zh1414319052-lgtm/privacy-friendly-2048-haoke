package org.secuso.privacyfriendly2048;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.secuso.privacyfriendly2048.activities.*;
import org.secuso.privacyfriendly2048.challenges.*;
import org.secuso.privacyfriendly2048.ceramic.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

/** Physical-device instrumentation only: drives real controls and touchscreen input. */
final class CeramicProbe {
    final Instrumentation i; final Context c; final AtomicReference<Activity> top=new AtomicReference<>();
    final StringBuilder report=new StringBuilder();
    CeramicProbe(Instrumentation i){this.i=i;c=i.getTargetContext();
        ((Application)c.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityResumed(Activity a){top.set(a);}
            public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}
            public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
        });
    }
    String run() throws Exception {
        Activity home=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        settle();shot("home");click(home,R.id.challenge_modes_action);Activity menu=await(ChallengeMenuActivity.class);shot("modes");
        i.runOnMainSync(menu::finish);settle();
        int[] cards={R.id.challenge_card_sprint,R.id.challenge_card_puzzle,R.id.challenge_card_obstacles,R.id.challenge_card_fibonacci,R.id.challenge_card_daily};
        for(Mode mode:Mode.values()){
            home=top.get();click(home,R.id.challenge_modes_action);menu=await(ChallengeMenuActivity.class);click(menu,cards[mode.ordinal()]);
            if(mode==Mode.PUZZLE){Activity pick=top.get();i.runOnMainSync(()->((ViewGroup)pick.getWindow().getDecorView().findViewWithTag("puzzle-list")).getChildAt(0).performClick());settle();}
            Activity game=await(ChallengeActivity.class);
            restart(game,true);State opening=model(game).state();
            shot(mode.name().toLowerCase()+"-start");
            String original=fingerprint(opening);
            boolean moved=false;Direction used=null;
            for(Direction dir:new Direction[]{Direction.UP,Direction.LEFT,Direction.DOWN,Direction.RIGHT}){
                swipe(game,dir);if(!original.equals(fingerprint(model(game).state()))){moved=true;used=dir;break;}
            }
            require(moved,mode+" no touch gesture produced a move");
            require(model(game).state().moves==opening.moves+1,mode+" move count");
            if(mode==Mode.OBSTACLES)for(int k=0;k<opening.cellCount();k++)if(opening.tile(k)==STONE)require(model(game).state().tile(k)==STONE,"Stone moved");
            click(game,R.id.challenge_undo);require(original.equals(fingerprint(model(game).state())),mode+" undo mismatch");
            swipe(game,used);String after=fingerprint(model(game).state());
            restart(game,false);require(after.equals(fingerprint(model(game).state())),mode+" cancel reset state");
            require((Integer)field(field(game,"audio"),"stream")!=0,mode+" no audio stream started");
            shot(mode.name().toLowerCase()+"-played");
            Activity previous=game;i.runOnMainSync(previous::recreate);game=awaitNew(ChallengeActivity.class,previous);
            require(after.equals(fingerprint(model(game).state())),mode+" recreation lost state");
            i.runOnMainSync(game::finish);menu=await(ChallengeMenuActivity.class);
            if(mode==Mode.PUZZLE){Activity pick=menu;i.runOnMainSync(()->((ViewGroup)pick.getWindow().getDecorView().findViewWithTag("puzzle-list")).getChildAt(0).performClick());}
            else click(menu,cards[mode.ordinal()]);
            game=await(ChallengeActivity.class);require(after.equals(fingerprint(model(game).state())),mode+" resume lost state");
            if(mode==Mode.PUZZLE){
                require(model(game).result()==Result.SOLVED,"Puzzle 1 should solve by up");
                click(game,R.id.challenge_next_level);
                require(model(game).state().moves==0,"Next level didn't reset moves");
                require(((TextView)game.findViewById(R.id.challenge_moves)).getText().toString().equals("5"),"Next level limit incorrect");
                shot("puzzle-next");
            }
            if(mode==Mode.DAILY){restart(game,true);require(original.equals(fingerprint(model(game).state())),"Daily opening changed");}
            require(soundLoaded(game)==7,mode+" sound files failed to load");

            report.append("PASS ").append(mode).append(": touch, undo, cancel, recreation, resume, sound resources").append('\n');
            i.runOnMainSync(game::finish);menu=await(ChallengeMenuActivity.class);i.runOnMainSync(menu::finish);await(MainActivity.class);
        }
        home=top.get();click(home,R.id.button_continueGame);Activity classic=await(GameActivity.class);
        shot("classic");
        String before=classicState(classic);key(KeyEvent.KEYCODE_DPAD_LEFT);key(KeyEvent.KEYCODE_DPAD_UP);String after=classicState(classic);
        require(!before.equals(after),"Classic movement unchanged");
        View undo=classic.findViewById(R.id.undoButton);require(undo.isEnabled(),"Classic undo disabled");click(classic,R.id.undoButton);
        click(classic,R.id.restartButton);shot("restart");
        AlertDialog dialog=(AlertDialog)field(classic,"restartConfirmation");i.runOnMainSync(()->dialog.findViewById(android.R.id.button2).performClick());settle();
        require(soundLoaded(classic)==7,"Classic sound load");report.append("PASS CLASSIC: moves, undo, cancel, sounds\n");
        // App-only font stress and original design fixture without changing device settings or saved game.
        i.runOnMainSync(classic::finish);await(MainActivity.class);
        fixture();
        return report.toString();
    }
    String visual() throws Exception {
        Activity home=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        settle();fixture();click(home,R.id.button_continueGame);
        Activity classic=await(GameActivity.class);shot("classic-final");click(classic,R.id.restartButton);shot("restart-final");
        AlertDialog dialog=(AlertDialog)field(classic,"restartConfirmation");i.runOnMainSync(()->dialog.findViewById(android.R.id.button2).performClick());settle();
        i.runOnMainSync(classic::finish);await(MainActivity.class);
        Activity puzzle=i.startActivitySync(new Intent(c,ChallengeActivity.class).putExtra("mode",Mode.PUZZLE.ordinal()).putExtra("level",0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        settle();restart(puzzle,true);swipe(puzzle,Direction.UP);
        require(model(puzzle).result()==Result.SOLVED,"Visual puzzle solve");
        View advance=puzzle.findViewById(R.id.challenge_next_level);
        android.graphics.Rect rect=new android.graphics.Rect();i.runOnMainSync(()->advance.getGlobalVisibleRect(rect));
        require(rect.height()==advance.getHeight(),"Next-level button clipped");
        shot("puzzle-final");click(puzzle,R.id.challenge_next_level);
        require(model(puzzle).state().moves==0,"Next level navigation");
        i.runOnMainSync(puzzle::finish);
        return report+"PASS final native numbers, confirmation cancel, full next-level button and navigation\n";
    }
    String audio() throws Exception {
        Activity game=i.startActivitySync(new Intent(c,ChallengeActivity.class).putExtra("mode",Mode.OBSTACLES.ordinal()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        settle();restart(game,true);
        CeramicAudio audio=(CeramicAudio)field(game,"audio");
        android.content.SharedPreferences prefs=android.preference.PreferenceManager.getDefaultSharedPreferences(c);
        boolean enabled=prefs.getBoolean("ceramic_sound",true);
        try{
            prefs.edit().putBoolean("ceramic_sound",true).commit();
            for(CeramicAudio.Cue cue:CeramicAudio.Cue.values()){
                i.runOnMainSync(()->audio.play(cue));settle();
                require((Integer)field(audio,"stream")!=0,"Failed sound "+cue);
                report.append("PASS audio stream ").append(cue).append('\n');
            }
            int stream=(Integer)field(audio,"stream");
            prefs.edit().putBoolean("ceramic_sound",false).commit();
            i.runOnMainSync(()->audio.play(CeramicAudio.Cue.MERGE));settle();
            require(stream==(Integer)field(audio,"stream"),"Mute started a stream");
            prefs.edit().putBoolean("ceramic_sound",true).commit();
            i.runOnMainSync(audio::pause);i.runOnMainSync(()->audio.play(CeramicAudio.Cue.MERGE));settle();
            require(stream==(Integer)field(audio,"stream"),"Paused audio started a stream");
            i.runOnMainSync(audio::resume);i.runOnMainSync(()->audio.play(CeramicAudio.Cue.MERGE));settle();
            require(stream!=(Integer)field(audio,"stream"),"Resumed audio did not play");
            swipe(game,Direction.LEFT);swipe(game,Direction.DOWN);shot("obstacles-audio-final");
            report.append("PASS mute, pause/resume, obstacle input after audio checks\n");
        }finally{prefs.edit().putBoolean("ceramic_sound",enabled).commit();i.runOnMainSync(game::finish);}
        return report.toString();
    }
    void fixture()throws Exception{
        Activity home=top.get();i.runOnMainSync(()->{
            CeramicScene scene=(CeramicScene)home.getWindow().getDecorView().findViewWithTag("home-scene");
            // The scene is available through its activity field; no fixture data is written to stores.
            try{scene=(CeramicScene)field(home,"scene");}catch(Exception e){throw new RuntimeException(e);}
            scene.text("current-score-value","512");scene.text("best-score-value","2048");
            scene.text("home-heading",CeramicAssets.localized(home,"继续上次的挑战"));
            ((ChallengeBoard)scene.node("home-board")).renderValues(new int[]{2,4,8,0,4,16,32,0,2,8,64,0,0,2,0,0},false);
            int[] at=new int[2];scene.getLocationOnScreen(at);
            report.append("DESIGN_MAPPING nativeScene=").append(at[0]).append(',').append(at[1]).append(" size=").append(scene.getWidth()).append('x').append(scene.getHeight()).append(" fontScale=").append(home.getResources().getConfiguration().fontScale).append('\n');
        });settle();shot("home-design-fixture");
        report.append("PASS design fixture: real native widgets, no saved-state mutation\n");
    }
    int soundLoaded(Activity a)throws Exception{Object audio=field(a,"audio");return ((Set<?>)field(audio,"loaded")).size();}
    Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    ChallengeGame model(Activity a)throws Exception{return (ChallengeGame)field(a,"game");}
    String classicState(Activity a)throws Exception{Object state=field(a,"gameState");return state.toString();}
    String fingerprint(State s){StringBuilder b=new StringBuilder(s.score+":"+s.moves+":");for(int k=0;k<s.cellCount();k++)b.append(s.tile(k)).append(',');return b.toString();}
    void restart(Activity a,boolean confirm)throws Exception{
        click(a,R.id.challenge_restart_button);AlertDialog dialog=(AlertDialog)field(a,"restartDialog");
        i.runOnMainSync(()->dialog.findViewById(confirm?android.R.id.button1:android.R.id.button2).performClick());settle();
    }
    void click(Activity a,int id){i.runOnMainSync(()->{View v=a.findViewById(id);require(v!=null&&v.isEnabled(),"Missing/disabled control "+id);v.performClick();});settle();}
    void key(int code){i.sendKeyDownUpSync(code);settle();}
    void swipe(Activity a,Direction d){
        int[] pos=new int[2];int[] side=new int[1];
        i.runOnMainSync(()->{View v=a.findViewById(R.id.challenge_board);v.getLocationOnScreen(pos);side[0]=v.getWidth();});
        float x=pos[0]+side[0]*.5f,y=pos[1]+side[0]*.5f;
        float dx=d==Direction.LEFT?-side[0]*.30f:d==Direction.RIGHT?side[0]*.30f:0;
        float dy=d==Direction.UP?-side[0]*.30f:d==Direction.DOWN?side[0]*.30f:0;
        long start=SystemClock.uptimeMillis();
        for(int step=0;step<=8;step++){int action=step==0?MotionEvent.ACTION_DOWN:step==8?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x+dx*step/8,y+dy*step/8,0);i.sendPointerSync(e);e.recycle();SystemClock.sleep(18);}
        settle();
    }
    Activity await(Class<?> cls){for(int n=0;n<40;n++){Activity a=top.get();if(cls.isInstance(a)){settle();return a;}SystemClock.sleep(100);}throw new AssertionError("Expected "+cls+" got "+top.get());}
    Activity awaitNew(Class<?> cls,Activity old){for(int n=0;n<50;n++){Activity a=top.get();if(cls.isInstance(a)&&a!=old){settle();return a;}SystemClock.sleep(100);}throw new AssertionError("No recreated activity");}
    void settle(){i.waitForIdleSync();SystemClock.sleep(260);i.waitForIdleSync();}
    void shot(String name)throws Exception{Bitmap b=i.getUiAutomation().takeScreenshot();require(b!=null,"No screenshot");try(FileOutputStream o=new FileOutputStream(new File(c.getCacheDir(),"ceramic-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();}
    void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
