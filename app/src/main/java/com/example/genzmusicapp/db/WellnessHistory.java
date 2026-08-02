package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wellness_history")
public class WellnessHistory {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public long timestamp;
    public String userId;
    public long bpm;
    public long steps;
    public long calculatedScore;
    public String calculatedMood;
}
