package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public int id = 1; // Single row for current user
    
    public String prefLanguage;
    public String prefGenres;
    public String prefArtists;
    public String prefMusicType;
}
