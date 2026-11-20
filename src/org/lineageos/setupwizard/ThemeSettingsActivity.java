/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.app.UiModeManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.widget.OobeLivePreviewView;

/**
 * 3-stage visual customization & showcase experience inside ThemeSettingsActivity:
 * - Stage 0 (Figma 4:1595): Choose your theme (3-phone lockscreen carousel: sky 1, sky 3, sky 4)
 * - Stage 1 (Figma 4:1261): Icon style [Visual] (4x2 live banner + Dimensional, Classic, Adaptive, Liquid Glass)
 * - Stage 2 (Figma 4:1381..4:1486): 5-Page Feature Showcase on #000000 backdrop
 */
public class ThemeSettingsActivity extends BaseSetupWizardActivity {

    private static final String[] SHOWCASE_TITLES = new String[]{
            "Meet Frosted Glass",
            "Redefined Animations",
            "Dynamic Island Live",
            "The Float Bar",
            "90+ New Clock Faces"
    };

    private static final String[] SHOWCASE_SUBTITLES = new String[]{
            "Real-time optical surface blur with delicate specular borders across the entire Control Center.",
            "Critically damped 120Hz Animation Kitchen spring physics in BlazeHouse Home Up Studio.",
            "Interactive capsule that morphs smoothly for media playback, calls, timers, and charging.",
            "Fluid bottom navigation capsule with chromatic edge refraction and active pill scrubbing.",
            "Over 90 customizable digital and analog lock screen clock designs inside BlazeHouse."
    };

    private static final String[] ICON_PACK_VALUES = new String[]{
            "org.blazeos.blazeicons",
            "none",
            "adaptive"
    };

    private View mRootLayout;
    private TextView mPageTitle;
    private TextView mPageSubtitle;
    private OobeLivePreviewView mLivePreview;

    private View mStageThemeControls;
    private View mStageIconControls;
    private View mShowcaseProgressContainer;
    private View[] mShowcaseBars;
    private View[] mThemeDots;
    private Button mBtnSky1;
    private Button mBtnSky3;
    private Button mBtnSky4;
    private View[] mIconStyleCards;

    private ImageButton mBackButton;
    private Button mNextButton;

    private int mCurrentStage = 0; // 0 = Theme Carousel, 1 = Icon Style, 2 = 5-Page Showcase
    private int mSelectedThemeIdx = 0; // 0 = sky 1 (Light Frosted), 1 = sky 3 (Liquid Glass), 2 = sky 4 (Dark Crystal)
    private int mSelectedIconStyleIdx = 0; // 0 = Dimensional, 1 = Classic, 2 = Adaptive, 3 = Liquid Glass
    private int mShowcasePage = 0; // 0..4

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentStage = savedInstanceState.getInt("theme_stage", 0);
            mSelectedThemeIdx = savedInstanceState.getInt("theme_idx", 0);
            mSelectedIconStyleIdx = savedInstanceState.getInt("icon_style_idx", 0);
            mShowcasePage = savedInstanceState.getInt("showcase_page", 0);
        }

        mRootLayout = findViewById(R.id.setup_wizard_layout);
        mPageTitle = findViewById(R.id.theme_page_title);
        mPageSubtitle = findViewById(R.id.theme_page_subtitle);
        mLivePreview = findViewById(R.id.oobe_live_preview);

        mStageThemeControls = findViewById(R.id.stage_theme_controls);
        mStageIconControls = findViewById(R.id.stage_icon_controls);
        mShowcaseProgressContainer = findViewById(R.id.showcase_progress_container);

        mShowcaseBars = new View[]{
                findViewById(R.id.showcase_bar_0),
                findViewById(R.id.showcase_bar_1),
                findViewById(R.id.showcase_bar_2),
                findViewById(R.id.showcase_bar_3),
                findViewById(R.id.showcase_bar_4)
        };

        mThemeDots = new View[]{
                findViewById(R.id.theme_dot_0),
                findViewById(R.id.theme_dot_1),
                findViewById(R.id.theme_dot_2)
        };

        mBtnSky1 = findViewById(R.id.btn_sky_1);
        mBtnSky3 = findViewById(R.id.btn_sky_3);
        mBtnSky4 = findViewById(R.id.btn_sky_4);

        mIconStyleCards = new View[]{
                findViewById(R.id.icon_style_dimensional),
                findViewById(R.id.icon_style_classic),
                findViewById(R.id.icon_style_adaptive)
        };

        mBackButton = findViewById(R.id.theme_back_button);
        mNextButton = findViewById(R.id.theme_next_button);

        if (mBtnSky1 != null) {
            OobeSpringHelper.attachPressSpring(mBtnSky1);
            mBtnSky1.setOnClickListener(v -> selectThemeOption(0));
        }
        if (mBtnSky3 != null) {
            OobeSpringHelper.attachPressSpring(mBtnSky3);
            mBtnSky3.setOnClickListener(v -> selectThemeOption(1));
        }
        if (mBtnSky4 != null) {
            OobeSpringHelper.attachPressSpring(mBtnSky4);
            mBtnSky4.setOnClickListener(v -> selectThemeOption(2));
        }

        if (mLivePreview != null) {
            final android.view.GestureDetector detector = new android.view.GestureDetector(this,
                    new android.view.GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(android.view.MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onSingleTapUp(android.view.MotionEvent e) {
                            if (mCurrentStage == 0) {
                                int w = mLivePreview.getWidth();
                                if (w > 0 && e.getX() < w * 0.34f) {
                                    selectThemeOption((mSelectedThemeIdx + 2) % 3);
                                } else {
                                    selectThemeOption((mSelectedThemeIdx + 1) % 3);
                                }
                            } else if (mCurrentStage == 1) {
                                selectIconStyleOption((mSelectedIconStyleIdx + 1) % 3);
                            } else if (mCurrentStage == 2 && mShowcasePage < 4) {
                                OobeSpringHelper.vibrateTick(ThemeSettingsActivity.this);
                                mShowcasePage++;
                                renderCurrentStage();
                            }
                            return true;
                        }

                        @Override
                        public boolean onFling(android.view.MotionEvent e1,
                                android.view.MotionEvent e2, float velocityX, float velocityY) {
                            if (e1 == null || e2 == null) return false;
                            float dx = e2.getX() - e1.getX();
                            if (Math.abs(dx) > 60f && Math.abs(dx) > Math.abs(e2.getY() - e1.getY())) {
                                if (mCurrentStage == 0) {
                                    selectThemeOption(dx < 0
                                            ? (mSelectedThemeIdx + 1) % 3
                                            : (mSelectedThemeIdx + 2) % 3);
                                    return true;
                                } else if (mCurrentStage == 1) {
                                    selectIconStyleOption(dx < 0
                                            ? (mSelectedIconStyleIdx + 1) % 3
                                            : (mSelectedIconStyleIdx + 2) % 3);
                                    return true;
                                } else if (mCurrentStage == 2) {
                                    if (dx < 0 && mShowcasePage < 4) {
                                        OobeSpringHelper.vibrateTick(ThemeSettingsActivity.this);
                                        mShowcasePage++;
                                        renderCurrentStage();
                                        return true;
                                    } else if (dx > 0 && mShowcasePage > 0) {
                                        OobeSpringHelper.vibrateTick(ThemeSettingsActivity.this);
                                        mShowcasePage--;
                                        renderCurrentStage();
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                    });
            mLivePreview.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
        }

        for (int i = 0; i < mIconStyleCards.length; i++) {
            final int idx = i;
            View card = mIconStyleCards[i];
            if (card != null) {
                OobeSpringHelper.attachPressSpring(card);
                card.setOnClickListener(v -> selectIconStyleOption(idx));
            }
        }

        if (mBackButton != null) {
            OobeSpringHelper.attachPressSpring(mBackButton);
            mBackButton.setOnClickListener(v -> handleStepBack());
        }

        if (mNextButton != null) {
            OobeSpringHelper.attachPressSpring(mNextButton);
            mNextButton.setOnClickListener(v -> handleStepForward());
        }

        // Default enable Dynamic Island
        applyDynamicIslandDefault();
        renderCurrentStage();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("theme_stage", mCurrentStage);
        outState.putInt("theme_idx", mSelectedThemeIdx);
        outState.putInt("icon_style_idx", mSelectedIconStyleIdx);
        outState.putInt("showcase_page", mShowcasePage);
    }

    @Override
    protected void onSystemBackPressed() {
        handleStepBack();
    }

    private void handleStepBack() {
        OobeSpringHelper.vibrateTick(this);
        if (mCurrentStage == 2) {
            if (mShowcasePage > 0) {
                mShowcasePage--;
                renderCurrentStage();
            } else {
                mCurrentStage = 1;
                renderCurrentStage();
            }
        } else if (mCurrentStage == 1) {
            mCurrentStage = 0;
            renderCurrentStage();
        } else {
            finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
        }
    }

    private void handleStepForward() {
        OobeSpringHelper.vibrateSuccess(this);
        if (mCurrentStage == 0) {
            applySelectedThemeToSystem();
            mCurrentStage = 1;
            renderCurrentStage();
        } else if (mCurrentStage == 1) {
            applySelectedIconStyleToSystem();
            mCurrentStage = 2;
            mShowcasePage = 0;
            renderCurrentStage();
        } else {
            if (mShowcasePage < 4) {
                mShowcasePage++;
                renderCurrentStage();
            } else {
                onNextPressed();
            }
        }
    }

    private void selectThemeOption(int index) {
        mSelectedThemeIdx = index;
        OobeSpringHelper.vibrateTick(this);
        applySelectedThemeToSystem();
        renderCurrentStage();
    }

    private void selectIconStyleOption(int index) {
        mSelectedIconStyleIdx = index;
        OobeSpringHelper.vibrateTick(this);
        applySelectedIconStyleToSystem();
        renderCurrentStage();
    }

    private void applySelectedThemeToSystem() {
        // 0 = Blaze Light (Light Frosted), 1 = Blaze Dark (Dark Obsidian), 2 = Dynamic Monet (Dark + Liquid Glass)
        boolean dark = (mSelectedThemeIdx != 0);
        int glassStyle = (mSelectedThemeIdx == 0) ? 1 : (mSelectedThemeIdx == 1 ? 3 : 0);
        try {
            SetupWizardApp.getSettingsBundle().putBoolean("enable_dark_theme", dark);
            Settings.Secure.putIntForUser(
                    getContentResolver(), "ui_night_mode", dark ? 2 : 1, UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(), "blaze_ui_glass_style", glassStyle, UserHandle.USER_CURRENT);
            SystemProperties.set("persist.sys.blaze.glass_style", String.valueOf(glassStyle));
            Settings.System.putIntForUser(
                    getContentResolver(),
                    "blaze_ui_glass_style_version",
                    (int) (System.currentTimeMillis() & 0x7FFFFFFF),
                    UserHandle.USER_CURRENT);
            UiModeManager uiModeManager = getSystemService(UiModeManager.class);
            if (uiModeManager != null) {
                uiModeManager.setNightMode(dark ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
            }
        } catch (Throwable ignored) {}
    }

    private void applySelectedIconStyleToSystem() {
        int clamped = Math.max(0, Math.min(ICON_PACK_VALUES.length - 1, mSelectedIconStyleIdx));
        String packValue = ICON_PACK_VALUES[clamped];
        boolean adaptive = (clamped == 2);
        try {
            Settings.Secure.putStringForUser(
                    getContentResolver(), "blaze_icon_pack", packValue, UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(), "themed_icon_enabled", adaptive ? 1 : 0, UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(), "themed_icons", adaptive ? 1 : 0, UserHandle.USER_CURRENT);
            String[] authorities = new String[]{
                    "com.android.launcher3.grid_control",
                    "com.google.android.apps.nexuslauncher.grid_control"
            };
            for (String auth : authorities) {
                try {
                    android.net.Uri uri = new android.net.Uri.Builder()
                            .scheme("content")
                            .authority(auth)
                            .appendPath("icon_themed")
                            .build();
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("boolean_value", adaptive);
                    getContentResolver().update(uri, values, null, null);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void applyDynamicIslandDefault() {
        try {
            Settings.Secure.putIntForUser(
                    getContentResolver(), "ax_dynamic_bar_enabled", 1, UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(), "ax_dynamic_bar_keyguard_enabled", 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(
                    getContentResolver(), "status_bar_show_dynamic_island", 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(
                    getContentResolver(), "status_bar_dynamic_island_media_controls", 1, UserHandle.USER_CURRENT);
        } catch (Throwable ignored) {}
    }

    private void renderCurrentStage() {
        float d = getResources().getDisplayMetrics().density;
        boolean isShowcase = (mCurrentStage == 2);

        if (mRootLayout != null) {
            if (isShowcase) {
                mRootLayout.setBackgroundColor(0xFF000000);
            } else {
                mRootLayout.setBackgroundResource(R.drawable.ameos_sky_canvas);
            }
        }

        try {
            int barBg = isShowcase ? 0xFF000000 : 0xFFF6FCFF;
            getWindow().setStatusBarColor(barBg);
            getWindow().setNavigationBarColor(isShowcase ? 0xFF000000 : 0xFFEDF9FF);
            WindowInsetsController wic = getWindow().getInsetsController();
            if (wic != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                wic.setSystemBarsAppearance(isShowcase ? 0 : mask, mask);
            }
        } catch (Throwable ignored) {}

        if (mStageThemeControls != null) {
            mStageThemeControls.setVisibility(mCurrentStage == 0 ? View.VISIBLE : View.GONE);
        }
        if (mStageIconControls != null) {
            mStageIconControls.setVisibility(mCurrentStage == 1 ? View.VISIBLE : View.GONE);
        }
        if (mShowcaseProgressContainer != null) {
            mShowcaseProgressContainer.setVisibility(isShowcase ? View.VISIBLE : View.GONE);
        }

        if (mCurrentStage == 0) {
            if (mPageTitle != null) {
                mPageTitle.setText("Choose your theme");
                mPageTitle.setTextColor(0xFF203A51);
            }
            if (mPageSubtitle != null) {
                mPageSubtitle.setText("Swipe or tap a lock screen preset to preview");
                mPageSubtitle.setTextColor(0xFF5B7894);
            }
            if (mNextButton != null) {
                mNextButton.setText("Apply This Theme");
            }
            if (mLivePreview != null) {
                mLivePreview.setPreviewMode(OobeLivePreviewView.MODE_THEME_CAROUSEL);
                mLivePreview.setThemeCarouselIndex(mSelectedThemeIdx);
            }
            styleSkyButtons();
        } else if (mCurrentStage == 1) {
            if (mPageTitle != null) {
                mPageTitle.setText("Icon style");
                mPageTitle.setTextColor(0xFF203A51);
            }
            if (mPageSubtitle != null) {
                mPageSubtitle.setText("Choose how app icons appear on your home screen");
                mPageSubtitle.setTextColor(0xFF5B7894);
            }
            if (mNextButton != null) {
                mNextButton.setText("Set up");
            }
            if (mLivePreview != null) {
                mLivePreview.setPreviewMode(OobeLivePreviewView.MODE_ICON_PREVIEW);
                mLivePreview.setIconStyleIndex(mSelectedIconStyleIdx);
            }
            for (int i = 0; i < mIconStyleCards.length; i++) {
                View card = mIconStyleCards[i];
                if (card != null) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(16f * d);
                    if (i == mSelectedIconStyleIdx) {
                        bg.setColor(0xFFE0F4FF);
                        bg.setStroke(Math.round(3f * d), 0xFF02C9FF);
                    } else {
                        bg.setColor(0xFFFFFFFF);
                        bg.setStroke(Math.round(1f * d), 0xFFD8ECFF);
                    }
                    card.setBackground(bg);
                }
            }
        } else {
            if (mPageTitle != null) {
                mPageTitle.setText(SHOWCASE_TITLES[mShowcasePage]);
                mPageTitle.setTextColor(Color.WHITE);
            }
            if (mPageSubtitle != null) {
                mPageSubtitle.setText(SHOWCASE_SUBTITLES[mShowcasePage]);
                mPageSubtitle.setTextColor(0xFFB0C4DE);
            }
            if (mNextButton != null) {
                mNextButton.setText(mShowcasePage == 4 ? "Done" : "Continue");
            }
            if (mLivePreview != null) {
                mLivePreview.setPreviewMode(OobeLivePreviewView.MODE_SHOWCASE);
                mLivePreview.setShowcasePageIndex(mShowcasePage);
            }
            for (int i = 0; i < mShowcaseBars.length; i++) {
                if (mShowcaseBars[i] != null) {
                    mShowcaseBars[i].setBackgroundColor(i <= mShowcasePage ? 0xFF02C9FF : 0x38FFFFFF);
                }
            }
        }
    }

    private void styleSkyButtons() {
        Button[] btns = new Button[]{mBtnSky1, mBtnSky3, mBtnSky4};
        for (int i = 0; i < btns.length; i++) {
            Button b = btns[i];
            if (b != null) {
                boolean active = (i == mSelectedThemeIdx);
                b.setBackgroundResource(active
                        ? R.drawable.ameos_btn_pill_filled
                        : R.drawable.ameos_btn_pill_outline);
                b.setTextColor(active ? Color.WHITE : 0xFF203A51);
            }
            if (mThemeDots != null && i < mThemeDots.length && mThemeDots[i] != null) {
                mThemeDots[i].setBackgroundColor(i == mSelectedThemeIdx ? 0xFF02C9FF : 0x5560A5FF);
            }
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_theme;
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
