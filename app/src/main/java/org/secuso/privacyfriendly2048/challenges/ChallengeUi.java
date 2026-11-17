package org.secuso.privacyfriendly2048.challenges;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.secuso.privacyfriendly2048.R;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

public final class ChallengeUi {
    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private ChallengeUi() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(Context context, int id) {
        return ContextCompat.getColor(context, id);
    }

    public static LinearLayout stack(Context context) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        return list;
    }

    public static ScrollView scroll(Context context) {
        ScrollView view = new ScrollView(context);
        view.setFillViewport(true);
        view.setBackgroundColor(color(context, R.color.challenge_screen));
        return view;
    }

    public static void add(LinearLayout list, android.view.View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH, WRAP);
        params.topMargin = dp(list.getContext(), top);
        list.addView(child, params);
    }

    public static void pad(LinearLayout view, int horizontal, int vertical) {
        Context context = view.getContext();
        int h = dp(context, horizontal);
        int v = dp(context, vertical);
        view.setPadding(h, v, h, v);
    }

    public static TextView text(Context context, CharSequence label, int size, int color, boolean bold) {
        AppCompatTextView text = new AppCompatTextView(context);
        text.setText(label);
        text.setIncludeFontPadding(false);
        text.setTextSize(size);
        text.setTextColor(ChallengeUi.color(context, color));
        text.setTypeface(font(context, bold));
        return text;
    }

    static Typeface font(Context context, boolean bold) {
        return ResourcesCompat.getFont(context, bold ? R.font.board_sans_bold : R.font.board_sans_regular);
    }

    public static GradientDrawable shape(Context context, int fill, int edge, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color(context, fill));
        shape.setCornerRadius(dp(context, radius));
        if (edge != 0) shape.setStroke(dp(context, 1), color(context, edge));
        return shape;
    }

    public static RippleDrawable ripple(Context context, int fill, int edge, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x28749AB0),
                shape(context, fill, edge, radius), null);
    }

    public static AppCompatButton button(Context context, int id, int label) {
        AppCompatButton button = new AppCompatButton(context);
        button.setId(id);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(font(context, true));
        button.setMinHeight(dp(context, 48));
        button.setGravity(Gravity.CENTER);
        button.setTextColor(color(context, R.color.challenge_accent));
        button.setBackground(ripple(context, R.color.challenge_surface, R.color.challenge_edge, 10));
        button.setPadding(dp(context, 9), 0, dp(context, 9), 0);
        return button;
    }

    public static int title(Mode mode) {
        switch (mode) {
            case SPRINT: return R.string.mode_sprint;
            case PUZZLE: return R.string.mode_puzzle;
            case OBSTACLES: return R.string.mode_obstacles;
            case FIBONACCI: return R.string.mode_fibonacci;
            case DAILY: return R.string.mode_daily;
            default: throw new AssertionError(mode);
        }
    }

    public static int description(Mode mode) {
        switch (mode) {
            case SPRINT: return R.string.mode_sprint_desc;
            case PUZZLE: return R.string.mode_puzzle_desc;
            case OBSTACLES: return R.string.mode_obstacles_desc;
            case FIBONACCI: return R.string.mode_fibonacci_desc;
            case DAILY: return R.string.mode_daily_desc;
            default: throw new AssertionError(mode);
        }
    }

    public static String goal(Context context, Setup setup, int target) {
        int id;
        switch (setup.goal) {
            case TILE: id = R.string.challenge_goal_tile; break;
            case SPACE: id = R.string.challenge_goal_space; break;
            case SCORE: id = R.string.challenge_goal_score; break;
            default: throw new AssertionError(setup.goal);
        }
        return context.getString(id, target);
    }
}
