package org.secuso.privacyfriendly2048;

import org.junit.Test;
import org.secuso.privacyfriendly2048.helpers.AppLanguage;
import java.util.Locale;
import static org.junit.Assert.assertEquals;

public class AppLanguageTest {
    @Test
    public void everyChinesePrimaryMapsToSimplifiedChinese() {
        for (String tag : new String[]{"zh", "zh-CN", "zh-SG", "zh-TW", "zh-HK", "zh-Hant-HK", "zh-Hans-MY"}) {
            assertEquals(tag, Locale.SIMPLIFIED_CHINESE, AppLanguage.fromPrimary(Locale.forLanguageTag(tag)));
        }
    }

    @Test
    public void englishAndOtherLanguagesFallBackToEnglish() {
        for (String tag : new String[]{"en", "en-GB", "ja-JP", "ru-RU", "ar", "und"}) {
            assertEquals(tag, Locale.ENGLISH, AppLanguage.fromPrimary(Locale.forLanguageTag(tag)));
        }
        assertEquals(Locale.ENGLISH, AppLanguage.fromPrimary(null));
    }
}
