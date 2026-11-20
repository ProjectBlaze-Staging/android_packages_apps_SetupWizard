/*
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.lineageos.setupwizard.R;

/**
 * Real-screenshot interactive preview canvas for ThemeSettingsActivity:
 * - Stage 1 (MODE_THEME_CAROUSEL): 3-phone carousel displaying real device screenshots
 *   (Blaze Light, Blaze Dark, Dynamic Monet).
 * - Stage 2 (MODE_ICON_PREVIEW): Real home screen & dock icon preview for the 3 authentic
 *   icon architectures (BlazeIcons Modern, Classic Lineage, Adaptive Material You).
 * - Stage 3 (MODE_SHOWCASE): 5-Page Feature Showcase displaying real device screenshots
 *   (Control Center Frosted Glass, 120Hz Animation Kitchen, Dynamic Island Media,
 *   Liquid Glass Float Bar, and 90+ Custom Clock Faces).
 */
public class OobeLivePreviewView extends View {

    public static final int MODE_THEME_CAROUSEL = 0;
    public static final int MODE_ICON_PREVIEW = 1;
    public static final int MODE_SHOWCASE = 2;

    private static final int[] THEME_RES_IDS = new int[]{
            R.drawable.oobe_real_theme_light,
            R.drawable.oobe_real_theme_dark,
            R.drawable.oobe_real_theme_monet
    };

    private static final String[] THEME_LABELS = new String[]{
            "Blaze Light",
            "Blaze Dark",
            "Dynamic Monet"
    };

    private static final int[] ICON_CROP_RES_IDS = new int[]{
            R.drawable.oobe_real_icons_blaze,
            R.drawable.oobe_real_icons_classic,
            R.drawable.oobe_real_icons_adaptive
    };

    private static final int[] ICON_HOME_RES_IDS = new int[]{
            R.drawable.oobe_real_home_blaze,
            R.drawable.oobe_real_home_classic,
            R.drawable.oobe_real_home_adaptive
    };

    private static final String[] ICON_LABELS = new String[]{
            "BlazeIcons • Modern Squircle",
            "Classic • LineageOS Stock",
            "Adaptive • Material You"
    };

    private static final int[] SHOWCASE_RES_IDS = new int[]{
            R.drawable.oobe_real_showcase_0,
            R.drawable.oobe_real_showcase_1,
            R.drawable.oobe_real_showcase_2,
            R.drawable.oobe_real_showcase_3,
            R.drawable.oobe_real_showcase_4
    };

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mSrcRect = new Rect();
    private final RectF mDstRect = new RectF();
    private final Path mClipPath = new Path();

    private final Bitmap[] mThemeBitmaps = new Bitmap[3];
    private final Bitmap[] mIconCropBitmaps = new Bitmap[3];
    private final Bitmap[] mIconHomeBitmaps = new Bitmap[3];
    private final Bitmap[] mShowcaseBitmaps = new Bitmap[5];

    private int mPreviewMode = MODE_THEME_CAROUSEL;
    private int mThemeCarouselIndex = 0; // 0..2
    private int mIconStyleIndex = 0; // 0..2
    private int mShowcasePageIndex = 0; // 0..4
    private float mTransitionProgress = 1f;

    public OobeLivePreviewView(Context context) {
        this(context, null);
    }

    public OobeLivePreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OobeLivePreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setPreviewMode(int mode) {
        if (mPreviewMode != mode) {
            mPreviewMode = mode;
            animateTransition();
        } else {
            invalidate();
        }
    }

    public void setThemeCarouselIndex(int index) {
        mThemeCarouselIndex = Math.max(0, Math.min(2, index));
        animateTransition();
    }

    public void setIconStyleIndex(int index) {
        mIconStyleIndex = Math.max(0, Math.min(2, index));
        animateTransition();
    }

    public void setShowcasePageIndex(int index) {
        mShowcasePageIndex = Math.max(0, Math.min(4, index));
        animateTransition();
    }

    public void updateState(boolean darkMode, int glassStyle, boolean dynamicIslandEnabled) {
        animateTransition();
    }

    private void animateTransition() {
        ValueAnimator anim = ValueAnimator.ofFloat(0.90f, 1f);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            mTransitionProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private Bitmap getCachedBitmap(Bitmap[] cache, int[] resIds, int index) {
        if (index < 0 || index >= cache.length) return null;
        if (cache[index] == null || cache[index].isRecycled()) {
            try {
                cache[index] = BitmapFactory.decodeResource(getResources(), resIds[index]);
            } catch (Throwable ignored) {}
        }
        return cache[index];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float d = getResources().getDisplayMetrics().density;

        if (mPreviewMode == MODE_THEME_CAROUSEL) {
            drawThreePhoneCarousel(canvas, w, h, d);
        } else if (mPreviewMode == MODE_ICON_PREVIEW) {
            drawRealIconStylePreview(canvas, w, h, d);
        } else {
            drawRealShowcaseScreenshot(canvas, w, h, d);
        }
    }

    private void drawThreePhoneCarousel(Canvas canvas, float w, float h, float d) {
        float maxH = Math.min(h - 14f * d, 320f * d);
        float centerW = Math.min(w * 0.48f, maxH * (440f / 978f));
        float centerH = centerW * (978f / 440f);
        if (centerH > maxH) {
            centerH = maxH;
            centerW = centerH * (440f / 978f);
        }

        float sideW = centerW * 0.80f;
        float sideH = centerH * 0.80f;
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        int leftIdx = (mThemeCarouselIndex + 2) % 3;
        int rightIdx = (mThemeCarouselIndex + 1) % 3;

        RectF leftRect = new RectF(
                cx - centerW * 0.52f - sideW * 0.62f,
                cy - sideH * 0.5f,
                cx - centerW * 0.52f + sideW * 0.38f,
                cy + sideH * 0.5f);
        drawPhoneScreenshotCard(canvas, leftRect,
                getCachedBitmap(mThemeBitmaps, THEME_RES_IDS, leftIdx),
                THEME_LABELS[leftIdx], false, false, d);

        RectF rightRect = new RectF(
                cx + centerW * 0.52f - sideW * 0.38f,
                cy - sideH * 0.5f,
                cx + centerW * 0.52f + sideW * 0.62f,
                cy + sideH * 0.5f);
        drawPhoneScreenshotCard(canvas, rightRect,
                getCachedBitmap(mThemeBitmaps, THEME_RES_IDS, rightIdx),
                THEME_LABELS[rightIdx], false, false, d);

        float cw = centerW * mTransitionProgress;
        float ch = centerH * mTransitionProgress;
        RectF mainRect = new RectF(cx - cw * 0.5f, cy - ch * 0.5f, cx + cw * 0.5f, cy + ch * 0.5f);
        drawPhoneScreenshotCard(canvas, mainRect,
                getCachedBitmap(mThemeBitmaps, THEME_RES_IDS, mThemeCarouselIndex),
                THEME_LABELS[mThemeCarouselIndex], true, false, d);
    }

    private void drawRealIconStylePreview(Canvas canvas, float w, float h, float d) {
        int idx = Math.max(0, Math.min(2, mIconStyleIndex));
        Bitmap cropBmp = getCachedBitmap(mIconCropBitmaps, ICON_CROP_RES_IDS, idx);
        Bitmap homeBmp = getCachedBitmap(mIconHomeBitmaps, ICON_HOME_RES_IDS, idx);

        float cardW = Math.min(w - 8f * d, 340f * d) * mTransitionProgress;
        float cardH = Math.min(h - 12f * d, cardW * (498f / 640f));
        if (cardH * (640f / 498f) < cardW) {
            cardW = cardH * (640f / 498f);
        }
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        mDstRect.set(cx - cardW * 0.5f, cy - cardH * 0.5f, cx + cardW * 0.5f, cy + cardH * 0.5f);
        float radius = 26f * d;

        // Outer soft shadow
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0x24001A36);
        RectF shadowRect = new RectF(mDstRect.left, mDstRect.top + 4f * d,
                mDstRect.right, mDstRect.bottom + 4f * d);
        canvas.drawRoundRect(shadowRect, radius, radius, mPaint);

        int save = canvas.save();
        mClipPath.reset();
        mClipPath.addRoundRect(mDstRect, radius, radius, Path.Direction.CW);
        canvas.clipPath(mClipPath);

        Bitmap target = cropBmp != null ? cropBmp : homeBmp;
        if (target != null && !target.isRecycled()) {
            drawCenterCropBitmap(canvas, target, mDstRect);
        } else {
            mPaint.setColor(0xFF0F172A);
            canvas.drawRect(mDstRect, mPaint);
        }

        // Top pill badge showing current real icon style name
        float pillW = Math.min(cardW * 0.78f, 220f * d);
        float pillH = 26f * d;
        RectF badgeRect = new RectF(cx - pillW * 0.5f, mDstRect.top + 10f * d,
                cx + pillW * 0.5f, mDstRect.top + 10f * d + pillH);
        mPaint.setColor(0xCC0B1324);
        canvas.drawRoundRect(badgeRect, pillH * 0.5f, pillH * 0.5f, mPaint);
        mStrokePaint.setStrokeWidth(1.2f * d);
        mStrokePaint.setColor(0x8802C9FF);
        canvas.drawRoundRect(badgeRect, pillH * 0.5f, pillH * 0.5f, mStrokePaint);

        mTextPaint.setTextSize(11.5f * d);
        mTextPaint.setColor(Color.WHITE);
        canvas.drawText(ICON_LABELS[idx], cx, badgeRect.centerY() + 4f * d, mTextPaint);

        canvas.restoreToCount(save);

        // Specular cyan selection border
        mStrokePaint.setStrokeWidth(3.2f * d);
        mStrokePaint.setColor(0xFF02C9FF);
        canvas.drawRoundRect(mDstRect, radius, radius, mStrokePaint);
    }

    private void drawRealShowcaseScreenshot(Canvas canvas, float w, float h, float d) {
        int page = Math.max(0, Math.min(4, mShowcasePageIndex));
        Bitmap bmp = getCachedBitmap(mShowcaseBitmaps, SHOWCASE_RES_IDS, page);

        float maxH = Math.min(h - 10f * d, 360f * d);
        float phoneW = Math.min(w * 0.64f, maxH * (500f / 1111f));
        float phoneH = phoneW * (1111f / 500f);
        if (phoneH > maxH) {
            phoneH = maxH;
            phoneW = phoneH * (500f / 1111f);
        }

        float cw = phoneW * mTransitionProgress;
        float ch = phoneH * mTransitionProgress;
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        // Ambient cyan/blue glow behind phone frame on the dark showcase canvas
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0x2202C9FF);
        canvas.drawCircle(cx, cy, Math.max(cw, ch) * 0.54f, mPaint);

        RectF rect = new RectF(cx - cw * 0.5f, cy - ch * 0.5f, cx + cw * 0.5f, cy + ch * 0.5f);
        drawPhoneScreenshotCard(canvas, rect, bmp, null, true, true, d);
    }

    private void drawPhoneScreenshotCard(Canvas canvas, RectF rect, Bitmap bmp,
            String bottomLabel, boolean active, boolean darkCanvas, float d) {
        float radius = (active ? 24f : 18f) * d;

        // Shadow / Bezel backing
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(active ? 0x33001E42 : 0x1A001E42);
        RectF shadow = new RectF(rect.left, rect.top + 4f * d, rect.right, rect.bottom + 4f * d);
        canvas.drawRoundRect(shadow, radius, radius, mPaint);

        int save = canvas.save();
        mClipPath.reset();
        mClipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(mClipPath);

        if (bmp != null && !bmp.isRecycled()) {
            mPaint.setAlpha(active ? 255 : 145);
            drawCenterCropBitmap(canvas, bmp, rect);
            mPaint.setAlpha(255);
        } else {
            mPaint.setColor(0xFF1E293B);
            canvas.drawRect(rect, mPaint);
        }

        if (!active) {
            mPaint.setColor(0x44EDF9FF);
            canvas.drawRect(rect, mPaint);
        }

        if (bottomLabel != null && active) {
            float gradH = 38f * d;
            mPaint.setShader(new LinearGradient(0f, rect.bottom - gradH, 0f, rect.bottom,
                    0x00000000, 0xCC060D1A, Shader.TileMode.CLAMP));
            canvas.drawRect(rect.left, rect.bottom - gradH, rect.right, rect.bottom, mPaint);
            mPaint.setShader(null);

            mTextPaint.setTextSize(11f * d);
            mTextPaint.setColor(Color.WHITE);
            canvas.drawText(bottomLabel, rect.centerX(), rect.bottom - 10f * d, mTextPaint);
        }

        canvas.restoreToCount(save);

        // Phone frame border
        mStrokePaint.setStrokeWidth((active ? 3.2f : 1.5f) * d);
        mStrokePaint.setColor(active ? 0xFF02C9FF : (darkCanvas ? 0x55FFFFFF : 0x8893C5FD));
        canvas.drawRoundRect(rect, radius, radius, mStrokePaint);
    }

    private void drawCenterCropBitmap(Canvas canvas, Bitmap bmp, RectF dst) {
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();
        if (bw <= 0 || bh <= 0) return;

        float dstAspect = dst.width() / dst.height();
        float bmpAspect = (float) bw / (float) bh;

        if (bmpAspect > dstAspect) {
            int cropW = Math.round(bh * dstAspect);
            int x0 = (bw - cropW) / 2;
            mSrcRect.set(x0, 0, x0 + cropW, bh);
        } else {
            int cropH = Math.round(bw / dstAspect);
            int y0 = (bh - cropH) / 2;
            mSrcRect.set(0, y0, bw, y0 + cropH);
        }
        canvas.drawBitmap(bmp, mSrcRect, dst, mPaint);
    }
}
