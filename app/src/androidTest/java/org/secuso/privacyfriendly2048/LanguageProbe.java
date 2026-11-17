package org.secuso.privacyfriendly2048;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.secuso.privacyfriendly2048.activities.GameActivity;
import org.secuso.privacyfriendly2048.activities.ChallengeActivity;
import org.secuso.privacyfriendly2048.challenges.ChallengeCatalog;
import org.secuso.privacyfriendly2048.challenges.ChallengeGame;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import org.secuso.privacyfriendly2048.helpers.AppLanguage;

public class LanguageProbe extends Instrumentation {
    private Bundle args;

    @Override
    public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            if ("ceramic_visual".equals(args.getString("mode"))) {result.putString("stream","\n"+new CeramicProbe(this).visual());finish(Activity.RESULT_OK,result);return;}
            if ("ceramic_audio".equals(args.getString("mode"))) {result.putString("stream","\n"+new CeramicProbe(this).audio());finish(Activity.RESULT_OK,result);return;}
            if ("ceramic".equals(args.getString("mode"))) {result.putString("stream","\n"+new CeramicProbe(this).run());finish(Activity.RESULT_OK,result);return;}
            if ("catalog".equals(args.getString("mode"))) {
                result.putString("stream", describePuzzles());
                finish(Activity.RESULT_OK, result);
                return;
            }
            boolean challenge = "challenge_landscape".equals(args.getString("mode"));
            if (challenge || "landscape".equals(args.getString("mode"))) {
                checkLandscape(challenge);
                result.putString("stream", "\nPASS: dark landscape and board recreation preserve score.\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            check("zh-TW,en-US", "隐私友好2048", "开始新游戏");
            check("en-US,zh-CN", "Privacy Friendly 2048", "Start New Game");
            check("ja-JP,zh-TW", "Privacy Friendly 2048", "Start New Game");
            check("zh-Hant-HK,ja-JP", "隐私友好2048", "开始新游戏");
            check("fr-FR", "Privacy Friendly 2048", "Start New Game");
            result.putString("stream", "\nPASS: five isolated locale list configurations and fresh UI resource contexts.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "\nFAIL: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void checkLandscape(boolean challenge) throws Exception {
        Class<? extends Activity> gameClass = challenge ? ChallengeActivity.class : GameActivity.class;
        int scoreId = challenge ? R.id.challenge_score : R.id.points;
        int recordId = challenge ? R.id.challenge_record : R.id.record;
        Context target = getTargetContext();
        Application app = (Application) target.getApplicationContext();
        AtomicReference<Activity> resumed = new AtomicReference<>();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityResumed(Activity a) { if (gameClass.isInstance(a)) resumed.set(a); }
            public void onActivityCreated(Activity a, Bundle state) {}
            public void onActivityStarted(Activity a) {}
            public void onActivityPaused(Activity a) {}
            public void onActivityStopped(Activity a) {}
            public void onActivitySaveInstanceState(Activity a, Bundle state) {}
            public void onActivityDestroyed(Activity a) {}
        });
        runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
        Activity first = startActivitySync(new Intent(target, gameClass)
                .putExtra("mode", ChallengeGame.Mode.OBSTACLES.ordinal())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        AtomicReference<String> score = new AtomicReference<>();
        AtomicReference<String> best = new AtomicReference<>();
        runOnMainSync(() -> {
            score.set(((TextView) first.findViewById(scoreId)).getText().toString());
            best.set(((TextView) first.findViewById(recordId)).getText().toString());
            first.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });
        Activity rotated = null;
        for (int i = 0; i < 80; i++) {
            Activity current = resumed.get();
            if (current != null && current.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                rotated = current;
                break;
            }
            SystemClock.sleep(100);
        }
        if (rotated == null) throw new AssertionError("App-scoped rotation did not reach landscape");
        waitForIdleSync();
        Activity landscape = rotated;
        runOnMainSync(() -> {
            if ((landscape.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    != Configuration.UI_MODE_NIGHT_YES) throw new AssertionError("Dark theme did not survive recreation");
            String after = ((TextView) landscape.findViewById(scoreId)).getText().toString();
            if (!score.get().equals(after)) throw new AssertionError("Score changed on recreation: "+score.get()+" -> "+after);
        });
        // A configuration callback can arrive before the rotation animation finishes.
        SystemClock.sleep(1000);
        waitForIdleSync();
        runOnMainSync(() -> {
            android.view.View decor = landscape.getWindow().getDecorView();
            if (decor.getWidth() <= decor.getHeight()) throw new AssertionError("Landscape layout was not measured");
            TextView record = landscape.findViewById(recordId);
            if (!best.get().contentEquals(record.getText()) || record.getLineCount() != 1
                    || record.getLayout().getLineEnd(0) < record.length()
                    || record.getLayout().getEllipsisCount(0) != 0) {
                throw new AssertionError("Landscape best score truncated or changed: " + record.getText());
            }
        });
        Bitmap screen = getUiAutomation().takeScreenshot();
        if (screen == null || screen.getWidth() <= screen.getHeight()) throw new AssertionError("No landscape screenshot");
        try (FileOutputStream output = new FileOutputStream(new File(target.getCacheDir(), challenge ? "qa_modes_landscape.jpg" : "qa_dark_landscape.jpg"))) {
            screen.compress(Bitmap.CompressFormat.JPEG, 88, output);
        }
        runOnMainSync(landscape::finish);
    }

    private String describePuzzles() {
        StringBuilder display = new StringBuilder("\n");
        for (int i = 0; i < ChallengeCatalog.puzzleCount(); i++) {
            ChallengeGame.Setup setup = ChallengeCatalog.puzzle(i);
            ChallengeGame.State opening = new ChallengeGame(setup).state();
            display.append("Puzzle ").append(i + 1).append(" — ").append(setup.goal)
                    .append(' ').append(setup.target).append(", ").append(setup.limit).append(" moves\n");
            for (int j = 0; j < opening.cellCount(); j++) {
                int tile = opening.tile(j);
                display.append(tile == ChallengeGame.STONE ? "×" : tile == 0 ? "." : Integer.toString(tile));
                display.append((j + 1) % setup.size == 0 ? '\n' : ' ');
            }
            display.append('\n');
        }
        return display.toString();
    }

    private void check(String tags, String name, String action) {
        Context context = getTargetContext();
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= 24) config.setLocales(LocaleList.forLanguageTags(tags));
        else config.setLocale(java.util.Locale.forLanguageTag(tags.split(",")[0]));
        Context localized = AppLanguage.localize(context, AppLanguage.fromConfiguration(config));
        if (!name.equals(localized.getString(R.string.launcher_name))
                || !action.equals(localized.getString(R.string.new_Game_Button))) {
            throw new AssertionError(tags + ": " + localized.getString(R.string.launcher_name)
                    + " / " + localized.getString(R.string.new_Game_Button));
        }
    }
}
