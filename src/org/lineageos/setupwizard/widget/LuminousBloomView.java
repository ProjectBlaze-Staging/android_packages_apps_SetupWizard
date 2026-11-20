/*
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * AmeOS 16 × BlazeAOSP OOBE ambient backdrop canvas.
 * Supports:
 * - Welcome Violet Mode (Figma 4:1190): Pure pitch-black (#000000 0%..26.08%) to deep rich purple/violet (#A50AFF 99.76%)
 * - Dark Cosmos Mode (Figma 4:1514): Deep obsidian cosmos with radial blue glow
 * - Light Sky Mode (Figma 4:1221): #F6FCFF -> #EDF9FF
 */
public class LuminousBloomView extends View {

    private final Paint mBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAmbientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mArcPath = new Path();
    private final Path mRimPath = new Path();

    private float mProgress = 1f;
    private float mPhase = 0f;
    private long mLastFrameMs = 0L;
    private boolean mDarkCosmosMode = true;
    private boolean mWelcomeVioletMode = false;
    private ValueAnimator mAnimator;
    private OnBloomCompleteListener mListener;

    public interface OnBloomCompleteListener {
        void onBloomComplete();
    }

    public LuminousBloomView(Context context) {
        this(context, null);
    }

    public LuminousBloomView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LuminousBloomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        float d = getResources().getDisplayMetrics().density;
        mArcStrokePaint.setStyle(Paint.Style.STROKE);
        mArcStrokePaint.setStrokeWidth(1.2f * d);
        mArcStrokePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setDarkCosmosMode(boolean darkCosmosMode) {
        mDarkCosmosMode = darkCosmosMode;
        invalidate();
    }

    public void setWelcomeVioletMode(boolean welcomeVioletMode) {
        mWelcomeVioletMode = welcomeVioletMode;
        invalidate();
    }

    public void startBloomAnimation(long duration, OnBloomCompleteListener listener) {
        mListener = listener;
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.onBloomComplete();
                }
            }
        });
        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = SystemClock.uptimeMillis();
        if (mLastFrameMs == 0L) mLastFrameMs = now;
        float dt = Math.min((now - mLastFrameMs) / 1000f, 0.05f);
        mLastFrameMs = now;
        mPhase += dt * 0.45f;

        float driftX = (float) Math.sin(mPhase) * (w * 0.025f);
        float driftY = (float) Math.cos(mPhase * 0.8f) * (h * 0.015f);
        float reveal = Math.min(1f, 0.35f + mProgress * 0.65f);

        if (mWelcomeVioletMode) {
            // Figma 4:1190 exact gradient: linear-gradient(179.39deg, #000000 26.08%, #A50AFF 99.76%)
            // Pure pitch black in top 26%, transitioning smoothly through dark plum to rich #A50AFF violet at bottom.
            // NO blue tint!
            mBasePaint.setShader(new LinearGradient(
                    0f, 0f, 0f, h,
                    new int[]{0xFF000000, 0xFF000000, 0xFF1F0235, 0xFF56058A, 0xFFA50AFF},
                    new float[]{0f, 0.2608f, 0.48f, 0.74f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, mBasePaint);
        } else if (mDarkCosmosMode) {
            // Figma 4:1514: Dark radial cosmos backdrop
            mBasePaint.setShader(new LinearGradient(
                    0f, 0f, 0f, h,
                    new int[]{0xFF020408, 0xFF050B18, 0xFF040914, 0xFF010204},
                    new float[]{0f, 0.42f, 0.78f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, mBasePaint);

            float upperCx = w * 0.50f + driftX;
            float upperCy = h * 0.42f + driftY;
            float upperR = Math.max(w, h) * 0.55f * reveal;
            mAmbientPaint.setShader(new RadialGradient(
                    upperCx, upperCy, Math.max(10f, upperR),
                    new int[]{0x44017CFE, 0x1F0050E0, 0x00020408},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(upperCx, upperCy, upperR, mAmbientPaint);
        } else {
            // Light Sky Canvas (#F6FCFF -> #EDF9FF)
            mBasePaint.setShader(new LinearGradient(
                    0f, 0f, 0f, h,
                    new int[]{0xFFF6FCFF, 0xFFF1FAFF, 0xFFEDF9FF},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, mBasePaint);
        }

        if (!mWelcomeVioletMode && isAttachedToWindow()) {
            postInvalidateOnAnimation();
        }
    }
}
