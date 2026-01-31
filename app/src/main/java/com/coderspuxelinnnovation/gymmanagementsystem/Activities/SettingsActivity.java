package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class SettingsActivity extends BaseActivity {

    private ChipGroup rgLanguage;
    private Chip rbEnglish, rbMarathi;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefManager = new PrefManager(this);

        setupToolbar();
        initViews();
        setupLanguageSelection();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rgLanguage = findViewById(R.id.rgLanguage);
        rbEnglish = findViewById(R.id.rbEnglish);
        rbMarathi = findViewById(R.id.rbMarathi);
    }

    private void setupLanguageSelection() {
        // Set currently selected language
        String currentLang = prefManager.getLanguage();
        if (currentLang.equals("mr")) {
            rbMarathi.setChecked(true);
        } else {
            rbEnglish.setChecked(true);
        }

        // Language change listener
        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEnglish) {
                changeLanguage("en");
            } else if (checkedId == R.id.rbMarathi) {
                changeLanguage("mr");
            }
        });
    }

    private void changeLanguage(String langCode) {
        // Save language preference
        prefManager.setLanguage(langCode);
        LocaleHelper.setLocale(this, langCode);

        // Restart app from Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
