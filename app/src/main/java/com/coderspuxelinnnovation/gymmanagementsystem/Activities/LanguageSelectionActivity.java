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

        rbEnglish = findViewById(R.id.rbEnglish);
        rbMarathi = findViewById(R.id.rbMarathi);
        btnContinue = findViewById(R.id.btnContinue);

        if (rbEnglish != null) {
            rbEnglish.setChecked(true);
        }

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
                boolean isEnglish = rbEnglish != null && rbEnglish.isChecked();
                boolean isMarathi = rbMarathi != null && rbMarathi.isChecked();

                if (!isEnglish && !isMarathi) {
                    btnContinue.setText(getString(R.string.please_select_language));
                    btnContinue.postDelayed(() ->
                            btnContinue.setText(getString(R.string.continue_text)), 2000);
                    return;
                }

                String langCode = isEnglish ? "en" : "mr";

                prefManager.setLanguage(langCode);
                prefManager.setFirstTimeLaunch(false);

                LocaleHelper.setLocale(this, langCode);

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