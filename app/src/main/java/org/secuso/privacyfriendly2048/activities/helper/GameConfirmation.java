package org.secuso.privacyfriendly2048.activities.helper;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.ceramic.*;
public final class GameConfirmation {
    private GameConfirmation(){}
    public static AlertDialog show(Context c,int title,CharSequence message,int confirmLabel,Runnable confirm){
        CeramicScene scene=new CeramicScene(c,"restart",true);
        scene.text("modal-title",c.getString(title));
        scene.text("modal-copy",message);
        scene.text("modal-confirm-label",c.getString(confirmLabel));
        AlertDialog dialog=new AlertDialog.Builder(c).setView(scene).create();
        scene.action("modal-cancel",android.R.id.button2,c.getString(R.string.cancel),dialog::dismiss);
        scene.action("modal-confirm",android.R.id.button1,c.getString(confirmLabel),()->{dialog.dismiss();confirm.run();});
        dialog.show();dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width=c.getResources().getDisplayMetrics().widthPixels;
        int height=c.getResources().getDisplayMetrics().heightPixels;
        dialog.getWindow().setLayout(Math.min(width,Math.round(height*1.05f)),ViewGroup.LayoutParams.WRAP_CONTENT);
        android.view.WindowManager.LayoutParams params=dialog.getWindow().getAttributes();
        params.gravity=android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL;
        float density=c.getResources().getDisplayMetrics().density;
        int available=height-Math.round(56*density);
        int dialogWidth=Math.min(width,Math.round(height*1.05f));
        params.y=Math.min(Math.round(258f*dialogWidth/432),Math.max(0,(available-Math.round(340f*dialogWidth/432))/2));
        dialog.getWindow().setAttributes(params);
        scene.node("modal-cancel").requestFocus();return dialog;
    }
}
