package org.secuso.privacyfriendly2048.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.activities.helper.LocalizedActivity;
import org.secuso.privacyfriendly2048.challenges.ChallengeCatalog;
import org.secuso.privacyfriendly2048.challenges.ChallengeStore;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeUi.*;

public final class ChallengeMenuActivity extends LocalizedActivity {
    private static final String SHOW_PUZZLES = "show_puzzles";
    private ScrollView scroll;
    private LinearLayout list;
    private ChallengeStore store;
    private boolean puzzles;
    private long day;
    private org.secuso.privacyfriendly2048.ceramic.CeramicAudio audio;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        puzzles = saved != null && saved.getBoolean(SHOW_PUZZLES);
        store = new ChallengeStore(this);
        audio=new org.secuso.privacyfriendly2048.ceramic.CeramicAudio(this);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        audio.resume();
        day = ChallengeCatalog.localDay();
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putBoolean(SHOW_PUZZLES, puzzles);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onBackPressed() {
        if (puzzles) {
            puzzles = false;
            render();
            scroll.scrollTo(0, 0);
        } else super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override protected void onPause(){audio.pause();super.onPause();}
    @Override protected void onDestroy(){audio.release();super.onDestroy();}
    private void render() {
        org.secuso.privacyfriendly2048.ceramic.CeramicScene scene=new org.secuso.privacyfriendly2048.ceramic.CeramicScene(this,"modes");
        scene.action("modes-back",View.NO_ID,getString(R.string.cancel),this::onBackPressed);
        scroll=org.secuso.privacyfriendly2048.ceramic.CeramicScene.install(this,scene);
        if(!puzzles){
            Mode[] modes={Mode.SPRINT,Mode.PUZZLE,Mode.OBSTACLES,Mode.FIBONACCI,Mode.DAILY};
            int[] ids={R.id.challenge_card_sprint,R.id.challenge_card_puzzle,R.id.challenge_card_obstacles,R.id.challenge_card_fibonacci,R.id.challenge_card_daily};
            for(int i=0;i<modes.length;i++){final Mode m=modes[i];
                scene.action("mode-"+i,ids[i],getString(title(m)),()->{
                    audio.play(org.secuso.privacyfriendly2048.ceramic.CeramicAudio.Cue.SELECT);
                    if(m==Mode.PUZZLE){puzzles=true;render();}else startActivity(ChallengeActivity.intent(this,m,0,ChallengeCatalog.localDay()));
                });
            }
            return;
        }
        for(int i=0;i<5;i++)scene.hide("mode-"+i);
        scene.text("modes-title",getString(R.string.challenge_pick_level));
        scene.text("modes-subtitle",org.secuso.privacyfriendly2048.ceramic.CeramicAssets.words(this,"9 个棋局，每一步都有答案。","Nine puzzles. Every move matters."));
        list=stack(this);list.setClipChildren(false);list.setClipToPadding(false);
        scene.addNative("puzzle-list",list,24,232,384,ChallengeCatalog.puzzleCount()*116);
        scene.setContentHeight(260+ChallengeCatalog.puzzleCount()*116);

        setTitle(puzzles ? R.string.challenge_pick_level : R.string.action_modes);
        if (puzzles) {
            for (int n = 0; n < ChallengeCatalog.puzzleCount(); n++) {
                final int index = n;
                Setup setup = ChallengeCatalog.puzzle(n);
                String detail = goal(this, setup, setup.target) + "  ·  " +
                        getString(R.string.challenge_move_limit, setup.limit);
                card(View.NO_ID, getString(R.string.challenge_level, n + 1), detail, progress(setup),
                        () -> startActivity(ChallengeActivity.intent(this, Mode.PUZZLE, index, day)));
            }
            return;
        }
        addMode(Mode.SPRINT, R.id.challenge_card_sprint);
        addMode(Mode.PUZZLE, R.id.challenge_card_puzzle);
        addMode(Mode.OBSTACLES, R.id.challenge_card_obstacles);
        addMode(Mode.FIBONACCI, R.id.challenge_card_fibonacci);
        addMode(Mode.DAILY, R.id.challenge_card_daily);
    }

    private void addMode(Mode mode, int id) {
        String record;
        if (mode == Mode.PUZZLE) {
            int solved = 0;
            for (int i = 0; i < ChallengeCatalog.puzzleCount(); i++) {
                if (store.record(ChallengeCatalog.puzzle(i)) > 0) solved++;
            }
            record = getString(R.string.challenge_level_count, ChallengeCatalog.puzzleCount(), solved);
        } else {
            Setup setup = mode == Mode.DAILY ? ChallengeCatalog.daily(day) : ChallengeCatalog.endless(mode, 0);
            record = progress(setup);
        }
        card(id, getString(title(mode)), getString(description(mode)), record, () -> {
            if (mode == Mode.PUZZLE) {
                puzzles = true;
                render();
                scroll.scrollTo(0, 0);
            } else {
                long selectedDay = mode == Mode.DAILY ? ChallengeCatalog.localDay() : day;
                startActivity(ChallengeActivity.intent(this, mode, 0, selectedDay));
            }
        });
    }

    private String progress(Setup setup) {
        long record = store.record(setup);
        String progress = record == 0 ? "" : getString(setup.isPuzzle()
                ? R.string.challenge_best_moves : R.string.challenge_best_score, record);
        if (store.canContinue(setup)) {
            return progress.isEmpty() ? getString(R.string.challenge_resume)
                    : getString(R.string.challenge_resume) + "  ·  " + progress;
        }
        return progress;
    }

    private void card(int id, String title, String description, String progress, Runnable open) {
        LinearLayout card = stack(this);
        card.setId(id);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.skin(this,"level-card"));
        pad(card, 16, 12);
        card.setSoundEffectsEnabled(false);card.setOnClickListener(v -> {audio.play(org.secuso.privacyfriendly2048.ceramic.CeramicAudio.Cue.SELECT);open.run();});
        TextView heading = text(this, title, 19, R.color.challenge_title, true);
        heading.setTextColor(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.INK);
        add(card, heading, 0);
        add(card, text(this, description, 14, R.color.challenge_muted, false), 3);
        if (!progress.isEmpty()) {
            add(card, text(this, progress, 13, R.color.challenge_accent, true), 6);
        }
        for(int k=1;k<card.getChildCount();k++)if(card.getChildAt(k) instanceof TextView)((TextView)card.getChildAt(k)).setTextColor(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.MUTED);
        add(list, card, list.getChildCount() == 0 ? 0 : 10);
    }
}
