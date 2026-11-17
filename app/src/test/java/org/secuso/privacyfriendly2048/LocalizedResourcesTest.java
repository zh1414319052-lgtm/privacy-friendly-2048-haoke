package org.secuso.privacyfriendly2048;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.secuso.privacyfriendly2048.helpers.AppLanguage;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class LocalizedResourcesTest {
    @Test
    public void recreateResourceContextWhenPrimaryLanguageChanges() {
        check("zh-TW,en-US", "隐私友好2048", "开始新游戏", "统计");
        check("en-US,zh-CN", "Privacy Friendly 2048", "Start New Game", "Statistics");
        check("ja-JP,zh-TW", "Privacy Friendly 2048", "Start New Game", "Statistics");
        check("zh-Hant-HK,ja-JP", "隐私友好2048", "开始新游戏", "统计");
        check("fr-FR", "Privacy Friendly 2048", "Start New Game", "Statistics");
    }

    private void check(String tags, String name, String action, String stats) {
        Context base = RuntimeEnvironment.getApplication();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocales(LocaleList.forLanguageTags(tags));
        Context scoped = AppLanguage.localize(base.createConfigurationContext(config),
                AppLanguage.fromConfiguration(config));
        assertEquals(tags, name, scoped.getString(R.string.launcher_name));
        assertEquals(tags, action, scoped.getString(R.string.new_Game_Button));
        assertEquals(tags, stats, scoped.getString(R.string.action_stat));
    }
}
