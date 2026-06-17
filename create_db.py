import os

base_dir = "app/src/main/java/com/example/genzmusicapp/db"
os.makedirs(base_dir, exist_ok=True)

# 1. UserProfile Entity
with open(os.path.join(base_dir, "UserProfile.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# 2. WellnessHistory Entity
with open(os.path.join(base_dir, "WellnessHistory.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wellness_history")
public class WellnessHistory {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public long timestamp;
    public long bpm;
    public long steps;
    public long calculatedScore;
    public String calculatedMood;
}
""")

# 3. SongHistory Entity
with open(os.path.join(base_dir, "SongHistory.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# 4. RecommendationCache Entity
with open(os.path.join(base_dir, "RecommendationCache.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# 5. AppDatabase & DAOs
with open(os.path.join(base_dir, "AppDatabase.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserProfile.class, WellnessHistory.class, SongHistory.class, RecommendationCache.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract WellnessDao wellnessDao();
    public abstract SongDao songDao();
    public abstract RecommendationDao recommendationDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "musicz_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
""")

# User DAO
with open(os.path.join(base_dir, "UserDao.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# Wellness DAO
with open(os.path.join(base_dir, "WellnessDao.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# Song DAO
with open(os.path.join(base_dir, "SongDao.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

# Recommendation DAO
with open(os.path.join(base_dir, "RecommendationDao.java"), "w", encoding="utf-8") as f:
    f.write("""package com.example.genzmusicapp.db;

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
""")

print("Database files created successfully.")
