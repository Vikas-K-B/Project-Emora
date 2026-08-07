package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "recommendation_cache")
public class RecommendationCache {
    @PrimaryKey
    @NonNull
    public String mood;
    public String playlistJson;
    public long timestamp;
    public String preferencesHash;

    public RecommendationCache(@NonNull String mood) {
        this.mood = mood;
    }
}
