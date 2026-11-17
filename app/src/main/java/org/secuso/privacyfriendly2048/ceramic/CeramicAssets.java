package org.secuso.privacyfriendly2048.ceramic;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.*;
import java.io.*;
import java.util.*;

/** Immutable, shared design resources. PNGs are copied losslessly by export_ceramic_android.mjs. */
public final class CeramicAssets {
    public static final int BG=0xfff1f6f5, INK=0xff173e49, MUTED=0xff627b80, BLUE=0xff286b7b;
    private static JSONObject design, materials, english;
    private static Typeface font;
    private static final Map<String,Bitmap> bitmaps=new HashMap<>();
    private static final Map<Integer,Typeface> fonts=new HashMap<>();
    private CeramicAssets() {}
    public static synchronized JSONObject design(Context c) {
        if(design==null){design=json(c,"design.json");materials=json(c,"materials.json");}
        return design;
    }
    private static JSONObject json(Context c,String name){
        try(InputStream in=c.getAssets().open("ceramic/"+name); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            return new JSONObject(out.toString("UTF-8"));
        }catch(Exception e){throw new IllegalStateException("Missing ceramic asset: "+name,e);}
    }
    public static JSONObject board(Context c,String id){
        JSONArray a=design(c).optJSONArray("boards");
        for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("id")))return a.optJSONObject(i);
        throw new IllegalArgumentException(id);
    }
    public static synchronized Typeface font(Context c,int weight){
        Typeface value=fonts.get(weight);if(value!=null)return value;
        if(font==null)font=Typeface.createFromAsset(c.getAssets(),"ceramic/NotoSansSC-VF.ttf");
        value=Build.VERSION.SDK_INT>=26?new Typeface.Builder(c.getAssets(),"ceramic/NotoSansSC-VF.ttf").setFontVariationSettings("'wght' "+weight).setWeight(weight).build():
            androidx.core.content.res.ResourcesCompat.getFont(c,weight>=650?org.secuso.privacyfriendly2048.R.font.board_sans_bold:org.secuso.privacyfriendly2048.R.font.board_sans_regular);
        fonts.put(weight,value);return value;
    }
    public static synchronized Bitmap bitmap(Context c,String file){
        Bitmap b=bitmaps.get(file);if(b==null){try(InputStream in=c.getAssets().open("ceramic/"+file)){BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;b=BitmapFactory.decodeStream(in,null,o);if(b==null)throw new IOException(file);bitmaps.put(file,b);}catch(IOException e){throw new IllegalStateException(e);}}return b;
    }
    public static boolean night(Context c){return (c.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    public static int background(Context c){return night(c)?0xff152d38:BG;}
    public static int foreground(Context c,int color){if(!night(c))return color;if(color==INK)return 0xffe8f4f2;if(color==MUTED)return 0xffabc0c1;if(color==BLUE)return 0xff9ad9e5;return color;}
    public static boolean chinese(Context c){return c.getResources().getConfiguration().locale.getLanguage().equals("zh");}
    public static String words(Context c,String zh,String en){return chinese(c)?zh:en;}
    public static String localized(Context c,String text){if(chinese(c))return text;if(english==null)english=json(c,"strings-en.json");return english.optString(text,text);}
    public static int color(String s){
        if(s==null||s.isEmpty()||s.equals("none"))return Color.TRANSPARENT;
        if(s.matches("#[0-9a-fA-F]{8}"))s="#"+s.substring(7,9)+s.substring(1,7);
        return Color.parseColor(s);
    }
    public static String tile(int value,boolean warm){
        if(value<0)return "tile-stone";
        if(value==0)return "hole-light";
        if(warm){if(value==2)return "tile-warm-ivory";if(value==4)return "tile-sand";if(value==8)return "tile-gold";}
        switch(value){case 1:case 2:return "tile-ivory";case 3:case 4:return "tile-ice";case 5:case 8:return "tile-cyan";case 13:case 16:return "tile-teal";case 21:case 32:case 1024:return "tile-deep";case 128:return "tile-sand";case 256:return "tile-orange";default:return "tile-gold";}
    }
    /** Same baseline as the authored 36px/800 tile label (55.875 - 2 in an 81.25px cell). */
    public static void drawNumber(Canvas canvas,android.widget.TextView view){
        String text=view.getText().toString();Paint paint=view.getPaint();paint.setColor(view.getCurrentTextColor());
        float x=(view.getWidth()-paint.measureText(text))/2;
        float y=view.getHeight()/2f+paint.getTextSize()*(13.25f/36f);
        canvas.drawText(text,x,y,paint);
    }
    public static int tileText(int value,boolean warm){String n=tile(value,warm);return n.equals("tile-deep")||n.equals("tile-teal")?0xfff6fcfc:INK;}
    public static Drawable skin(Context c,String name){design(c);if(night(c)){if(name.equals("tray-light"))name="tray-dark";else if(name.equals("hole-light"))name="hole-dark";}return new Skin(bitmap(c,name+".png"),materials.optJSONObject(name));}
    private static final class Skin extends Drawable {
        final Bitmap image;final float px,py,bw,bh;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Skin(Bitmap b,JSONObject spec){image=b;JSONArray r=spec.optJSONArray("body");px=(float)r.optDouble(0);py=(float)r.optDouble(1);bw=(float)r.optDouble(2);bh=(float)r.optDouble(3);}
        @Override public void draw(@NonNull Canvas canvas){Rect r=getBounds();float sx=r.width()/bw,sy=r.height()/bh;canvas.drawBitmap(image,null,new RectF(r.left-px*sx,r.top-py*sy,r.left+(image.getWidth()-px)*sx,r.top+(image.getHeight()-py)*sy),p);}
        @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
        @Override public void setColorFilter(@Nullable ColorFilter f){p.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
