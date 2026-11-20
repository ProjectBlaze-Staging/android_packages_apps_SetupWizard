/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import android.annotation.Nullable;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageButton;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class SimMissingActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SetupWizardUtils.simMissing(this) || !SetupWizardUtils.hasTelephony(this)) {
            finishAction(RESULT_SKIP, new Intent().putExtra("onBackPressed", true));
            return;
        }

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

        ImageButton backBtn = findViewById(R.id.sim_back_button);
        Button skipBtn = findViewById(R.id.sim_skip_button);

        if (backBtn != null) {
            OobeSpringHelper.attachPressSpring(backBtn);
            backBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
            });
        }

        if (skipBtn != null) {
            OobeSpringHelper.attachPressSpring(skipBtn);
            skipBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateSuccess(this);
                onSkipPressed();
            });
        }

        setNextAllowed(true);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.sim_missing_page;
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
}
