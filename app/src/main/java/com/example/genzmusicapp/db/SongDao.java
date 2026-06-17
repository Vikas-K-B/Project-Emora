package com.example.genzmusicapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM song_history ORDER BY lastPlayedTimestamp DESC")
    List<SongHistory> getAllHistory();

    @Query("SELECT * FROM song_history WHERE trackId = :trackId")
    SongHistory getSong(String trackId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SongHistory song);
}
