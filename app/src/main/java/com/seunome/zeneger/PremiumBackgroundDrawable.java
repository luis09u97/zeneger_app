package com.seunome.zeneger;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

/** Animated blue dashboard atmosphere shared by both color modes. */
public final class PremiumBackgroundDrawable extends Drawable implements Animatable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint geometryPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path geometryPath = new Path();
    private final Matrix glowMatrix = new Matrix();
    private final boolean darkMode;
    private final RadialGradient primaryGlow;
    private final RadialGradient cyanGlow;
    private final RadialGradient deepBlueGlow;
    private ValueAnimator animator;
    private Shader baseShader;
    private float progress;
    private int drawableAlpha = 255;

    public PremiumBackgroundDrawable(boolean darkMode) {
        this.darkMode = darkMode;
        primaryGlow = createGlow(darkMode ? 0x7A087CFF : 0x4D3E9BFF);
        cyanGlow = createGlow(darkMode ? 0x4327C8FF : 0x3339BFFF);
        deepBlueGlow = createGlow(darkMode ? 0x5C003B9C : 0x244E8DFF);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int[] colors = darkMode
                ? new int[]{0xFF010612, 0xFF03122A, 0xFF06234A, 0xFF020817}
                : new int[]{0xFFF9FCFF, 0xFFF0F6FF, 0xFFE5F0FF, 0xFFF7FAFF};
        baseShader = new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                colors, new float[]{0f, 0.34f, 0.72f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        paint.setAlpha(drawableAlpha);
        paint.setShader(baseShader);
        canvas.drawRect(bounds, paint);

        double phase = progress * Math.PI * 2.0;
        float max = Math.max(bounds.width(), bounds.height());
        drawGlow(canvas,
                bounds.left + bounds.width() * (0.78f + 0.06f * (float) Math.sin(phase)),
                bounds.top + bounds.height() * (0.16f + 0.05f * (float) Math.cos(phase)),
                max * 0.52f, primaryGlow);
        drawGlow(canvas,
                bounds.left + bounds.width() * (0.08f + 0.07f * (float) Math.cos(phase * 0.64)),
                bounds.top + bounds.height() * (0.62f + 0.08f * (float) Math.sin(phase * 0.72)),
                max * 0.42f, cyanGlow);
        drawGlow(canvas,
                bounds.left + bounds.width() * (0.60f + 0.08f * (float) Math.cos(phase * 0.48)),
                bounds.top + bounds.height() * (0.96f + 0.04f * (float) Math.sin(phase * 0.58)),
                max * 0.38f, deepBlueGlow);

        drawDiagonalStripes(canvas, bounds);
        drawGeometry(canvas, bounds, phase);
        drawParticles(canvas, bounds, phase);
    }

    private static RadialGradient createGlow(int color) {
        return new RadialGradient(0f, 0f, 1f,
                new int[]{color, Color.TRANSPARENT}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
    }

    private void drawGlow(Canvas canvas, float cx, float cy, float radius, RadialGradient shader) {
        glowMatrix.reset();
        glowMatrix.setScale(radius, radius);
        glowMatrix.postTranslate(cx, cy);
        shader.setLocalMatrix(glowMatrix);
        paint.setShader(shader);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    private void drawDiagonalStripes(Canvas canvas, Rect bounds) {
        float width = bounds.width();
        float drift = ((progress * 2f) % 1f) * width * 0.46f;

        geometryPaint.setStyle(Paint.Style.STROKE);
        geometryPaint.setStrokeWidth(Math.max(14f, width * 0.05f));
        geometryPaint.setColor(darkMode ? 0x382D8DFF : 0x283D91F5);
        for (int i = -5; i < 10; i++) {
            float startX = bounds.left + i * width * 0.23f + drift;
            canvas.drawLine(startX, bounds.bottom, startX + width * 0.78f, bounds.top,
                    geometryPaint);
        }

        geometryPaint.setStrokeWidth(Math.max(1f, width / 320f));
        geometryPaint.setColor(darkMode ? 0x703E9BFF : 0x55457FD2);
        for (int i = -3; i < 12; i++) {
            float startX = bounds.left + i * width * 0.16f - drift * 0.45f;
            canvas.drawLine(startX, bounds.bottom, startX + width * 0.62f, bounds.top,
                    geometryPaint);
        }
        geometryPaint.setStyle(Paint.Style.FILL);
    }

    private void drawGeometry(Canvas canvas, Rect bounds, double phase) {
        geometryPaint.setColor(darkMode ? 0x24155FC4 : 0x182879D7);
        polygon(canvas, bounds.width() * 0.06f, bounds.height() * 0.22f,
                bounds.width() * 0.42f, bounds.height() * 0.34f,
                bounds.width() * 0.31f, bounds.height() * 0.55f,
                bounds.width() * 0.02f, bounds.height() * 0.45f,
                (float) Math.sin(phase) * bounds.width() * 0.025f);

        geometryPaint.setColor(darkMode ? 0x2B0876DE : 0x142B87E8);
        polygon(canvas, bounds.width() * 0.58f, bounds.height() * 0.33f,
                bounds.width() * 0.96f, bounds.height() * 0.45f,
                bounds.width() * 0.82f, bounds.height() * 0.69f,
                bounds.width() * 0.48f, bounds.height() * 0.57f,
                (float) Math.cos(phase * 0.72) * bounds.width() * 0.03f);
    }

    private void polygon(Canvas canvas, float x1, float y1, float x2, float y2,
                         float x3, float y3, float x4, float y4, float drift) {
        geometryPath.reset();
        geometryPath.moveTo(x1 + drift, y1);
        geometryPath.lineTo(x2 + drift, y2);
        geometryPath.lineTo(x3 + drift, y3);
        geometryPath.lineTo(x4 + drift, y4);
        geometryPath.close();
        canvas.drawPath(geometryPath, geometryPaint);
    }

    private void drawParticles(Canvas canvas, Rect bounds, double phase) {
        particlePaint.setColor(darkMode ? 0xB982C7FF : 0x99629BE0);
        for (int i = 0; i < 18; i++) {
            float seed = (i * 0.6180339f) % 1f;
            float x = bounds.left + ((seed + progress * (0.015f + i * 0.001f)) % 1f)
                    * bounds.width();
            float wave = (float) Math.sin(phase * (0.32 + i * 0.018) + i * 1.7);
            float y = bounds.top + (((i * 0.137f) % 1f) * bounds.height())
                    + wave * bounds.height() * 0.025f;
            float radius = Math.max(1.2f, bounds.width() / 360f)
                    * (0.8f + (i % 3) * 0.38f);
            particlePaint.setAlpha(55 + (i % 4) * 24);
            canvas.drawCircle(x, y, radius, particlePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        drawableAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        geometryPaint.setColorFilter(colorFilter);
        particlePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void start() {
        if (isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(18000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> {
            progress = (float) value.getAnimatedValue();
            invalidateSelf();
        });
        animator.start();
    }

    @Override
    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    public boolean isRunning() {
        return animator != null && animator.isRunning();
    }
}
