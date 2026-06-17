package com.example.genzmusicapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class StressChartView extends View {
    private String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private float[] values = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

    public void setData(float[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        invalidate();
    }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StressChartView(Context context) {
        super(context);
    }

    public StressChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float chartTop = dp(12f);
        float labelHeight = dp(32f);
        float chartHeight = height - labelHeight - chartTop;
        float base = chartTop + chartHeight;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(22, 218, 226, 253));
        canvas.drawLine(0, chartTop + chartHeight * 0.25f, width, chartTop + chartHeight * 0.25f, paint);
        canvas.drawLine(0, chartTop + chartHeight * 0.65f, width, chartTop + chartHeight * 0.65f, paint);
        canvas.drawLine(0, base, width, base, paint);

        float gap = dp(14f);
        float slot = width / values.length;
        float barWidth = Math.max(dp(7f), slot - gap);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < values.length; i++) {
            float center = slot * i + slot / 2f;
            float left = center - barWidth / 2f;
            float top = base - chartHeight * values[i];
            int startColor = i == 2 ? Color.rgb(221, 183, 255) : Color.rgb(99, 247, 255);
            paint.setShader(new LinearGradient(0, top, 0, base, startColor, Color.argb(28, 99, 247, 255), Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, top, left + barWidth, base, barWidth / 2f, barWidth / 2f, paint);
        }
        paint.setShader(null);

        paint.setColor(Color.argb(210, 218, 226, 253));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(13f));
        for (int i = 0; i < labels.length; i++) {
            canvas.drawText(labels[i], slot * i + slot / 2f, height - dp(8f), paint);
        }
        paint.setFakeBoldText(false);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
