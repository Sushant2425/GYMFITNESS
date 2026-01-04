package com.coderspuxelinnnovation.gymmanagementsystem.base;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        PrefManager pref = new PrefManager(newBase);
        String lang = pref.getLanguage();
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }
}
