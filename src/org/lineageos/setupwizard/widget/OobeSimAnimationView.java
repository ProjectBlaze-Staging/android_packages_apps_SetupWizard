/*
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Exact 1:1 port of the 2-stage Figma SIM Setup animation (Figma 4:1284 / 4:1300):
 *
 * - Keyframe 1 (Full Phone View - 299x408dp stage):
 *   Centered vertical smartphone (163.6x325.9dp, #4791FF outer blue backplate,
 *   3dp black inner bezel, white screen, 16dp top-center circular black selfie punch-hole camera,
 *   two left volume buttons #436286, and one right power button #436286).
 *
 * - Keyframe 2 (Figma I4:1300;331:1270 exact coordinates):
 *   The camera zooms into the left edge of the phone (backplate scales from 163.6x325.9 to
 *   294.43x586.50, i.e. 1.80x scale, positioned so its left bezel sits at the center of the card,
 *   with the lower volume button visible above the circle and the rounded bottom-left corner
 *   visible below the circle, plus soft white gradient fade masks at the top and bottom-right).
 *   A 143x143dp blue circular magnification ring (6dp #4791FF stroke) appears centered on the
 *   phone's left bezel, and a hollow blue SIM tray (66.58x60.48dp, #4791FF) slides smoothly out
 *   to the left from the phone's left bezel.
 *
 * - Cycle Timing (with 7.5-second pause at the end of every cycle as requested by user):
 *   Total loop duration = 12,000 ms (12.0 seconds):
 *   - 0.0s .. 1.0s: Show Keyframe 1 (Full phone view)
 *   - 1.0s .. 1.9s: Smooth spring-eased camera zoom into left edge + 143dp blue circle ring reveal
 *   - 1.7s .. 2.4s: Hollow blue SIM tray slides out to the left from the phone edge
 *   - 2.4s .. 4.5s: Hold Keyframe 2 (Zoomed left edge + circle ring + ejected SIM tray)
 *   - 4.5s .. 5.2s: Smooth return to Keyframe 1 (Full phone view)
 *   - 5.2s .. 12.0s: Rest/wait for ~7 seconds on Keyframe 1 before repeating!
 */
public class OobeSimAnimationView extends View {

    private static final float STAGE_W = 299f;
    private static final float STAGE_H = 408f;
    private static final long TOTAL_CYCLE_MS = 12000L; // ~4.5s animation + ~7.5s pause before repeating

    // Canonical Figma 3D hollow SIM tray path (84.76 x 77.49 viewport)
    private static final Path CANONICAL_SIM_TRAY_PATH = createCanonicalSimTrayPath();

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Path mClipPath = new Path();
    private final Path mLoupeClipPath = new Path();
    private final Path mTrayDrawPath = new Path();
    private final Matrix mTrayMatrix = new Matrix();

    private float mCycleProgress = 0f;
    private ValueAnimator mAnimator;

    public OobeSimAnimationView(Context context) {
        this(context, null);
    }

    public OobeSimAnimationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OobeSimAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    private static Path createCanonicalSimTrayPath() {
        Path p = new Path();
        // Outer contour of Figma 4:1289 / I4:1300;331:1278 SIM tray (84.76 x 77.49)
        p.moveTo(80.77f, 0f);
        p.cubicTo(82.97f, 0f, 84.76f, 1.79f, 84.76f, 4.00f);
        p.lineTo(84.76f, 73.49f);
        p.cubicTo(84.76f, 75.70f, 82.97f, 77.49f, 80.77f, 77.49f);
        p.cubicTo(78.56f, 77.49f, 76.77f, 75.70f, 76.77f, 73.49f);
        p.lineTo(76.77f, 70.95f);
        p.lineTo(15.63f, 70.95f);
        p.cubicTo(7.00f, 70.95f, 0f, 63.95f, 0f, 55.32f);
        p.lineTo(0f, 19.24f);
        p.cubicTo(0f, 9.94f, 7.54f, 2.41f, 16.84f, 2.41f);
        p.lineTo(77.10f, 2.41f);
        p.cubicTo(77.71f, 0.99f, 79.12f, 0f, 80.77f, 0f);
        p.close();

        // Inner hollow cutout with angled corner (bottom-right after 180° + scaleY(-1) transform)
        Path hole = new Path();
        hole.moveTo(16.84f, 12.94f);
        hole.cubicTo(13.36f, 12.94f, 10.53f, 15.76f, 10.53f, 19.24f);
        hole.lineTo(10.53f, 55.32f);
        hole.cubicTo(10.53f, 58.13f, 12.82f, 60.42f, 15.63f, 60.42f);
        hole.lineTo(72.44f, 60.42f);
        hole.lineTo(72.44f, 29.54f);
        hole.lineTo(54.96f, 12.94f);
        hole.close();

        p.setFillType(Path.FillType.EVEN_ODD);
        p.addPath(hole);
        return p;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoopAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    private void startLoopAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(TOTAL_CYCLE_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(anim -> {
            mCycleProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    private static float smoothStep(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float vw = getWidth();
        float vh = getHeight();
        if (vw <= 0 || vh <= 0) return;

        // Current elapsed time in seconds within the 12.0s cycle
        float tSec = mCycleProgress * (TOTAL_CYCLE_MS / 1000f);

        // Zoom progression (0 = Keyframe 1 Full Phone, 1 = Keyframe 2 Zoomed Left Edge)
        float zoomT;
        if (tSec < 0.9f) {
            zoomT = 0f;
        } else if (tSec < 1.85f) {
            zoomT = smoothStep((tSec - 0.9f) / 0.95f);
        } else if (tSec < 4.5f) {
            zoomT = 1f;
        } else if (tSec < 5.3f) {
            zoomT = 1f - smoothStep((tSec - 4.5f) / 0.8f);
        } else {
            // 5.3s .. 12.0s: ~6.7s stationary pause on Keyframe 1 before repeating
            zoomT = 0f;
        }

        // SIM tray slide-out progression (0 = inside phone, 1 = fully ejected to the left)
        float trayT;
        if (tSec < 1.55f) {
            trayT = 0f;
        } else if (tSec < 2.35f) {
            trayT = smoothStep((tSec - 1.55f) / 0.80f);
        } else if (tSec < 4.35f) {
            trayT = 1f;
        } else if (tSec < 4.95f) {
            trayT = 1f - smoothStep((tSec - 4.35f) / 0.60f);
        } else {
            trayT = 0f;
        }

        float scale = Math.min(vw / STAGE_W, vh / STAGE_H);
        float offsetX = (vw - STAGE_W * scale) * 0.5f;
        float offsetY = (vh - STAGE_H * scale) * 0.5f;

        int rootSave = canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        // Clip to the 299x408 rounded-17dp inner stage (Figma 4:1300)
        mClipPath.reset();
        mRect.set(0f, 0f, STAGE_W, STAGE_H);
        mClipPath.addRoundRect(mRect, 17f, 17f, Path.Direction.CW);
        canvas.clipPath(mClipPath);

        // Exact Figma 4:1300 -> I4:1300;331:1274 camera zoom math:
        // In Figma Keyframe 1: phone backplate is 163.626 x 325.942 at left = 68.0, top = 41.0.
        // In Figma Keyframe 2: zoomed phone backplate is 294.426 x 586.496 (exact 1.80x scale!)
        // positioned so its left edge is at x = 148.5 (center of the 299dp stage) and its bottom
        // edge is at y = 368.0 (just below the 143dp zoom circle!).
        float camScale = 1f + 0.80f * zoomT;
        float pivotX = 68.0f;
        float pivotY = 366.94f;
        float panX = (148.5f - 68.0f) * zoomT;
        float panY = 2.0f * zoomT;

        int camSave = canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(camScale, camScale, pivotX, pivotY);

        drawFigmaPhone(canvas);

        canvas.restoreToCount(camSave);

        // Keyframe 2: 143dp Blue Magnification Circle Ring + Hollow Blue SIM Tray Ejecting Left
        if (zoomT > 0.02f) {
            float loupeAlpha = smoothStep((zoomT - 0.02f) / 0.98f);
            // In Figma Keyframe 2 (media_1795080583625_7a2b22f6.jpg):
            // The 143x143dp circle is centered at x = 136.0, y = 232.0 (right over the phone's left bezel at x = 148.5)
            float loupeCx = 136.0f;
            float loupeCy = 232.0f;
            float loupeRadius = 71.5f * (0.82f + 0.18f * loupeAlpha); // 143dp diameter

            // Draw the hollow blue SIM tray (66.58 x 60.48dp, Figma I4:1300;331:1278) sliding out to the LEFT
            if (trayT > 0.01f) {
                int loupeClipSave = canvas.save();
                mLoupeClipPath.reset();
                mLoupeClipPath.addCircle(loupeCx, loupeCy, loupeRadius - 2.5f, Path.Direction.CW);
                canvas.clipPath(mLoupeClipPath);

                float trayW = 66.58f;
                float trayH = 60.48f;
                float phoneLeftBezelX = 148.5f;
                // When trayT = 1, the tray protrudes 58dp to the left of the phone's left bezel
                float trayLeftX = phoneLeftBezelX - (trayW - 6f) * trayT;
                float trayTopY = loupeCy - trayH * 0.5f;

                mTrayMatrix.reset();
                // Mirror BOTH horizontally and vertically (rotate-180 around center)
                // so the vertical tray bar with top/bottom lip sits on the LEFT and the hollow tray body
                // with its bottom-left angled corner sits between the bar and the phone's black bezel!
                mTrayMatrix.postScale(-1f, -1f, 84.76f * 0.5f, 77.49f * 0.5f);
                mTrayMatrix.postScale(trayW / 84.76f, trayH / 77.49f);
                mTrayMatrix.postTranslate(trayLeftX, trayTopY);

                mTrayDrawPath.set(CANONICAL_SIM_TRAY_PATH);
                mTrayDrawPath.transform(mTrayMatrix);

                // Clip at the phone's left outer edge so the tray emerges cleanly out of the phone frame
                canvas.clipRect(0f, 0f, phoneLeftBezelX + 1.5f, STAGE_H);

                mFillPaint.setStyle(Paint.Style.FILL);
                mFillPaint.setColor(0xFF4791FF);
                mFillPaint.setAlpha(Math.round(255 * loupeAlpha));
                canvas.drawPath(mTrayDrawPath, mFillPaint);
                mFillPaint.setAlpha(255);

                canvas.restoreToCount(loupeClipSave);
            }

            // Soft white fade masks at top and right edge of Keyframe 2 (Figma I4:1300;331:1488)
            int maskAlpha = Math.round(255 * zoomT);
            // Top fade mask so the top of the zoomed volume button/bezel fades softly into #F7FCFF
            mFadePaint.setShader(new LinearGradient(0f, 0f, 0f, 72f,
                    Color.argb(maskAlpha, 247, 252, 255),
                    Color.argb(0, 247, 252, 255),
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, STAGE_W, 72f, mFadePaint);

            // Right edge fade mask so the bottom-right horizontal bezel fades softly into #F7FCFF
            mFadePaint.setShader(new LinearGradient(STAGE_W - 78f, 0f, STAGE_W - 12f, 0f,
                    Color.argb(0, 247, 252, 255),
                    Color.argb(maskAlpha, 247, 252, 255),
                    Shader.TileMode.CLAMP));
            canvas.drawRect(STAGE_W - 78f, 0f, STAGE_W, STAGE_H, mFadePaint);
            mFadePaint.setShader(null);

            // 143dp Blue Magnification Circle Ring (Figma border-6 border-[#4791ff])
            mStrokePaint.setStrokeWidth(6.0f);
            mStrokePaint.setColor(0xFF4791FF);
            mStrokePaint.setAlpha(Math.round(255 * loupeAlpha));
            canvas.drawCircle(loupeCx, loupeCy, loupeRadius, mStrokePaint);
            mStrokePaint.setAlpha(255);
        }

        canvas.restoreToCount(rootSave);
    }

    private void drawFigmaPhone(Canvas canvas) {
        // 1. Left Volume Buttons & Right Power Button (Figma I4:1300;331:1251, #436286)
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(0xFF436286);

        // Upper volume button (left=65, top=98, width=3, height=44)
        mRect.set(64.5f, 98f, 69f, 142f);
        canvas.drawRoundRect(mRect, 2f, 2f, mFillPaint);

        // Lower volume button (left=65, top=145, width=3, height=44)
        mRect.set(64.5f, 145f, 69f, 189f);
        canvas.drawRoundRect(mRect, 2f, 2f, mFillPaint);

        // Right power button (left=231, top=156, width=3, height=59)
        mRect.set(230f, 156f, 234.5f, 215f);
        canvas.drawRoundRect(mRect, 2f, 2f, mFillPaint);

        // 2. Blue Outer Backplate (Figma I4:1300;331:1212: 163.626 x 325.942, radius 31.598, #4791FF)
        mRect.set(68.0f, 41.0f, 231.63f, 366.94f);
        mFillPaint.setColor(0xFF4791FF);
        canvas.drawRoundRect(mRect, 31.6f, 31.6f, mFillPaint);

        // 3. White Screen + 3dp Solid Black Inner Bezel (Figma I4:1300;331:1223: 155 x 318, radius 28)
        mRect.set(72.3f, 45.0f, 227.3f, 363.0f);
        mFillPaint.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(mRect, 28.0f, 28.0f, mFillPaint);

        mStrokePaint.setStrokeWidth(3.0f);
        mStrokePaint.setColor(0xFF000000);
        canvas.drawRoundRect(mRect, 28.0f, 28.0f, mStrokePaint);

        // 4. Top-Center Selfie Punch-Hole Camera (Figma I4:1300;331:1253: 16x16dp at top=54, centered)
        float camCx = 149.8f;
        float camCy = 62.0f;
        mFillPaint.setColor(0xFF05070A);
        canvas.drawCircle(camCx, camCy, 7.6f, mFillPaint);
        mFillPaint.setColor(0xFF283747);
        canvas.drawCircle(camCx - 1.8f, camCy - 1.8f, 2.1f, mFillPaint);
    }
}
