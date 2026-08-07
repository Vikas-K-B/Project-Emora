package com.example.genzmusicapp.recommendation;

import android.util.Log;
import com.example.genzmusicapp.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static final String MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public interface GeminiCallback {
        void onSuccess(String jsonResponse);
        void onFailure(String error);
    }

    public void generatePlaylist(String systemPrompt, String userPrompt, GeminiCallback callback) {
        new Thread(() -> {
            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.equals("INSERT_YOUR_API_KEY_HERE")) {
                    callback.onFailure("Invalid Gemini API Key");
                    return;
                }

                URL url = new URL(MODEL_URL + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                JSONObject payload = new JSONObject();
                
                // Set system instructions
                JSONObject systemInstruction = new JSONObject();
                JSONObject systemParts = new JSONObject();
                systemParts.put("text", systemPrompt);
                systemInstruction.put("parts", new JSONArray().put(systemParts));
                payload.put("systemInstruction", systemInstruction);
                
                // Require JSON output
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("responseMimeType", "application/json");
                generationConfig.put("temperature", 0.7);
                payload.put("generationConfig", generationConfig);

                // Set user message
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                contentObj.put("role", "user");
                JSONObject textPart = new JSONObject();
                textPart.put("text", userPrompt);
                contentObj.put("parts", new JSONArray().put(textPart));
                contents.put(contentObj);
                
                payload.put("contents", contents);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject root = new JSONObject(response.toString());
                    JSONArray candidates = root.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                        if (content != null) {
                            JSONArray parts = content.optJSONArray("parts");
                            if (parts != null && parts.length() > 0) {
                                String text = parts.getJSONObject(0).optString("text", "");
                                callback.onSuccess(text);
                                return;
                            }
                        }
                    }
                    callback.onFailure("Empty or invalid response from Gemini");
                } else {
                    callback.onFailure("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini API Error", e);
                callback.onFailure("Exception: " + e.getMessage());
            }
        }).start();
    }
}
