package com.example.genzmusicapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

public class BiometricClassifier {

    private Interpreter tflite;
    
    // Mood Classes
    // 0: Resting (Baseline/Meditation)
    // 1: Stressed
    // 2: Energetic (Amusement / High Activity)
    private static final String[] MOOD_CLASSES = {"Resting", "Stressed", "Energetic"};

    // Constants for buffers assuming ~1 update per second
    private static final int BUFFER_1M_SIZE = 60;
    private static final int BUFFER_5M_SIZE = 300;
    
    // History buffers
    private final LinkedList<Float> hrBuffer = new LinkedList<>();
    private final LinkedList<Float> stepsBuffer = new LinkedList<>();
    
    // Personalization baseline (could be loaded from user profile later)
    private float userBaselineHr = 70.0f; // Default starting baseline

    public BiometricClassifier(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("stress_classifier.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    
    public static class MoodPrediction {
        public final String mood;
        public final int confidencePercent;
        
        public MoodPrediction(String mood, int confidencePercent) {
            this.mood = mood;
            this.confidencePercent = confidencePercent;
        }
    }
    
    public void setUserBaselineHr(float baselineHr) {
        this.userBaselineHr = baselineHr;
    }

    /**
     * Updates buffers and predicts the user's mood based on biometrics.
     * @param currentBpm User's current heart rate
     * @param currentSteps User's current activity steps
     * @return MoodPrediction object containing the label and confidence
     */
    public MoodPrediction predictMood(float currentBpm, float currentSteps) {
        // Update buffers
        hrBuffer.addLast(currentBpm);
        stepsBuffer.addLast(currentSteps);
        
        if (hrBuffer.size() > BUFFER_5M_SIZE) hrBuffer.removeFirst();
        if (stepsBuffer.size() > BUFFER_1M_SIZE) stepsBuffer.removeFirst(); // Activity trend only needs 1m
        
        // Compute Temporal Features
        float hr1mAvg = computeAverage(hrBuffer, BUFFER_1M_SIZE);
        float hr5mAvg = computeAverage(hrBuffer, BUFFER_5M_SIZE);
        
        // HR Change Rate over the last minute (Current - value 60 secs ago)
        float hrChangeRate = 0f;
        if (hrBuffer.size() >= BUFFER_1M_SIZE) {
            hrChangeRate = currentBpm - hrBuffer.get(hrBuffer.size() - BUFFER_1M_SIZE);
        } else if (hrBuffer.size() > 1) {
            hrChangeRate = currentBpm - hrBuffer.getFirst();
        }
        
        float activityTrend = computeAverage(stepsBuffer, BUFFER_1M_SIZE);
        
        // Personalization
        // Update rolling baseline very slowly (e.g. over days) but for now it's static/set externally
        float hrBaselineDiff = currentBpm - userBaselineHr;

        if (tflite == null) {
            return new MoodPrediction("Unknown", 0);
        }

        // Input array: 
        // [Current_HR, Current_Steps, HR_1m_Avg, HR_5m_Avg, HR_Change_Rate, Activity_Trend, User_Baseline_HR, HR_Baseline_Difference]
        float[][] input = new float[][]{{
            currentBpm, 
            currentSteps, 
            hr1mAvg, 
            hr5mAvg, 
            hrChangeRate, 
            activityTrend, 
            userBaselineHr, 
            hrBaselineDiff
        }};
        
        float[][] output = new float[1][3];

        tflite.run(input, output);

        int maxIndex = 0;
        float maxProb = output[0][0];
        for (int i = 1; i < 3; i++) {
            if (output[0][i] > maxProb) {
                maxProb = output[0][i];
                maxIndex = i;
            }
        }

        int confidence = Math.round(maxProb * 100);
        return new MoodPrediction(MOOD_CLASSES[maxIndex], confidence);
    }
    
    private float computeAverage(LinkedList<Float> buffer, int limit) {
        if (buffer.isEmpty()) return 0f;
        int count = Math.min(buffer.size(), limit);
        float sum = 0f;
        // Start from the end of the list (most recent)
        int startIndex = buffer.size() - count;
        for (int i = startIndex; i < buffer.size(); i++) {
            sum += buffer.get(i);
        }
        return sum / count;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
