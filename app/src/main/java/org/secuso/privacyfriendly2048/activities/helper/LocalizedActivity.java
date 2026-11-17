package org.secuso.privacyfriendly2048.activities.helper;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import org.secuso.privacyfriendly2048.helpers.AppLanguage;

public abstract class LocalizedActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.localize(base, AppLanguage.systemLanguage()));
    }
}
