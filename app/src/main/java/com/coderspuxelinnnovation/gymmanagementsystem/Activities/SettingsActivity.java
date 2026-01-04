package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.widget.Toolbar;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;

public class SettingsActivity extends BaseActivity {

    private RadioGroup rgLanguage;
    private RadioButton rbEnglish, rbMarathi;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefManager = new PrefManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.settings));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rgLanguage = findViewById(R.id.rgLanguage);
        rbEnglish = findViewById(R.id.rbEnglish);
        rbMarathi = findViewById(R.id.rbMarathi);

        // Set already selected language
        String currentLang = prefManager.getLanguage();
        if (currentLang.equals("mr")) {
            rbMarathi.setChecked(true);
        } else {
            rbEnglish.setChecked(true);
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEnglish) {
                changeLanguage("en");
            } else if (checkedId == R.id.rbMarathi) {
                changeLanguage("mr");
            }
        });
    }

    private void changeLanguage(String langCode) {
        prefManager.setLanguage(langCode);
        LocaleHelper.setLocale(this, langCode);

        // Restart app from Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}