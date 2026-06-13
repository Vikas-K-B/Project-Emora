package com.example.genzmusicapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveBarsView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] heights = {0.28f, 0.58f, 0.82f, 1.0f, 0.64f, 0.38f};

    public WaveBarsView(Context context) {
        super(context);
        init();
    }

    public WaveBarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(Color.rgb(99, 247, 255));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float gap = width * 0.035f;
        float barWidth = (width - (gap * (heights.length - 1))) / heights.length;

        for (int i = 0; i < heights.length; i++) {
            int alpha = i == 0 || i == heights.length - 1 ? 95 : 210;
            paint.setAlpha(alpha);
            float left = i * (barWidth + gap);
            float barHeight = height * heights[i];
            float top = height - barHeight;
            canvas.drawRoundRect(left, top, left + barWidth, height, barWidth / 2f, barWidth / 2f, paint);
        }
    }
}
