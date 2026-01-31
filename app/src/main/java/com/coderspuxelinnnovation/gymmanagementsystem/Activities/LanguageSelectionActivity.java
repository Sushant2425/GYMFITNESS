package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Registration.LoginActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;

public class LanguageSelectionActivity extends BaseActivity {

    private RadioButton rbEnglish, rbMarathi;
    private MaterialButton btnContinue;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        prefManager = new PrefManager(this);

        // Direct RadioButton references (NO RadioGroup needed)
        rbEnglish = findViewById(R.id.rbEnglish);
        rbMarathi = findViewById(R.id.rbMarathi);
        btnContinue = findViewById(R.id.btnContinue);

        // Default to English
        if (rbEnglish != null) {
            rbEnglish.setChecked(true);
        }

        // Handle individual RadioButton clicks
        if (rbEnglish != null) {
            rbEnglish.setOnClickListener(v -> {
                rbMarathi.setChecked(false);
                rbEnglish.setChecked(true);
            });
        }

        if (rbMarathi != null) {
            rbMarathi.setOnClickListener(v -> {
                rbEnglish.setChecked(false);
                rbMarathi.setChecked(true);
            });
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                // Check which button is selected
                boolean isEnglish = rbEnglish != null && rbEnglish.isChecked();
                boolean isMarathi = rbMarathi != null && rbMarathi.isChecked();

                if (!isEnglish && !isMarathi) {
                    // No selection - show validation
                    btnContinue.setText("Please select a language first");
                    btnContinue.postDelayed(() ->
                            btnContinue.setText(getString(R.string.continue_text)), 2000);
                    return;
                }

                String langCode = isEnglish ? "en" : "mr";

                // Save language preference
                prefManager.setLanguage(langCode);
                prefManager.setFirstTimeLaunch(false);

                // Apply language with smooth transition
                LocaleHelper.setLocale(this, langCode);

                // Navigate to Login
                Intent intent = new Intent(LanguageSelectionActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity();
            });
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back press - user must select language
    }
}
