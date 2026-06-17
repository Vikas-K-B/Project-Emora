package com.example.genzmusicapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface WellnessDao {
    @Query("SELECT * FROM wellness_history ORDER BY timestamp DESC LIMIT :limit")
    List<WellnessHistory> getRecentHistory(int limit);

    @Insert
    void insert(WellnessHistory history);
}
