package com.example.genzmusicapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RecommendationDao {
    @Query("SELECT * FROM recommendation_cache WHERE mood = :mood")
    RecommendationCache getRecommendationForMood(String mood);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insert(RecommendationCache recommendation);

    @Query("DELETE FROM recommendation_cache WHERE mood = :mood")
    void clearCacheForMood(String mood);
}
