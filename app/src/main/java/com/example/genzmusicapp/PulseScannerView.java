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

        drawPulseLine(canvas, cx, cy, size);
        drawOrbitDots(canvas, cx, cy, middle);
        postInvalidateOnAnimation();
    }

    private void drawPulseLine(Canvas canvas, float cx, float cy, float size) {
        float left = cx - size * 0.36f;
        float y = cy;
        float step = size * 0.055f;

        pulsePath.reset();
        pulsePath.moveTo(left, y);
        pulsePath.lineTo(left + step * 2f, y);
        pulsePath.lineTo(left + step * 2.6f, y - size * 0.08f);
        pulsePath.lineTo(left + step * 3.2f, y + size * 0.16f);
        pulsePath.lineTo(left + step * 3.8f, y - size * 0.06f);
        pulsePath.lineTo(left + step * 4.7f, y + size * 0.08f);
        pulsePath.lineTo(left + step * 5.3f, y);
        pulsePath.lineTo(left + step * 9.5f, y);
        pulsePath.lineTo(left + step * 10.2f, y - size * 0.08f);
        pulsePath.lineTo(left + step * 10.8f, y + size * 0.15f);
        pulsePath.lineTo(left + step * 11.5f, y);
        pulsePath.lineTo(left + step * 13.2f, y);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.rgb(99, 247, 255));
        canvas.drawPath(pulsePath, paint);
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
