package org.secuso.privacyfriendly2048.ceramic;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.PathParser;
import org.json.*;
import java.util.*;

/** Native object renderer for the approved editable design, not a screenshot overlay. */
public final class CeramicScene extends ViewGroup {
    private final Map<String,View> nodes=new HashMap<>();
    private final Map<String,JSONObject> specs=new HashMap<>();
    private final Map<View,Box> boxes=new HashMap<>();
    private float baseWidth,baseHeight;
    private float scale=1;
    private static final class Box {float x,y,w,h;Box(double x,double y,double w,double h){this.x=(float)x;this.y=(float)y;this.w=(float)w;this.h=(float)h;}}

    public CeramicScene(Context c,String page){this(c,page,false);}
    public CeramicScene(Context c,String page,boolean modal){
        super(c);JSONObject b=CeramicAssets.board(c,page);baseWidth=(float)b.optDouble("width");baseHeight=(float)b.optDouble("height")-56;
        setClipChildren(false);setClipToPadding(false);setBackgroundColor(CeramicAssets.background(c));
        JSONArray children=b.optJSONArray("children");
        if(modal){JSONArray selected=new JSONArray();for(int i=0;i<children.length();i++){JSONObject n=children.optJSONObject(i);if(n.optString("id").startsWith("modal-")&&!n.optString("id").equals("modal-scrim"))selected.put(n);}baseHeight=340;setBackgroundColor(Color.TRANSPARENT);parse(selected,0,-290,1,1,null);}
        else parse(children,0,-32,1,1,null);
    }
    public static ScrollView install(AppCompatActivity a,CeramicScene scene){
        if(a.getSupportActionBar()!=null)a.getSupportActionBar().hide();
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        a.getWindow().setStatusBarColor(CeramicAssets.background(a));a.getWindow().setNavigationBarColor(CeramicAssets.background(a));
        int flags=0;if(!CeramicAssets.night(a)){flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;}
        a.getWindow().getDecorView().setSystemUiVisibility(flags);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipChildren(false);scroll.setClipToPadding(false);scroll.setBackgroundColor(CeramicAssets.background(a));scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(scene,new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));a.setContentView(scroll);return scroll;
    }
    private void parse(JSONArray a,float ox,float oy,float sx,float sy,ViewGroup parent){
        if(a==null)return;
        for(int i=0;i<a.length();i++){
            JSONObject n=a.optJSONObject(i);String id=n.optString("id");if(id.startsWith("system-"))continue;
            String type=n.optString("type");float x=ox+(float)n.optDouble("x")*sx,y=oy+(float)n.optDouble("y")*sy,w=(float)n.optDouble("width")*sx,h=(float)n.optDouble("height")*sy;
            if(type.equals("instance")){
                JSONObject comp=CeramicAssets.design(getContext()).optJSONObject("components").optJSONObject(n.optString("component"));
                ImageView im=new ImageView(getContext());im.setImageBitmap(CeramicAssets.bitmap(getContext(),comp.optJSONArray("children").optJSONObject(0).optString("src")));im.setScaleType(ImageView.ScaleType.FIT_XY);put(id,im,x,y,w,h,n,parent);continue;
            }
            if(type.equals("group")){
                NativeGroup g=new NativeGroup(getContext());put(id,g,x,y,w,h,n,parent);parse(n.optJSONArray("children"),0,0,sx,sy,g);continue;
            }
            View v;
            if(type.equals("text")){v=new Label(getContext(),n,sx,sy);if((parent==null&&!id.startsWith("modal-"))||id.contains("score-")||id.startsWith("game-best-")||id.startsWith("open-modes-")){TextView label=(TextView)v;label.setTextColor(CeramicAssets.foreground(getContext(),label.getCurrentTextColor()));}}
            else v=new Shape(getContext(),n);
            put(id,v,x,y,w,h,n,parent);
        }
    }
    private void put(String id,View v,float x,float y,float w,float h,JSONObject n,ViewGroup parent){
        v.setTag(id);v.setImportantForAccessibility(v instanceof TextView?IMPORTANT_FOR_ACCESSIBILITY_YES:IMPORTANT_FOR_ACCESSIBILITY_NO);
        boxes.put(v,new Box(x,y,w,h));nodes.put(id,v);specs.put(id,n);if(parent==null)addView(v);else parent.addView(v);
    }
    public void setContentHeight(float height){baseHeight=height-56;requestLayout();}
    public View node(String id){View v=nodes.get(id);if(v==null)throw new IllegalArgumentException("Missing node "+id);return v;}
    public TextView text(String id){return (TextView)node(id);}
    public void text(String id,CharSequence value){text(id).setText(value);}
    public void hide(String id){node(id).setVisibility(GONE);}
    public void move(String id,float x,float y,float w,float h){View v=node(id);boxes.put(v,new Box(x,y-32,w,h));requestLayout();}
    public void replace(String id,View replacement){
        View old=node(id);ViewGroup parent=(ViewGroup)old.getParent();int index=parent.indexOfChild(old);Box b=boxes.remove(old);parent.removeView(old);parent.addView(replacement,index);boxes.put(replacement,b);replacement.setTag(id);nodes.put(id,replacement);requestLayout();
    }
    public void addNative(String id,View v,float x,float y,float w,float h){put(id,v,x,y-32,w,h,new JSONObject(),null);}
    public void action(String id,int resourceId,String label,Runnable run){View v=node(id);if(resourceId!=View.NO_ID)v.setId(resourceId);v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);v.setContentDescription(label);v.setFocusable(true);v.setClickable(true);v.setSoundEffectsEnabled(false);v.setOnClickListener(ignored->{run.run();});}
    public void landscapeGame(){
        if(getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE)return;
        baseWidth=800;baseHeight=420;
        move("game-back",24,44,48,48);move("game-menu",728,44,48,48);
        move("game-title",377,42,310,35);move("game-subtitle",377,77,310,23);
        move("game-board",24,112,300,300);move("game-score",376,119,184,66);move("game-best",576,119,184,66);
        move("game-hint",376,205,384,29);move("undo",376,278,184,60);move("restart",576,278,184,60);
        move("tip-divider",456,359,224,1);move("game-tip",392,374,352,28);
        if(nodes.containsKey("attempts"))move("attempts",376,241,384,26);
        if(nodes.containsKey("result"))move("result",376,410,384,30);
        if(nodes.containsKey("advance"))move("advance",90,44,240,48);
    }
    @Override protected void onMeasure(int ws,int hs){
        int width=MeasureSpec.getSize(ws);scale=width/baseWidth;
        // Portrait uses the exact design proportions. In landscape keep a readable centred column.
        if(baseWidth==800)scale=Math.min(scale,(getResources().getDisplayMetrics().heightPixels-56*getResources().getDisplayMetrics().density)/baseHeight);
        int height=Math.round(baseHeight*scale);
        if(MeasureSpec.getMode(hs)==MeasureSpec.EXACTLY)height=Math.max(height,MeasureSpec.getSize(hs));
        else if(MeasureSpec.getMode(hs)==MeasureSpec.AT_MOST)height=Math.min(height,MeasureSpec.getSize(hs));
        measureChildrenAt(this);setMeasuredDimension(width,height);
    }
    private void measureChildrenAt(ViewGroup g){for(int i=0;i<g.getChildCount();i++){View v=g.getChildAt(i);Box b=boxes.get(v);if(b==null)continue;if(v instanceof Label)((Label)v).scale(scale);v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(b.w*scale)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(b.h*scale)),MeasureSpec.EXACTLY));if(v instanceof NativeGroup)measureChildrenAt((ViewGroup)v);}}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){layoutChildren(this,(getWidth()-baseWidth*scale)/2);}
    private void layoutChildren(ViewGroup g,float offset){for(int i=0;i<g.getChildCount();i++){View v=g.getChildAt(i);Box q=boxes.get(v);if(q==null)continue;int x=Math.round(q.x*scale+offset),y=Math.round(q.y*scale);v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());if(v instanceof NativeGroup)layoutChildren((ViewGroup)v,0);}}
    private final class NativeGroup extends ViewGroup {
        NativeGroup(Context c){super(c);setClipChildren(false);setClipToPadding(false);}
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h));}
        @Override protected void onLayout(boolean c,int l,int t,int r,int b){}
    }
    public static final class Label extends AppCompatTextView {
        private final float size,baseline,lineHeight;private final String align;private float scale=1;
        Label(Context c,JSONObject n,float sx,float sy){super(c);size=(float)n.optDouble("fontSize",16)*sx;baseline=(float)n.optDouble("baseline",size*1.2)*sy;lineHeight=(float)n.optDouble("lineHeight",1.12)*size;align=n.optString("align","left");setText(CeramicAssets.localized(c,n.optString("text")));setTextColor(CeramicAssets.color(n.optString("fill","#173E49")));setTypeface(CeramicAssets.font(c,n.optInt("fontWeight",500)));setIncludeFontPadding(false);setPadding(0,0,0,0);setGravity(Gravity.TOP);}
        void scale(float value){scale=value;setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,size*scale*getResources().getConfiguration().fontScale);}
        @Override protected void onDraw(Canvas canvas){
            getPaint().setColor(getCurrentTextColor());String[] lines=getText().toString().split("\n",-1);float fs=getResources().getConfiguration().fontScale;
            getPaint().setTextSize(size*scale*fs);
            float widest=0;for(String line:lines)widest=Math.max(widest,getPaint().measureText(line));
            if(widest>getWidth())getPaint().setTextSize(getPaint().getTextSize()*getWidth()/widest);
            float available=getHeight()/Math.max(1,lines.length);
            if(getPaint().getFontSpacing()>available)getPaint().setTextSize(getPaint().getTextSize()*available/getPaint().getFontSpacing());
            for(int i=0;i<lines.length;i++){String s=lines[i];float width=getPaint().measureText(s),x=align.equals("center")?(getWidth()-width)/2:align.equals("right")?getWidth()-width:0;canvas.drawText(s,x,(baseline+i*lineHeight*fs)*scale,getPaint());}
        }
    }
    private static final class Shape extends View {
        private final JSONObject n;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path;
        Shape(Context c,JSONObject n){super(c);this.n=n;path=n.optString("type").equals("path")?PathParser.createPathFromPathData(n.optString("d")):null;}
        @Override protected void onDraw(Canvas c){float w=(float)n.optDouble("width",1),h=(float)n.optDouble("height",1);c.save();c.scale(getWidth()/w,getHeight()/h);String fill=n.optString("fill","none"),stroke=n.optString("stroke","none");float sw=(float)n.optDouble("strokeWidth");if(!fill.equals("none")){p.setColor(surface(fill));p.setStyle(Paint.Style.FILL);draw(c,w,h);}if(sw>0&&!stroke.equals("none")){p.setColor(surface(stroke));p.setStrokeWidth(sw);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);draw(c,w,h);}c.restore();}
        private int surface(String s){int value=CeramicAssets.color(s);if(CeramicAssets.night(getContext())){String id=n.optString("id");if(id.matches("dot[123]"))return CeramicAssets.foreground(getContext(),value);if(n.optString("type").equals("path")&&(id.startsWith("game-back")||id.startsWith("game-menu")||id.startsWith("modes-back")||id.startsWith("open-modes")))return CeramicAssets.foreground(getContext(),value);if(value==0xfff9fcfb)return 0xff213e4a;if(value==0xffe0eceb)return 0xff3c5963;}return value;}
        private void draw(Canvas c,float w,float h){if(path!=null)c.drawPath(path,p);else c.drawRoundRect(0,0,w,h,(float)n.optDouble("radius"),(float)n.optDouble("radius"),p);}
    }
}
