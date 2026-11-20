/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.app.LocalePicker;
import com.android.internal.util.EmergencyAffordanceManager;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.util.SetupWizardUtils;
import org.lineageos.setupwizard.widget.LuminousBloomView;

import java.util.List;
import java.util.Locale;

public class WelcomeActivity extends SubBaseActivity {

    private LuminousBloomView mBloomView;
    private View mContentContainer;
    private View mSkyTransitionSheet;
    private TextView mCurrentLocaleText;
    private GestureDetector mSwipeUpDetector;
    private boolean mTransitioning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onSetupStart();
        setNextAllowed(true);
    }

    @Override
    protected void onStartSubactivity() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.welcome_activity;
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        View glifLayout = findViewById(R.id.setup_wizard_layout);
        if (glifLayout != null) {
            View footer = glifLayout.findViewWithTag("suc_layout_footer");
            if (footer != null) {
                footer.setVisibility(View.GONE);
            }
        }

        mBloomView = findViewById(R.id.luminous_bloom_view);
        if (mBloomView != null) {
            mBloomView.setWelcomeVioletMode(true);
        }
        mContentContainer = findViewById(R.id.welcome_content_container);
        mSkyTransitionSheet = findViewById(R.id.sky_transition_sheet);
        mCurrentLocaleText = findViewById(R.id.current_locale_text);

        Button startButton = findViewById(R.id.start);
        View emergRow = findViewById(R.id.emerg_row);
        View emergButton = findViewById(R.id.emerg_button);
        View accessibilityRow = findViewById(R.id.accessibility_row);
        View accessibilityButton = findViewById(R.id.accessibility_button);
        View langCard = findViewById(R.id.language_selector_card);
        View brandLogo = findViewById(R.id.brand_logo);

        updateDisplayedLocale();

        if (startButton != null) {
            OobeSpringHelper.attachPressSpring(startButton);
            startButton.setOnClickListener(view -> triggerSkySheetAndProceed());
        }

        mSwipeUpDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null && (e1.getY() - e2.getY() > 80f) && velocityY < -250f) {
                    triggerSkySheetAndProceed();
                    return true;
                }
                return false;
            }
        });

        if (glifLayout != null) {
            glifLayout.setOnTouchListener((v, event) -> {
                mSwipeUpDetector.onTouchEvent(event);
                return false;
            });
        }

        View.OnClickListener localeClickListener = v -> {
            OobeSpringHelper.vibrateTick(this);
            showQuickLocaleDialog();
        };

        if (langCard != null) {
            OobeSpringHelper.attachPressSpring(langCard);
            langCard.setOnClickListener(localeClickListener);
        }
        if (brandLogo != null) {
            brandLogo.setOnClickListener(localeClickListener);
        }

        View a11yTarget = accessibilityRow != null ? accessibilityRow : accessibilityButton;
        if (a11yTarget != null) {
            OobeSpringHelper.attachPressSpring(a11yTarget);
            a11yTarget.setOnClickListener(view -> {
                OobeSpringHelper.vibrateTick(this);
                try {
                    Intent intent = new Intent("com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS");
                    if (intent.resolveActivity(getPackageManager()) == null) {
                        intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    }
                    startSubactivity(intent);
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception ignored) {}
                }
            });
        }

        View emergTarget = emergRow != null ? emergRow : emergButton;
        if (emergTarget != null) {
            if (SetupWizardUtils.hasTelephony(this)) {
                emergTarget.setVisibility(View.VISIBLE);
                OobeSpringHelper.attachPressSpring(emergTarget);
                emergTarget.setOnClickListener(view -> {
                    OobeSpringHelper.vibrateTick(this);
                    startSubactivity(new Intent(SetupWizardApp.ACTION_EMERGENCY_DIAL));
                });

                if (new EmergencyAffordanceManager(this).needsEmergencyAffordance()) {
                    emergTarget.setOnLongClickListener(view -> {
                        new EmergencyAffordanceManager(WelcomeActivity.this).performEmergencyCall();
                        return true;
                    });
                }
            } else {
                emergTarget.setVisibility(View.GONE);
            }
        }

        runEntranceChoreography();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTransitioning = false;
        if (mSkyTransitionSheet != null) {
            mSkyTransitionSheet.setVisibility(View.GONE);
            mSkyTransitionSheet.setAlpha(0f);
        }
        if (mContentContainer != null) {
            mContentContainer.setAlpha(1f);
            mContentContainer.setTranslationY(0f);
        }
    }

    private void triggerSkySheetAndProceed() {
        if (mTransitioning) return;
        mTransitioning = true;
        OobeSpringHelper.vibrateSuccess(this);

        if (mSkyTransitionSheet != null && mContentContainer != null) {
            int h = mContentContainer.getHeight() > 0 ? mContentContainer.getHeight() : 1800;
            mSkyTransitionSheet.setVisibility(View.VISIBLE);
            mSkyTransitionSheet.setAlpha(1f);
            mSkyTransitionSheet.setTranslationY(h);
            mContentContainer.animate()
                    .alpha(0f)
                    .translationY(-h * 0.16f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
            mSkyTransitionSheet.animate()
                    .translationY(0f)
                    .setDuration(340)
                    .setInterpolator(OobeSpringHelper.LAMINAR_SPRING_INTERPOLATOR)
                    .withEndAction(this::onNextPressed)
                    .start();
        } else {
            onNextPressed();
        }
    }

    private void updateDisplayedLocale() {
        if (mCurrentLocaleText == null) return;
        Locale current = Locale.getDefault();
        String display = current.getDisplayName(current);
        if (display != null && !display.isEmpty()) {
            display = display.substring(0, 1).toUpperCase(current) + display.substring(1);
            mCurrentLocaleText.setText(display);
        }
    }

    private void showQuickLocaleDialog() {
        try {
            final List<LocalePicker.LocaleInfo> locales = LocalePicker.getAllAssetLocales(this, false);
            if (locales == null || locales.isEmpty()) return;

            final CharSequence[] labels = new CharSequence[locales.size()];
            Locale current = Locale.getDefault();
            int checkedIndex = 0;

            for (int i = 0; i < locales.size(); i++) {
                LocalePicker.LocaleInfo info = locales.get(i);
                labels[i] = info.getLabel();
                if (info.getLocale() != null && info.getLocale().getLanguage().equals(current.getLanguage())) {
                    checkedIndex = i;
                }
            }

            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.setup_locale)
                    .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                        LocalePicker.LocaleInfo chosen = locales.get(which);
                        if (chosen != null && chosen.getLocale() != null) {
                            OobeSpringHelper.vibrateSuccess(this);
                            LocalePicker.updateLocale(chosen.getLocale());
                            updateDisplayedLocale();
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } catch (Throwable ignored) {}
    }

    private void runEntranceChoreography() {
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
        if (mBloomView != null) {
            mBloomView.startBloomAnimation(700, null);
        }
    }

    @Override
    protected void onSystemBackPressed() {
        // First screen of OOBE: ignore back press.
    }

    @Override
    public void onBackPressed() {
    }
}
