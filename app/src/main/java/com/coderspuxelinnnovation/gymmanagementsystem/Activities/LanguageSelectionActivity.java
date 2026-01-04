package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Registration.LoginActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;

public class LanguageSelectionActivity extends BaseActivity {

    private RadioGroup rgLanguage;
    private RadioButton rbEnglish, rbMarathi;
    private MaterialButton btnContinue;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        prefManager = new PrefManager(this);

        rgLanguage = findViewById(R.id.rgLanguage);
        rbEnglish = findViewById(R.id.rbEnglish);
        rbMarathi = findViewById(R.id.rbMarathi);
        btnContinue = findViewById(R.id.btnContinue);

        // Default to English
        rbEnglish.setChecked(true);

        btnContinue.setOnClickListener(v -> {
            int selectedId = rgLanguage.getCheckedRadioButtonId();
            String langCode = "en";

            if (selectedId == R.id.rbMarathi) {
                langCode = "mr";
            }

            // Save language preference
            prefManager.setLanguage(langCode);
            prefManager.setFirstTimeLaunch(false);

            // Apply language
            LocaleHelper.setLocale(this, langCode);

            // Go to Login
            Intent intent = new Intent(LanguageSelectionActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back press on language selection
        // User must select a language
    }
}