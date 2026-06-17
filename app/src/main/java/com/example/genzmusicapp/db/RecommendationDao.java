package com.example.genzmusicapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RecommendationDao {
    @Query("SELECT * FROM recommendation_cache WHERE mood = :mood ORDER BY finalScore DESC")
    List<RecommendationCache> getRecommendationsForMood(String mood);

    @Insert
    void insertAll(List<RecommendationCache> recommendations);

    @Query("DELETE FROM recommendation_cache WHERE mood = :mood")
    void clearCacheForMood(String mood);
}
