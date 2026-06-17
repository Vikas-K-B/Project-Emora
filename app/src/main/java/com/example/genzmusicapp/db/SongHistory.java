package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "song_history")
public class SongHistory {
    @PrimaryKey
    @NonNull
    public String trackId;
    
    public String trackName;
    public String artistName;
    public String genre;
    public long lastPlayedTimestamp;
    public int playCount;
    public int userFeedback; // e.g. -1 for not interested, 0 neutral, 1 liked
    
    public SongHistory() {
        trackId = "";
    }
}
