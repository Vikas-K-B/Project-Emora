import sys

with open('app/src/main/java/com/example/genzmusicapp/PulseScannerView.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_draw = """        drawPulseLine(canvas, cx, cy, size);
        drawOrbitDots(canvas, cx, cy, middle);
        postInvalidateOnAnimation();
    }"""

new_draw = """        drawHeart(canvas, cx, cy, size * 0.12f);
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
    }"""

content = content.replace(old_draw, new_draw)

# We also need to remove the unused drawPulseLine method to clean up
import re
content = re.sub(r'    private void drawPulseLine\(Canvas canvas, float cx, float cy, float size\) \{.*?\n    \}\n', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/genzmusicapp/PulseScannerView.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added drawHeart and setBpm to PulseScannerView")
