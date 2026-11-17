package org.secuso.privacyfriendly2048.ceramic;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;
import org.secuso.privacyfriendly2048.activities.*;
public final class CeramicNavigation {
    public static void menu(Activity a,View anchor){
        PopupMenu p=new PopupMenu(a,anchor);
        String[] names={CeramicAssets.words(a,"玩法","Game modes"),CeramicAssets.words(a,"设置","Settings"),CeramicAssets.words(a,"统计","Statistics"),CeramicAssets.words(a,"帮助","Help"),CeramicAssets.words(a,"关于","About")};
        Class<?>[] targets={ChallengeMenuActivity.class,SettingsActivity.class,StatsActivity.class,HelpActivity.class,AboutActivity.class};
        for(int i=0;i<names.length;i++)p.getMenu().add(0,i,i,names[i]);
        p.setOnMenuItemClickListener(item->{a.startActivity(new Intent(a,targets[item.getItemId()]));return true;});p.show();
    }
}
