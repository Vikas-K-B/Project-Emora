package com.example.genzmusicapp.recommendation;

import android.util.Log;
import com.example.genzmusicapp.recommendation.model.PlaylistResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RecommendationParser {
    private static final String TAG = "RecommendationParser";
    private final Gson gson;

    public RecommendationParser() {
        this.gson = new Gson();
    }

    public PlaylistResponse parse(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try {
            // Sometimes the LLM might wrap the JSON in markdown blocks like ```json ... ```
            // We should aggressively clean it before parsing
            String cleanedJson = cleanJson(jsonString);
            return gson.fromJson(cleanedJson, PlaylistResponse.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Failed to parse JSON from Gemini: " + jsonString, e);
            return null;
        }
    }

    private String cleanJson(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
    
    public String toJson(PlaylistResponse response) {
        return gson.toJson(response);
    }
}
