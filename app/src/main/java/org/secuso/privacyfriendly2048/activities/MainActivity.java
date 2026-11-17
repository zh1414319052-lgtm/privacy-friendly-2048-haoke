package org.secuso.privacyfriendly2048.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.activities.helper.*;
import org.secuso.privacyfriendly2048.ceramic.*;
import org.secuso.privacyfriendly2048.challenges.ChallengeBoard;
import java.io.*;

public class MainActivity extends LocalizedActivity {
    private SharedPreferences prefs;
    private CeramicScene scene;
    private CeramicAudio audio;
    private AlertDialog newGameConfirmation;
    private int size;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("myPref",MODE_PRIVATE);audio=new CeramicAudio(this);
    }
    @Override protected void onResume(){super.onResume();audio.resume();render();}
    @Override protected void onPause(){audio.pause();super.onPause();}
    @Override protected void onDestroy(){if(newGameConfirmation!=null)newGameConfirmation.dismiss();audio.release();super.onDestroy();}
    private Object read(String filename){try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(new File(getFilesDir(),filename)))){return in.readObject();}catch(Exception e){return null;}}
    private void render(){
        size=Math.max(4,Math.min(7,prefs.getInt("currentPage",0)+4));
        Object saved=read(GameActivity.saveFileName(size));GameState state=saved instanceof GameState?(GameState)saved:null;
        if(state!=null&&(state.n!=size||state.numbers==null||state.numbers.length!=size*size))state=null;
        Object stats=read("statistics"+size+".txt");long best=stats instanceof GameStatistics?((GameStatistics)stats).getRecord():0;
        scene=new CeramicScene(this,"home");
        ChallengeBoard preview=new ChallengeBoard(this,size,"2".equals(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_color","1")),false,null);
        preview.setId(View.NO_ID);preview.setFocusable(false);preview.setClickable(false);
        preview.renderValues(state==null?new int[size*size]:state.numbers,false);
        scene.replace("home-board",preview);scene.move("home-board",24,315,384,384);
        scene.text("current-score-value",state==null?"0":Integer.toString(state.points));scene.text("best-score-value",Long.toString(best));
        scene.text("board-type",CeramicAssets.words(this,"经典","Classic")+" · "+size+" × "+size);
        scene.text("save-status",CeramicAssets.words(this,state==null?"等待开始":"已自动保存",state==null?"Ready to play":"Autosaved"));
        if(state==null){scene.text("home-heading",CeramicAssets.words(this,"开始新的挑战","Your next challenge"));scene.text("continue-label",getString(R.string.new_Game_Button));}
        final boolean resume=state!=null;
        scene.action("continue",R.id.button_continueGame,scene.text("continue-label").getText().toString(),()->{audio.play(CeramicAudio.Cue.SELECT);openGame(size,!resume);});
        scene.action("start-new",R.id.button_newGame,getString(R.string.new_Game_Button),this::chooseSize);
        scene.action("board-type",View.NO_ID,CeramicAssets.words(this,"选择棋盘大小","Choose board size"),this::chooseSize);
        scene.action("open-modes",R.id.challenge_modes_action,getString(R.string.action_modes),()->{audio.play(CeramicAudio.Cue.SELECT);startActivity(new Intent(this,ChallengeMenuActivity.class));});
        scene.action("app-mark",View.NO_ID,CeramicAssets.words(this,"设置与统计","Settings and statistics"),()->CeramicNavigation.menu(this,scene.node("app-mark")));
        CeramicScene.install(this,scene);
    }
    private void chooseSize(){
        audio.play(CeramicAudio.Cue.SELECT);
        new AlertDialog.Builder(this).setTitle(CeramicAssets.words(this,"选择棋盘","Choose board"))
            .setItems(new String[]{"4 × 4","5 × 5","6 × 6","7 × 7"},(d,which)->{
                int chosen=which+4;prefs.edit().putInt("currentPage",which).apply();
                if(new File(getFilesDir(),GameActivity.saveFileName(chosen)).isFile())
                    newGameConfirmation=GameConfirmation.show(this,R.string.confirm_replace_title,getString(R.string.confirm_replace_message,chosen),R.string.replace_game,()->openGame(chosen,true));
                else openGame(chosen,true);
            }).setNegativeButton(R.string.cancel,null).show();
    }
    private void openGame(int n,boolean fresh){startActivityForResult(new Intent(this,GameActivity.class).putExtra("n",n).putExtra("new",fresh),1);}
}
