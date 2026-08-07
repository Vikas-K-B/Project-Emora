package com.example.genzmusicapp.recommendation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.genzmusicapp.recommendation.model.PlaylistResponse;

import java.util.List;

public class RecommendationManager {
    private static final String TAG = "RecommendationManager";
    private static final int MAX_RETRIES = 1;

    private final RecommendationRepository repository;
    private final GeminiService geminiService;
    private final RecommendationValidator validator;
    private final RecommendationParser parser;
    private final Handler mainHandler;

    public interface ManagerCallback {
        void onSuccess(PlaylistResponse playlist);
        void onFallback(String reason);
    }

    public RecommendationManager(Context context) {
        this.parser = new RecommendationParser();
        this.repository = new RecommendationRepository(context, parser);
        this.geminiService = new GeminiService();
        this.validator = new RecommendationValidator(new ITunesService());
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchRecommendations(String mood, String activity, String timeOfDay,
                                     String langs, String genres, String artists,
                                     List<String> recent, List<String> skipped, List<String> favorites,
                                     boolean forceRefresh, ManagerCallback callback) {

        String currentPreferencesHash = String.valueOf((langs + genres + artists).hashCode());

        if (!forceRefresh) {
            repository.getCachedPlaylist(mood, currentPreferencesHash, cached -> {
                if (cached != null) {
                    mainHandler.post(() -> callback.onSuccess(cached));
                } else {
                    executeGeminiFlow(mood, activity, timeOfDay, langs, genres, artists,
                            recent, skipped, favorites, currentPreferencesHash, 0, callback);
                }
            });
        } else {
            executeGeminiFlow(mood, activity, timeOfDay, langs, genres, artists,
                    recent, skipped, favorites, currentPreferencesHash, 0, callback);
        }
    }

    private void executeGeminiFlow(String mood, String activity, String timeOfDay,
                                   String langs, String genres, String artists,
                                   List<String> recent, List<String> skipped, List<String> favorites,
                                   String prefHash, int retryCount, ManagerCallback callback) {
        
        String systemPrompt = RecommendationPromptBuilder.buildSystemPrompt();
        String userPrompt;
        
        if (retryCount == 0) {
            userPrompt = RecommendationPromptBuilder.buildUserPrompt(mood, activity, timeOfDay,
                    langs, genres, artists, recent, skipped, favorites);
        } else {
            userPrompt = RecommendationPromptBuilder.buildRetryPrompt();
        }

        geminiService.generatePlaylist(systemPrompt, userPrompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                // Background thread parsing and validation
                new Thread(() -> {
                    PlaylistResponse response = parser.parse(jsonResponse);
                    if (response != null && validator.validate(response)) {
                        repository.savePlaylist(mood, response, prefHash);
                        mainHandler.post(() -> callback.onSuccess(response));
                    } else {
                        handleFailure(mood, activity, timeOfDay, langs, genres, artists,
                                recent, skipped, favorites, prefHash, retryCount, "Validation failed", callback);
                    }
                }).start();
            }

            @Override
            public void onFailure(String error) {
                handleFailure(mood, activity, timeOfDay, langs, genres, artists,
                        recent, skipped, favorites, prefHash, retryCount, error, callback);
            }
        });
    }

    private void handleFailure(String mood, String activity, String timeOfDay,
                               String langs, String genres, String artists,
                               List<String> recent, List<String> skipped, List<String> favorites,
                               String prefHash, int retryCount, String reason, ManagerCallback callback) {
        Log.e(TAG, "Gemini flow failed: " + reason);
        if (retryCount < MAX_RETRIES) {
            Log.d(TAG, "Retrying...");
            executeGeminiFlow(mood, activity, timeOfDay, langs, genres, artists,
                    recent, skipped, favorites, prefHash, retryCount + 1, callback);
        } else {
            Log.d(TAG, "Max retries reached. Triggering fallback.");
            mainHandler.post(() -> callback.onFallback(reason));
        }
    }
}
