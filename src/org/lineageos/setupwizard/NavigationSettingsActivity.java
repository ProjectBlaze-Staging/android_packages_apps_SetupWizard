/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static org.lineageos.internal.util.DeviceKeysConstants.KEY_MASK_APP_SWITCH;
import static org.lineageos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;

import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;

import lineageos.providers.LineageSettings;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class NavigationSettingsActivity extends BaseSetupWizardActivity {

    private SetupWizardApp mSetupWizardApp;
    private String mSelection = NAV_BAR_MODE_GESTURAL_OVERLAY;

    private View mCardGestures;
    private View mCardButtons;
    private CheckBox mHideGesturalHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mSetupWizardApp = (SetupWizardApp) getApplication();
        boolean navBarEnabled = false;
        if (mSetupWizardApp.getSettingsBundle().containsKey(DISABLE_NAV_KEYS)) {
            navBarEnabled = mSetupWizardApp.getSettingsBundle().getBoolean(DISABLE_NAV_KEYS);
        }

        int deviceKeys = getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys);
        boolean hasHomeKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        mCardGestures = findViewById(R.id.card_nav_gestures);
        mCardButtons = findViewById(R.id.card_nav_buttons);
        mHideGesturalHint = findViewById(R.id.hide_navigation_hint);
        ImageButton backBtn = findViewById(R.id.nav_back_button);
        Button continueBtn = findViewById(R.id.nav_continue_button);

        int available = 3;
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            if (mCardGestures != null) mCardGestures.setVisibility(View.GONE);
            mSelection = NAV_BAR_MODE_3BUTTON_OVERLAY;
            available--;
        }
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            if (mCardButtons != null) mCardButtons.setVisibility(View.GONE);
            available--;
        }

        if (!navBarEnabled && hasHomeKey || available <= 1) {
            mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY,
                    NAV_BAR_MODE_3BUTTON_OVERLAY);
            finishAction(RESULT_OK);
            return;
        }

        clipCardToRounded48(mCardGestures);
        clipCardToRounded48(mCardButtons);

        if (mCardGestures != null) {
            OobeSpringHelper.attachPressSpring(mCardGestures);
            mCardGestures.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                selectMode(NAV_BAR_MODE_GESTURAL_OVERLAY);
            });
        }

        if (mCardButtons != null) {
            OobeSpringHelper.attachPressSpring(mCardButtons);
            mCardButtons.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                selectMode(NAV_BAR_MODE_3BUTTON_OVERLAY);
            });
        }

        if (backBtn != null) {
            OobeSpringHelper.attachPressSpring(backBtn);
            backBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
            });
        }

        if (continueBtn != null) {
            OobeSpringHelper.attachPressSpring(continueBtn);
            continueBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateSuccess(this);
                onNextPressed();
            });
        }

        selectMode(mSelection);
    }

    private void clipCardToRounded48(View card) {
        if (card == null) return;
        float d = getResources().getDisplayMetrics().density;
        float outerRadius = 32f * d;
        float innerRadius = Math.max(0f, outerRadius - 6f * d);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), outerRadius);
            }
        });
        if (card instanceof android.view.ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child != null) {
                    child.setClipToOutline(true);
                    child.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), innerRadius);
                        }
                    });
                }
            }
        }
    }

    private void selectMode(String mode) {
        mSelection = mode;
        boolean isGestures = NAV_BAR_MODE_GESTURAL_OVERLAY.equals(mode);
        styleSelectionRing(mCardGestures, isGestures);
        styleSelectionRing(mCardButtons, !isGestures);
        if (mHideGesturalHint != null) {
            mHideGesturalHint.setVisibility(isGestures ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void styleSelectionRing(View card, boolean active) {
        if (card == null) return;
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(32f * d);
        ring.setColor(0xFF102238);
        if (active) {
            // Figma 4:1308: 6dp #02C9FF active selection ring
            ring.setStroke(Math.round(6f * d), 0xFF02C9FF);
        } else {
            ring.setStroke(Math.round(1.5f * d), 0x4460A5FF);
        }
        card.setBackground(ring);
    }

    @Override
    protected void onNextPressed() {
        mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY, mSelection);
        boolean hideHint = mHideGesturalHint != null && mHideGesturalHint.isChecked();
        LineageSettings.System.putIntForUser(getContentResolver(),
                LineageSettings.System.NAVIGATION_BAR_HINT, hideHint ? 0 : 1,
                UserHandle.USER_CURRENT);
        super.onNextPressed();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_navigation;
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
