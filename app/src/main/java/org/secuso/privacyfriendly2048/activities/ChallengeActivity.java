package org.secuso.privacyfriendly2048.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.ceramic.*;
import org.secuso.privacyfriendly2048.activities.helper.GameConfirmation;
import org.secuso.privacyfriendly2048.activities.helper.LocalizedActivity;
import org.secuso.privacyfriendly2048.challenges.ChallengeBoard;
import org.secuso.privacyfriendly2048.challenges.ChallengeCatalog;
import org.secuso.privacyfriendly2048.challenges.ChallengeGame;
import org.secuso.privacyfriendly2048.challenges.ChallengeStore;
import org.secuso.privacyfriendly2048.helpers.AppLanguage;

import java.text.DateFormat;
import java.util.Date;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeUi.*;

public final class ChallengeActivity extends LocalizedActivity {
    private static final String MODE = "mode";
    private static final String LEVEL = "level";
    private static final String DAY = "day";

    private Mode mode;
    private int level;
    private long day;
    private Setup setup;
    private ChallengeGame game;
    private ChallengeStore store;
    private ChallengeBoard board;
    private TextView headline;
    private TextView status;
    private TextView score;
    private TextView record;
    private TextView moves;
    private View undo;
    private View next;
    private CeramicScene scene;
    private CeramicAudio audio;
    private AlertDialog restartDialog;

    static Intent intent(Context context, Mode mode, int level, long day) {
        return new Intent(context, ChallengeActivity.class).putExtra(MODE, mode.ordinal())
                .putExtra(LEVEL, level).putExtra(DAY, day);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Intent intent = getIntent();
        int selected = intent.getIntExtra(MODE, 0);
        mode = Mode.values()[Math.max(0, Math.min(Mode.values().length - 1, selected))];
        level = saved == null ? intent.getIntExtra(LEVEL, 0) : saved.getInt(LEVEL);
        level = Math.max(0, Math.min(ChallengeCatalog.puzzleCount() - 1, level));
        day = intent.getLongExtra(DAY, ChallengeCatalog.localDay());
        store = new ChallengeStore(this);
        audio=new CeramicAudio(this);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadBoard();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putInt(LEVEL, level);
        super.onSaveInstanceState(out);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (restartDialog != null) restartDialog.dismiss();
        if(audio!=null)audio.release();
        super.onDestroy();
    }

    private void loadBoard() {
        setup = mode == Mode.PUZZLE ? ChallengeCatalog.puzzle(level)
                : mode == Mode.DAILY ? ChallengeCatalog.daily(day)
                : ChallengeCatalog.endless(mode, System.nanoTime());
        game = store.load(setup);
        setTitle(mode == Mode.PUZZLE ? getString(R.string.challenge_level, level + 1)
                : getString(title(mode)));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("switch_preference_1", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        scene=new CeramicScene(this,"game");
        scene.text("game-title",getTitle());scene.text("game-subtitle",setup.size+" × "+setup.size+" · "+(mode==Mode.DAILY?new java.text.SimpleDateFormat("yyyy.MM.dd",java.util.Locale.ROOT).format(new Date(ChallengeCatalog.dateMillis(day))):getString(title(mode))));
        scene.action("game-back",View.NO_ID,getString(R.string.cancel),this::finish);
        scene.action("game-menu",View.NO_ID,CeramicAssets.words(this,"更多","More"),()->CeramicNavigation.menu(this,scene.node("game-menu")));
        headline=scene.text("game-hint");
        board=new ChallengeBoard(this,setup.size,"2".equals(prefs.getString("pref_color","1")),prefs.getBoolean("pref_animationActivated",true),this::move);
        scene.replace("game-board",board);scene.move("game-board",24,298,384,384);
        score=scene.text("game-score-value");score.setId(R.id.challenge_score);
        record=scene.text("game-best-value");record.setId(R.id.challenge_record);
        scene.text("game-best-label",getString(setup.isPuzzle()?R.string.challenge_record_moves_label:R.string.challenge_record_label));
        LinearLayout attempts=new LinearLayout(this);attempts.setGravity(Gravity.CENTER);
        TextView label=text(this,getString(setup.isPuzzle()?R.string.challenge_remaining_label:R.string.challenge_attempts_label)+"  ",13,R.color.challenge_muted,false);label.setTextColor(CeramicAssets.foreground(this,CeramicAssets.MUTED));attempts.addView(label);
        moves=text(this,"",15,R.color.challenge_title,true);moves.setTextColor(CeramicAssets.foreground(this,CeramicAssets.BLUE));moves.setId(R.id.challenge_moves);attempts.addView(moves);
        scene.addNative("attempts",attempts,24,700,384,27);
        status=text(this,"",14,R.color.challenge_title,true);status.setTextColor(CeramicAssets.foreground(this,CeramicAssets.BLUE));status.setGravity(Gravity.CENTER);status.setId(R.id.challenge_status);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        scene.addNative("result",status,24,810,384,42);
        undo=scene.node("undo");
        scene.action("undo",R.id.challenge_undo,getString(R.string.undo),()->{if(game.undo()){audio.play(CeramicAudio.Cue.UNDO);persist();render(true);}});
        scene.action("restart",R.id.challenge_restart_button,getString(R.string.challenge_restart),this::confirmRestart);
        if(mode==Mode.PUZZLE){
            AppCompatButton advance=button(this,R.id.challenge_next_level,level+1<ChallengeCatalog.puzzleCount()?R.string.challenge_next:R.string.challenge_list);
            androidx.core.view.ViewCompat.setBackgroundTintList(advance,null);advance.setBackground(CeramicAssets.skin(this,"primary"));advance.setTextColor(android.graphics.Color.WHITE);advance.setSoundEffectsEnabled(false);
            scene.addNative("advance",advance,24,858,384,52);next=advance;
            next.setOnClickListener(v->{audio.play(CeramicAudio.Cue.SELECT);if(level+1>=ChallengeCatalog.puzzleCount())finish();else{level++;loadBoard();}});
        }else next=null;
        scene.text("game-tip",setup.isPuzzle()?getString(R.string.challenge_no_spawn):mode==Mode.FIBONACCI?getString(description(mode)):CeramicAssets.localized(this,"不必着急，每一步都可以想一想。"));
        scene.landscapeGame();CeramicScene.install(this,scene);
        persist();
        render(false);
    }

    private TextView stat(LinearLayout container, int valueId, int label) {
        LinearLayout group = stack(this);
        group.setGravity(Gravity.CENTER);
        pad(group, 4, 6);
        group.setBackground(shape(this, R.color.challenge_surface, R.color.challenge_edge, 8));
        TextView caption = text(this, getString(label), 12, R.color.challenge_muted, false);
        caption.setGravity(Gravity.CENTER);
        add(group, caption, 0);
        TextView value = text(this, "", 19, R.color.challenge_title, true);
        value.setId(valueId);
        value.setGravity(Gravity.CENTER);
        add(group, value, 1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, WRAP, 1);
        if (container.getChildCount() != 0) params.leftMargin = dp(this, 6);
        container.addView(group, params);
        return value;
    }

    @Override protected void onResume(){super.onResume();if(audio!=null)audio.resume();}
    @Override protected void onPause(){if(audio!=null)audio.pause();super.onPause();}
    private void move(Direction direction) {
        long before=game.state().score;int priorLargest=game.state().largest();
        boolean contact=mode==Mode.OBSTACLES&&game.result()==Result.PLAYING&&contactsStone(game.state(),direction);
        if (game.move(direction)) {
            Result result=game.result();
            audio.play(result==Result.SOLVED?CeramicAudio.Cue.CLEAR:
                result==Result.OUT_OF_MOVES||result==Result.STUCK?CeramicAudio.Cue.END:
                game.state().largest()>=setup.target&&priorLargest<setup.target?CeramicAudio.Cue.CLEAR:
                game.state().score>before?CeramicAudio.Cue.MERGE:contact?CeramicAudio.Cue.STONE:CeramicAudio.Cue.SLIDE);
            persist();
            render(true);
        } else if(contact) audio.play(CeramicAudio.Cue.STONE);
    }
    private boolean contactsStone(State state,Direction direction){
        int dr=direction==Direction.UP?-1:direction==Direction.DOWN?1:0;
        int dc=direction==Direction.LEFT?-1:direction==Direction.RIGHT?1:0;
        for(int k=0;k<state.cellCount();k++){
            if(state.tile(k)<=0)continue;
            int row=k/setup.size+dr,col=k%setup.size+dc;
            while(row>=0&&row<setup.size&&col>=0&&col<setup.size){
                int v=state.tile(row*setup.size+col);if(v==STONE)return true;if(v>0)break;
                row+=dr;col+=dc;
            }
        }
        return false;
    }

    private void confirmRestart() {
        if (restartDialog != null && restartDialog.isShowing()) return;
        audio.play(CeramicAudio.Cue.SELECT);
        restartDialog = GameConfirmation.show(this, R.string.challenge_restart_question,
                getString(R.string.challenge_restart_detail), R.string.challenge_restart, () -> {
                    audio.play(CeramicAudio.Cue.SELECT);
                    game.restart();
                    persist();
                    render(false);
                });
    }

    private void persist() {
        if (!store.save(setup, game)) {
            Toast.makeText(this, R.string.challenge_save_error, Toast.LENGTH_LONG).show();
        }
    }

    private void render(boolean moved) {
        State state = game.state();
        int largest = state.largest();
        int target = mode == Mode.SPRINT && largest >= 256 ? 512 : setup.target;
        headline.setText(goal(this, setup, target));
        board.render(state, moved);
        score.setText(Long.toString(state.score));
        moves.setText(Integer.toString(setup.isPuzzle() ? setup.limit - state.moves : state.moves));
        long best = store.record(setup);
        record.setText(best == 0 && setup.isPuzzle() ? "—" : Long.toString(best));

        Result result = game.result();
        String notice = "";
        switch (result) {
            case SOLVED: notice = getString(R.string.challenge_solved, state.moves); break;
            case OUT_OF_MOVES: notice = getString(R.string.challenge_out_of_moves); break;
            case STUCK: notice = getString(R.string.challenge_stuck); break;
            case PLAYING:
                if (mode == Mode.SPRINT && largest >= 256 && largest < 512) {
                    notice = getString(R.string.challenge_sprint_half);
                } else if (!setup.isPuzzle() && largest >= target) {
                    notice = getString(R.string.challenge_reached, target);
                }
                break;
        }
        status.setText(notice);
        status.setVisibility(notice.isEmpty() ? View.GONE : View.VISIBLE);
        undo.setEnabled(game.canUndo());
        undo.setAlpha(game.canUndo() ? 1f : .48f);
        if (next != null) {
            next.setVisibility(result == Result.SOLVED ? View.VISIBLE : View.GONE);
            scene.node("game-tip").setVisibility(result==Result.SOLVED?View.GONE:View.VISIBLE);
            scene.node("tip-divider").setVisibility(result==Result.SOLVED?View.GONE:View.VISIBLE);
        }
    }
}
