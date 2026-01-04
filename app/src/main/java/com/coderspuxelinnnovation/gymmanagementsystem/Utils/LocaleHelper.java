package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
