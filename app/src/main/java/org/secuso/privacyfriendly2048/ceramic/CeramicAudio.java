package org.secuso.privacyfriendly2048.ceramic;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import org.secuso.privacyfriendly2048.R;
import java.util.HashSet;
import java.util.Set;

/** Activity-owned short sounds: one event per action, no background playback. */
public final class CeramicAudio {
    public enum Cue { SELECT, SLIDE, MERGE, STONE, UNDO, CLEAR, END }
    private final Context context;
    private final SoundPool pool;
    private final int[] ids=new int[7];
    private final Set<Integer> loaded=new HashSet<>();
    private boolean active=true,released;
    private int stream;
    private long last;
    public CeramicAudio(Context c){context=c.getApplicationContext();pool=new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)loaded.add(id);});int[] raw={R.raw.ceramic_ui_select,R.raw.ceramic_tile_slide,R.raw.ceramic_tile_merge,R.raw.ceramic_stone_contact,R.raw.ceramic_undo,R.raw.ceramic_puzzle_clear,R.raw.ceramic_out_of_moves};for(int i=0;i<raw.length;i++)ids[i]=pool.load(context,raw[i],1);}
    public void play(Cue cue){if(released||!active||!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ceramic_sound",true))return;int id=ids[cue.ordinal()];long now=SystemClock.uptimeMillis();if(!loaded.contains(id)||now-last<45)return;last=now;if(stream!=0)pool.stop(stream);stream=pool.play(id,.65f,.65f,1,0,1f);}
    public void pause(){active=false;if(!released)pool.autoPause();}
    public void resume(){active=true;}
    public void release(){if(!released){released=true;pool.release();loaded.clear();}}
}
