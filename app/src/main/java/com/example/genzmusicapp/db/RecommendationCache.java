package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "recommendation_cache")
public class RecommendationCache {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String mood;
    @NonNull
    public String trackId;
    public String jsonPayload;
    public int finalScore;
    
    public RecommendationCache() {
        trackId = "";
    }
}
