/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Boot animation and OOBE design concepts are adapted from AmeOS 16 by AmeOS Team (Figma Community), licensed under CC BY 4.0.
 * Credits: AmeOS Team (Maksieku, Stellaris, Pjanda, arkainnit, Joke_Bamb, tmrx, trvhz).
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.lineageos.setupwizard.SetupWizardApp.KEY_SEND_METRICS;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

import org.lineageos.setupwizard.util.OobeSpringHelper;

public class LineageSettingsActivity extends BaseSetupWizardActivity {

    private SetupWizardApp mSetupWizardApp;

    private CheckBox mMetrics;
    private CheckBox mNavKeys;
    private CheckBox mAcceptTerms;
    private Button mAgreeButton;

    private boolean mSupportsKeyDisabler = false;

    private final View.OnClickListener mMetricsClickListener = view -> {
        boolean checked = !mMetrics.isChecked();
        mMetrics.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(KEY_SEND_METRICS, checked);
    };

    private final View.OnClickListener mNavKeysClickListener = view -> {
        boolean checked = !mNavKeys.isChecked();
        mNavKeys.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(DISABLE_NAV_KEYS, checked);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetupWizardApp = (SetupWizardApp) getApplication();

        try {
            getWindow().setStatusBarColor(0xFFF6FCFF);
            getWindow().setNavigationBarColor(0xFFEDF9FF);
            WindowInsetsController wic = getWindow().getInsetsController();
            if (wic != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                wic.setSystemBarsAppearance(mask, mask);
            }
        } catch (Throwable ignored) {}

        String os_name = getString(R.string.os_name);
        View metricsRow = findViewById(R.id.metrics);
        if (metricsRow != null) {
            metricsRow.setOnClickListener(mMetricsClickListener);
        }
        String metricsHelpImproveLineage =
                getString(R.string.services_help_improve_cm, os_name);
        String metricsSummary = getString(R.string.services_metrics_label,
                metricsHelpImproveLineage, os_name, os_name);
        final SpannableStringBuilder metricsSpan = new SpannableStringBuilder(metricsSummary);
        metricsSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, metricsHelpImproveLineage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView metrics = findViewById(R.id.enable_metrics_summary);
        if (metrics != null) {
            metrics.setText(metricsSpan);
        }
        mMetrics = findViewById(R.id.enable_metrics_checkbox);

        View navKeysRow = findViewById(R.id.nav_keys);
        mNavKeys = findViewById(R.id.nav_keys_checkbox);
        mSupportsKeyDisabler = isKeyDisablerSupported(this);
        if (navKeysRow != null && mNavKeys != null) {
            navKeysRow.setOnClickListener(mNavKeysClickListener);
            if (mSupportsKeyDisabler) {
                mNavKeys.setChecked(LineageSettings.System.getIntForUser(getContentResolver(),
                        LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0);
            } else {
                navKeysRow.setVisibility(View.GONE);
            }
        }

        View acceptTermsRow = findViewById(R.id.accept_terms_row);
        mAcceptTerms = findViewById(R.id.accept_terms_checkbox);
        mAgreeButton = findViewById(R.id.terms_agree_button);
        ImageButton backBtn = findViewById(R.id.terms_back_button);

        if (acceptTermsRow != null && mAcceptTerms != null) {
            OobeSpringHelper.attachPressSpring(acceptTermsRow);
            acceptTermsRow.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                boolean nextState = !mAcceptTerms.isChecked();
                mAcceptTerms.setChecked(nextState);
                if (mAgreeButton != null) {
                    mAgreeButton.setEnabled(nextState);
                    mAgreeButton.setAlpha(nextState ? 1.0f : 0.45f);
                }
            });
        }

        if (backBtn != null) {
            OobeSpringHelper.attachPressSpring(backBtn);
            backBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
            });
        }

        if (mAgreeButton != null) {
            OobeSpringHelper.attachPressSpring(mAgreeButton);
            mAgreeButton.setOnClickListener(v -> {
                if (mAcceptTerms != null && !mAcceptTerms.isChecked()) {
                    mAcceptTerms.setChecked(true);
                    mAgreeButton.setEnabled(true);
                    mAgreeButton.setAlpha(1.0f);
                }
                OobeSpringHelper.vibrateSuccess(this);
                onNextPressed();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisableNavkeysOption();
        updateMetricsOption();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_lineage_settings;
    }

    @Override
    protected int getTitleResId() {
        return -1;
    }

    @Override
    protected int getIconResId() {
        return -1;
    }

    @Override
    protected com.google.android.setupdesign.GlifLayout getGlifLayout() {
        return null;
    }

    private void updateMetricsOption() {
        if (mMetrics == null) return;
        final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
        boolean metricsChecked =
                !myPageBundle.containsKey(KEY_SEND_METRICS) || myPageBundle
                        .getBoolean(KEY_SEND_METRICS);
        mMetrics.setChecked(metricsChecked);
        myPageBundle.putBoolean(KEY_SEND_METRICS, metricsChecked);
    }

    private void updateDisableNavkeysOption() {
        if (mSupportsKeyDisabler && mNavKeys != null) {
            final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
            boolean enabled = LineageSettings.System.getIntForUser(getContentResolver(),
                    LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
            boolean checked = myPageBundle.containsKey(DISABLE_NAV_KEYS) ?
                    myPageBundle.getBoolean(DISABLE_NAV_KEYS) :
                    enabled;
            mNavKeys.setChecked(checked);
            myPageBundle.putBoolean(DISABLE_NAV_KEYS, checked);
        }
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }
}
