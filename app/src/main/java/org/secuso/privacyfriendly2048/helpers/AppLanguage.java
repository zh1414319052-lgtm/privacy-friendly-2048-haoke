package org.secuso.privacyfriendly2048.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public final class AppLanguage {
    private AppLanguage() {}

    public static Locale fromPrimary(Locale primary) {
        return primary != null && "zh".equalsIgnoreCase(primary.getLanguage())
                ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
    }

    public static Locale fromConfiguration(Configuration config) {
        Locale primary = Build.VERSION.SDK_INT >= 24
                ? (config.getLocales().isEmpty() ? null : config.getLocales().get(0))
                : config.locale;
        return fromPrimary(primary);
    }

    public static Locale systemLanguage() {
        return fromConfiguration(Resources.getSystem().getConfiguration());
    }

    public static Context localize(Context base, Locale language) {
        Configuration override = new Configuration();
        override.setLocale(language); // A single locale: secondary system languages must not match.
        return base.createConfigurationContext(override);
    }
}
