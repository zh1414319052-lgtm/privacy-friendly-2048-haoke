package org.secuso.privacyfriendly2048.challenges;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.widget.TextViewCompat;

import org.secuso.privacyfriendly2048.R;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;
import static org.secuso.privacyfriendly2048.challenges.ChallengeUi.*;

public final class ChallengeBoard extends android.view.ViewGroup {
    public interface MoveListener {
        void move(Direction direction);
    }

    private static final int[] ACTIONS = {
            R.id.challenge_left, R.id.challenge_right, R.id.challenge_up, R.id.challenge_down
    };
    private static final int[] ACTION_LABELS = {
            R.string.swipe_left, R.string.swipe_right, R.string.swipe_up, R.string.swipe_down
    };
    private static final int[] BLUE = {0xFFD1E1F2, 0xFFB4CEE7, 0xFF9BC1E3, 0xFF79ACD6,
            0xFF4B86B9, 0xFF346B9B, 0xFF24537D, 0xFF163B61, 0xFF102E4A};
    private static final int[] CLASSIC = {0xFFF4E5D2, 0xFFEED6B2, 0xFFF2BC86, 0xFFEAA46C,
            0xFFD78653, 0xFFB46238, 0xFF994528, 0xFF7D3626, 0xFF612B21};

    private final int size;
    private final MoveListener listener;
    private final boolean classicPalette;
    private final boolean animate;
    private TextView[] cells;
    private int[] shown;
    private float downX;
    private float downY;
    private final int swipeThreshold;

    public ChallengeBoard(Context context, int size, boolean classic, boolean animate, MoveListener listener) {
        super(context);
        this.size = size;
        this.listener = listener;
        classicPalette = classic;
        this.animate = animate;
        swipeThreshold = Math.max(dp(context, 16), ViewConfiguration.get(context).getScaledTouchSlop() * 2);
        setId(R.id.challenge_board);
        setClipChildren(false);setClipToPadding(false);
        setFocusable(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(context.getString(R.string.challenge_board_label));
        setBackground(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.skin(context,"tray-light"));
        build(size);
    }

    private void build(int size) {
        cells = new TextView[size * size]; shown = new int[cells.length];
        java.util.Arrays.fill(shown, Integer.MIN_VALUE);
        for(int i=0;i<cells.length;i++){
            TextView tile=new androidx.appcompat.widget.AppCompatTextView(getContext()){@Override protected void onDraw(android.graphics.Canvas c){org.secuso.privacyfriendly2048.ceramic.CeramicAssets.drawNumber(c,this);}};
            tile.setTypeface(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.font(getContext(),800));
            tile.setGravity(Gravity.CENTER);tile.setIncludeFontPadding(false);tile.setPadding(0,0,0,0);
            tile.setMaxLines(1);tile.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            cells[i]=tile;addView(tile);
        }
    }
    public void render(State state,boolean moved){int[] v=new int[cells.length];for(int i=0;i<v.length;i++)v[i]=state.tile(i);renderValues(v,moved);}
    public void renderValues(int[] values,boolean moved){
        if(values.length!=cells.length)throw new IllegalArgumentException("Board dimensions");
        for(int i=0;i<cells.length;i++){
            int value=values[i];if(shown[i]==value)continue;shown[i]=value;TextView tile=cells[i];
            tile.setBackground(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.skin(getContext(),
                org.secuso.privacyfriendly2048.ceramic.CeramicAssets.tile(value,classicPalette)));
            tile.setTextColor(org.secuso.privacyfriendly2048.ceramic.CeramicAssets.tileText(value,classicPalette));
            tile.setText(value<=0?"":Integer.toString(value));
            tile.setContentDescription(value==STONE?getContext().getString(R.string.challenge_stone_cell)
                :value==0?getContext().getString(R.string.challenge_empty_cell):Integer.toString(value));
            tile.animate().cancel();tile.setAlpha(1);tile.setScaleX(1);tile.setScaleY(1);
            if(moved&&animate&&value>0){tile.setScaleX(.94f);tile.setScaleY(.94f);tile.animate().scaleX(1).scaleY(1).setDuration(115).start();}
        }
        requestLayout();
    }
    @Override protected void onMeasure(int ws,int hs){
        int side=MeasureSpec.getSize(ws);
        if(MeasureSpec.getMode(hs)!=MeasureSpec.UNSPECIFIED)side=Math.min(side,MeasureSpec.getSize(hs));
        setMeasuredDimension(side,side);
        float inset=side*16f/384,gap=side*9f/384,cell=(side-2*inset-(size-1)*gap)/size;
        for(TextView t:cells){float max=cell*(36f/81.25f);int digits=t.getText().length();if(digits>=4)max*=3.2f/digits;
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX,max);t.measure(MeasureSpec.makeMeasureSpec(Math.round(cell),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(cell),MeasureSpec.EXACTLY));}
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        float inset=getWidth()*16f/384,gap=getWidth()*9f/384,cell=(getWidth()-2*inset-(size-1)*gap)/size;
        for(int i=0;i<cells.length;i++){int x=Math.round(inset+(i%size)*(cell+gap)),y=Math.round(inset+(i/size)*(cell+gap));cells[i].layout(x,y,x+cells[i].getMeasuredWidth(),y+cells[i].getMeasuredHeight());}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE: return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) >= swipeThreshold) {
                    Direction direction = Math.abs(dx) >= Math.abs(dy)
                            ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                            : (dy > 0 ? Direction.DOWN : Direction.UP);
                    if(listener!=null)listener.move(direction);
                } else performClick();
                return true;
            default: return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (isInTouchMode()) requestFocusFromTouch();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: if(listener!=null)listener.move(Direction.LEFT); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: if(listener!=null)listener.move(Direction.RIGHT); return true;
            case KeyEvent.KEYCODE_DPAD_UP: if(listener!=null)listener.move(Direction.UP); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: if(listener!=null)listener.move(Direction.DOWN); return true;
            default: return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        for (int i = 0; i < ACTIONS.length; i++) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTIONS[i],
                    getContext().getString(ACTION_LABELS[i])));
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (action == ACTIONS[i]) {
                if(listener!=null)listener.move(Direction.values()[i]);
                return true;
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
