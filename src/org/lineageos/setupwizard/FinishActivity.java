/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import com.google.android.setupcompat.util.SystemBarHelper;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.util.SetupWizardUtils;
import org.lineageos.setupwizard.widget.LuminousBloomView;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private LuminousBloomView mBloomView;
    private View mContentContainer;
    private View mHeroStage;
    private View mStarBg;
    private SetupWizardApp mSetupWizardApp;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mIsFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        mSetupWizardApp = (SetupWizardApp) getApplication();
        mBloomView = findViewById(R.id.finish_bloom_view);
        mContentContainer = findViewById(R.id.finish_content_container);
        mHeroStage = findViewById(R.id.finish_hero_stage);
        mStarBg = findViewById(R.id.finish_star_bg);

        View glifLayout = findViewById(R.id.setup_wizard_layout);
        if (glifLayout != null) {
            View footer = glifLayout.findViewWithTag("suc_layout_footer");
            if (footer != null) {
                footer.setVisibility(View.GONE);
            }
        }

        Button startBtn = findViewById(R.id.start);
        if (startBtn != null) {
            OobeSpringHelper.attachPressSpring(startBtn);
            startBtn.setOnClickListener(view -> {
                OobeSpringHelper.vibrateSuccess(this);
                onNextPressed();
            });
        }

        setNextText(R.string.start);

        if (mContentContainer != null) {
            float d = getResources().getDisplayMetrics().density;
            mContentContainer.setAlpha(0f);
            mContentContainer.setTranslationY(18f * d);
            mContentContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(420)
                    .start();
        }
        if (mStarBg != null) {
            mStarBg.setScaleX(0.82f);
            mStarBg.setScaleY(0.82f);
            mStarBg.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(15f)
                    .setDuration(680)
                    .start();
        }
        if (mBloomView != null) {
            mBloomView.startBloomAnimation(700, null);
        }
    }

    private void disableNavigation() {
        hideNextButton();
        SystemBarHelper.hideSystemBars(getWindow());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
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

    @Override
    protected void onNextPressed() {
        startFinishSequence();
    }

    @Override
    public void onNavigateNext() {
        startFinishSequence();
    }

    @Override
    protected void onSystemBackPressed() {
        if (!mIsFinishing) {
            super.onSystemBackPressed();
        }
    }

    private void startFinishSequence() {
        if (mIsFinishing) {
            return;
        }
        mIsFinishing = true;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        disableNavigation();

        // Figma 4:1724 -> 4:1621: Scale-down / zoom transition into the home screen
        final View rootView = findViewById(R.id.setup_wizard_layout);
        if (rootView != null && rootView.isAttachedToWindow()) {
            if (mHeroStage != null) {
                mHeroStage.animate()
                        .scaleX(1.38f)
                        .scaleY(1.38f)
                        .setDuration(420)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
            rootView.animate()
                    .scaleX(0.86f)
                    .scaleY(0.86f)
                    .alpha(0f)
                    .setDuration(420)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> mHandler.post(() -> {
                        rootView.setVisibility(View.INVISIBLE);
                        completeSetup();
                    }))
                    .start();
            return;
        }
        completeSetup();
    }

    private void completeSetup() {
        handleWallpaperTheme(mSetupWizardApp);
        ensure120HzSmoothness(this);
        SetupWizardUtils.finishSetupWizard(FinishActivity.this);
    }

    private static void ensure120HzSmoothness(Context context) {
        try {
            Settings.System.putFloatForUser(context.getContentResolver(),
                    "min_refresh_rate", 120.0f, UserHandle.USER_CURRENT);
            Settings.System.putFloatForUser(context.getContentResolver(),
                    "peak_refresh_rate", 120.0f, UserHandle.USER_CURRENT);
        } catch (Throwable ignored) {}
    }

    private static void handleWallpaperTheme(Context context) {
        Bundle privacyData = SetupWizardApp.getSettingsBundle();
        if (privacyData != null && privacyData.containsKey("enable_dark_theme")) {
            Settings.Secure.putIntForUser(context.getContentResolver(),
                    "ui_night_mode",
                    privacyData.getBoolean("enable_dark_theme") ? 2 : 1,
                    UserHandle.USER_CURRENT);
        }
    }
}
