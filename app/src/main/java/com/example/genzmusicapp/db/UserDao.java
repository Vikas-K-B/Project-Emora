package com.example.genzmusicapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getProfile();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProfile(UserProfile profile);
}
