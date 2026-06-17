package com.example.genzmusicapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class PulseScannerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pulsePath = new Path();
    private final long startTime = SystemClock.uptimeMillis();

    public PulseScannerView(Context context) {
        super(context);
    }

    public PulseScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height);
        float cx = width / 2f;
        float cy = height / 2f;
        float outer = size * 0.47f;
        float middle = size * 0.35f;
        float inner = size * 0.22f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(26, 99, 247, 255));
        canvas.drawCircle(cx, cy, outer, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setPathEffect(null);
        paint.setColor(Color.argb(88, 99, 247, 255));
        canvas.drawCircle(cx, cy, outer, paint);
        canvas.drawCircle(cx, cy, inner, paint);

        paint.setPathEffect(new DashPathEffect(new float[]{8f, 8f}, 0));
        paint.setColor(Color.argb(60, 218, 226, 253));
        canvas.drawCircle(cx, cy, middle, paint);
        paint.setPathEffect(null);

        drawHeart(canvas, cx, cy, size * 0.12f);
        drawOrbitDots(canvas, cx, cy, middle);
        postInvalidateOnAnimation();
    }

    private int currentBpm = 75; // Default resting HR
    private final Path heartPath = new Path();

    public void setBpm(int bpm) {
        if (bpm > 0) {
            this.currentBpm = bpm;
        }
    }

    private void drawHeart(Canvas canvas, float cx, float cy, float baseSize) {
        long elapsed = SystemClock.uptimeMillis() - startTime;
        
        // Calculate beat scale based on BPM
        long beatDurationMs = 60000L / Math.max(1, currentBpm); 
        float phase = (elapsed % beatDurationMs) / (float) beatDurationMs;
        
        // Custom easing for a heartbeat "lub-dub" double pulse
        // A single beat usually has two quick pulses, then a rest.
        float scale = 1.0f;
        if (phase < 0.15f) {
            scale = 1.0f + (float) Math.sin(phase / 0.15f * Math.PI) * 0.25f; // First pulse
        } else if (phase > 0.25f && phase < 0.4f) {
            scale = 1.0f + (float) Math.sin((phase - 0.25f) / 0.15f * Math.PI) * 0.15f; // Second smaller pulse
        }
        
        float finalSize = baseSize * scale;
        
        // Construct heart path
        // Standard geometric approximation of a heart
        heartPath.reset();
        float topY = cy - finalSize * 0.5f;
        float bottomY = cy + finalSize * 0.8f;
        float leftX = cx - finalSize;
        float rightX = cx + finalSize;
        
        heartPath.moveTo(cx, topY + finalSize * 0.3f);
        // Left lobe
        heartPath.cubicTo(
            cx, topY - finalSize * 0.5f,
            leftX - finalSize * 0.2f, topY - finalSize * 0.2f,
            leftX, cy
        );
        // Left bottom
        heartPath.cubicTo(
            leftX, cy + finalSize * 0.5f,
            cx - finalSize * 0.2f, bottomY - finalSize * 0.2f,
            cx, bottomY
        );
        // Right bottom
        heartPath.cubicTo(
            cx + finalSize * 0.2f, bottomY - finalSize * 0.2f,
            rightX, cy + finalSize * 0.5f,
            rightX, cy
        );
        // Right lobe
        heartPath.cubicTo(
            rightX + finalSize * 0.2f, topY - finalSize * 0.2f,
            cx, topY - finalSize * 0.5f,
            cx, topY + finalSize * 0.3f
        );
        heartPath.close();

        // Draw Heart Glow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(100, 255, 60, 120));
        paint.setShadowLayer(finalSize * 0.4f, 0, 0, Color.argb(150, 255, 40, 100));
        canvas.drawPath(heartPath, paint);
        
        // Draw Solid Heart Core
        paint.clearShadowLayer();
        paint.setColor(Color.argb(255, 255, 70, 130));
        canvas.drawPath(heartPath, paint);
    }


    private void drawOrbitDots(Canvas canvas, float cx, float cy, float radius) {
        long elapsed = SystemClock.uptimeMillis() - startTime;
        float angleA = (elapsed % 4600L) / 4600f * 360f;
        float angleB = 210f - ((elapsed % 6200L) / 6200f * 360f);
        drawDot(canvas, cx, cy, radius, angleA, Color.rgb(99, 247, 255), dp(6f));
        drawDot(canvas, cx, cy, radius, angleB, Color.rgb(255, 220, 248), dp(4.5f));
    }

    private void drawDot(Canvas canvas, float cx, float cy, float radius, float angle, int color, float dotRadius) {
        double radians = Math.toRadians(angle);
        float x = cx + (float) Math.cos(radians) * radius;
        float y = cy + (float) Math.sin(radians) * radius;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAlpha(230);
        canvas.drawCircle(x, y, dotRadius, paint);
        paint.setAlpha(42);
        canvas.drawCircle(x, y, dotRadius * 3.5f, paint);
        paint.setAlpha(255);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
