package com.example.genzmusicapp.recommendation;

import android.content.Context;
import com.example.genzmusicapp.db.AppDatabase;
import com.example.genzmusicapp.db.RecommendationCache;
import com.example.genzmusicapp.recommendation.model.PlaylistResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecommendationRepository {
    private final AppDatabase db;
    private final RecommendationParser parser;
    private final ExecutorService executor;

    public RecommendationRepository(Context context, RecommendationParser parser) {
        this.db = AppDatabase.getDatabase(context);
        this.parser = parser;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface CacheCallback {
        void onLoaded(PlaylistResponse response);
    }

    public void getCachedPlaylist(String mood, String currentPreferencesHash, CacheCallback callback) {
        executor.execute(() -> {
            RecommendationCache cache = db.recommendationDao().getRecommendationForMood(mood);
            if (cache != null) {
                // If the user changed their preferences (like languages or genres) or the cache is too old,
                // we might want to invalidate. But for now, we just return it if preferences match.
                if (currentPreferencesHash != null && !currentPreferencesHash.equals(cache.preferencesHash)) {
                    // Preferences changed, force refresh
                    callback.onLoaded(null);
                    return;
                }
                
                PlaylistResponse response = parser.parse(cache.playlistJson);
                callback.onLoaded(response);
            } else {
                callback.onLoaded(null);
            }
        });
    }

    public void savePlaylist(String mood, PlaylistResponse response, String currentPreferencesHash) {
        executor.execute(() -> {
            RecommendationCache cache = new RecommendationCache(mood);
            cache.playlistJson = parser.toJson(response);
            cache.timestamp = System.currentTimeMillis();
            cache.preferencesHash = currentPreferencesHash;
            db.recommendationDao().insert(cache);
        });
    }
    
    public void clearCache(String mood) {
        executor.execute(() -> db.recommendationDao().clearCacheForMood(mood));
    }
}
