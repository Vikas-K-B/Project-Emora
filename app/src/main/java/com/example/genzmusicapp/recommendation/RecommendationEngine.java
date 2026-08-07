package com.example.genzmusicapp.recommendation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecommendationEngine {
    private static final String TAG = "RecommendationEngine";
    public static final boolean DEBUG_MODE = true; // Debug mode logs individual component scores

    public static List<RankedSong> cachedRecommendations = new ArrayList<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class RankedSong {
        public JSONObject songData;
        public int finalScore;
        public String explanation;
        public String trackId;
        public String artistName;
        public String trackName;
        public String genre;
        public String artworkUrl;

        // Debug component scores
        public int moodScore = 0;
        public int genreScore = 0;
        public int artistScore = 0;
        public int languageScore = 0;
        public int historyScore = 0;
        public int penaltyScore = 0;

        public RankedSong(JSONObject data) {
            this.songData = data;
            this.trackId = String.valueOf(songData.optLong("trackId"));
            this.artistName = songData.optString("artistName", "Unknown Artist");
            this.trackName = songData.optString("trackName", "Unknown Song");
            this.genre = songData.optString("primaryGenreName", "Unknown");
            this.artworkUrl = songData.optString("artworkUrl100", "");
        }
    }

    public interface RecommendationCallback {
        void onSuccess(List<RankedSong> recommendations);
        void onFailure(String error, List<RankedSong> cachedRecommendations);
    }

    public static void fetchAndRankLegacy(
            Context context,
            String currentMood,
            String prefLanguage,
            String prefGenres,
            String prefArtists,
            RecommendationCallback callback) {

        executor.execute(() -> {
            try {
                // 1. Build a pool of broad queries to ensure fallback capability
                String moodQuery = "pop";
                if (currentMood.equals("Stressed")) moodQuery = "relaxing chill";
                else if (currentMood.equals("Relaxed") || currentMood.equals("Calm") || currentMood.equals("Resting")) moodQuery = "acoustic soft";
                else if (currentMood.equals("Energetic")) moodQuery = "upbeat energy";

                List<String> langs = parseCommaString(prefLanguage);
                List<String> genres = parseCommaString(prefGenres);
                List<String> artists = parseCommaString(prefArtists);

                // Generate search queries handling MULTIPLE artists and MULTIPLE languages
                List<String> searchQueries = new ArrayList<>();
                
                // If empty, add a default blank string so the loops run at least once
                List<String> iterLangs = langs.isEmpty() ? Collections.singletonList("") : langs;
                List<String> iterArtists = artists.isEmpty() ? Collections.singletonList("") : artists;
                List<String> iterGenres = genres.isEmpty() ? Collections.singletonList("") : genres;

                // Priority 1: Language + Artist + Genre
                for (String l : iterLangs) {
                    for (String a : iterArtists) {
                        for (String g : iterGenres) {
                            if (!l.isEmpty() && !a.isEmpty() && !g.isEmpty()) {
                                searchQueries.add((l + " " + a + " " + g + " " + moodQuery).trim());
                                searchQueries.add((l + " " + a + " " + g).trim());
                            }
                        }
                    }
                }
                
                // Priority 2: Language + Artist (Strongest Two)
                for (String l : iterLangs) {
                    for (String a : iterArtists) {
                        if (!l.isEmpty() && !a.isEmpty()) {
                            searchQueries.add((l + " " + a + " " + moodQuery).trim());
                            searchQueries.add((l + " " + a).trim());
                        }
                    }
                }
                
                // Priority 3: Single Preferences
                if (!langs.isEmpty() && artists.isEmpty()) {
                    for (String l : iterLangs) {
                        for (String g : iterGenres) {
                            if (!g.isEmpty()) searchQueries.add((l + " " + g).trim());
                        }
                        searchQueries.add((l + " " + moodQuery).trim());
                        searchQueries.add(l);
                    }
                } else if (!artists.isEmpty() && langs.isEmpty()) {
                    for (String a : iterArtists) {
                        for (String g : iterGenres) {
                            if (!g.isEmpty()) searchQueries.add((a + " " + g).trim());
                        }
                        searchQueries.add((a + " " + moodQuery).trim());
                        searchQueries.add(a);
                    }
                } else if (!genres.isEmpty() && langs.isEmpty() && artists.isEmpty()) {
                    for (String g : iterGenres) {
                        searchQueries.add((g + " " + moodQuery).trim());
                        searchQueries.add(g);
                    }
                }
                
                // Absolute Fallback
                if (langs.isEmpty() && artists.isEmpty() && genres.isEmpty()) {
                    searchQueries.add(moodQuery);
                    searchQueries.add("music");
                }

                // De-duplicate while preserving order
                List<String> uniqueQueriesList = new ArrayList<>();
                for (String q : searchQueries) {
                    if (!q.isEmpty() && !uniqueQueriesList.contains(q)) uniqueQueriesList.add(q);
                }

                // Keep the first 3 highly specific queries at the top, but randomly shuffle the broad fallback queries!
                // This guarantees that iTunes returns completely different songs every time you hit refresh.
                if (uniqueQueriesList.size() > 3) {
                    java.util.Collections.shuffle(uniqueQueriesList.subList(3, uniqueQueriesList.size()));
                }

                // 2. Fetch from iTunes API
                Map<String, JSONObject> allFetchedSongs = new HashMap<>();
                
                for (String query : uniqueQueriesList) {
                    if (allFetchedSongs.size() >= 400) break; // Fetch a massive pool so refresh gives genuinely new songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=100");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) continue;

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject song = results.getJSONObject(i);
                            String trackId = String.valueOf(song.optLong("trackId"));
                            if (!allFetchedSongs.containsKey(trackId)) {
                                allFetchedSongs.put(trackId, song);
                            }
                        }
                    }
                }

                if (allFetchedSongs.isEmpty()) {
                    callback.onFailure("No songs found online.", cachedRecommendations);
                    return;
                }

                // 3. Score the songs
                SharedPreferences prefs = context.getSharedPreferences("MusicZPrefs", Context.MODE_PRIVATE);
                JSONObject history = new JSONObject(prefs.getString("user_learned_preferences", "{}"));
                
                String recentTracksStr = prefs.getString("recently_recommended_tracks", "");
                List<String> recentTracks = new ArrayList<>(Arrays.asList(recentTracksStr.split(",")));

                List<RankedSong> rankedSongs = new ArrayList<>();
                
                // STRICT Filtering & Scoring
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                    
                    boolean drop = false;
                    String rawData = songData.toString().toLowerCase();

                    // 1. CONFIDENCE-BASED LANGUAGE MATCH
                    // iTunes metadata often lacks explicit language tags (especially for "English" or regional Indian songs).
                    // So we only drop the song if it EXPLICITLY contains evidence of a WRONG language.
                    if (!langs.isEmpty() && !langs.get(0).isEmpty()) {
                        java.util.List<String> knownLanguages = java.util.Arrays.asList("telugu", "tamil", "kannada", "malayalam", "hindi", "punjabi", "bengali", "marathi", "gujarati", "spanish", "french", "german", "korean", "japanese", "english", "bollywood", "tollywood", "kollywood", "sandalwood");
                        for (String known : knownLanguages) {
                            if (rawData.contains(known)) {
                                boolean userWantsThis = false;
                                for (String userLang : langs) {
                                    if (userLang.toLowerCase().contains(known) || known.contains(userLang.toLowerCase())) {
                                        userWantsThis = true;
                                        break;
                                    }
                                    // Treat sandalwood as kannada, bollywood as hindi, etc.
                                    if (userLang.equalsIgnoreCase("kannada") && known.equals("sandalwood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("hindi") && known.equals("bollywood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("telugu") && known.equals("tollywood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("tamil") && known.equals("kollywood")) userWantsThis = true;
                                }
                                if (!userWantsThis) {
                                    drop = true;
                                    break;
                                }
                            }
                        }
                    }

                    // 2. STRICT ARTIST MATCH
                    // Fixes bug where song title matching artist name gets recommended
                    boolean matchesArtist = false;
                    for (String a : artists) {
                        if (!a.isEmpty() && rankedSong.artistName.toLowerCase().contains(a.toLowerCase())) {
                            matchesArtist = true;
                            break;
                        }
                    }
                    if (!artists.isEmpty() && !artists.get(0).isEmpty() && !matchesArtist) {
                        drop = true;
                    }
                    
                    // 3. STRICT GENRE MATCH
                    boolean matchesGenre = false;
                    for (String g : genres) {
                        if (!g.isEmpty() && rankedSong.genre.toLowerCase().contains(g.toLowerCase())) {
                            matchesGenre = true;
                            break;
                        }
                    }
                    // Only drop on genre if they asked for genres but NOT artists. 
                    // (If they asked for an artist, we prioritize the artist over genre).
                    if ((artists.isEmpty() || artists.get(0).isEmpty()) && !genres.isEmpty() && !genres.get(0).isEmpty() && !matchesGenre) {
                        drop = true;
                    }

                    if (!drop) {
                        rankedSongs.add(rankedSong);
                    }
                }

                // NO Ultimate Fallback. 
                // If strict filters drop everything, we return empty so the UI knows there are no valid matches,
                // instead of showing garbage wrong-language/wrong-artist songs.

                // 4. Recommendation Bucketing
                Map<String, List<RankedSong>> buckets = new HashMap<>();
                for (RankedSong song : rankedSongs) {
                    String bArtist = "Other";
                    for (String a : artists) {
                        if (!a.isEmpty() && song.artistName.toLowerCase().contains(a.toLowerCase())) {
                            bArtist = a;
                            break;
                        }
                    }
                    String bLang = "Other";
                    for (String l : langs) {
                        if (!l.isEmpty() && song.songData.toString().toLowerCase().contains(l.toLowerCase())) {
                            bLang = l;
                            break;
                        }
                    }
                    String bucketKey = bLang + "-" + bArtist;
                    if (!buckets.containsKey(bucketKey)) buckets.put(bucketKey, new ArrayList<>());
                    buckets.get(bucketKey).add(song);
                }

                for (List<RankedSong> bucketList : buckets.values()) {
                    // Shuffle before sort to randomize ties
                    Collections.shuffle(bucketList);
                    Collections.sort(bucketList, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));
                }

                // 5. Round-robin pick with diversity penalties and 30% cap
                List<RankedSong> finalPlaylist = new ArrayList<>();
                Map<String, Integer> artistCount = new HashMap<>();
                int maxPerArtist = 7; // 30% of 25

                String lastArtist = "";
                String lastGenre = "";
                
                boolean addedAny;
                do {
                    addedAny = false;
                    for (String key : buckets.keySet()) {
                        List<RankedSong> bucketList = buckets.get(key);
                        if (!bucketList.isEmpty()) {
                            RankedSong song = bucketList.remove(0);
                            
                            // Artist contribution cap (30%)
                            int currentArtistCount = artistCount.getOrDefault(song.artistName, 0);
                            if (currentArtistCount >= maxPerArtist) {
                                continue; 
                            }
                            
                            // Diversity penalties
                            if (song.artistName.equalsIgnoreCase(lastArtist)) {
                                song.penaltyScore += 15;
                                song.finalScore -= 15;
                            }
                            if (song.genre.equalsIgnoreCase(lastGenre)) {
                                song.penaltyScore += 5;
                                song.finalScore -= 5;
                            }
                            
                            artistCount.put(song.artistName, currentArtistCount + 1);
                            finalPlaylist.add(song);
                            addedAny = true;
                            lastArtist = song.artistName;
                            lastGenre = song.genre;
                            
                            if (finalPlaylist.size() >= 25) break;
                        }
                    }
                } while (addedAny && finalPlaylist.size() < 25);
                
                // Re-sort the final playlist by score descending so the absolute best ones are at the top
                Collections.sort(finalPlaylist, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));
                
                // 6. Update Recommendation History
                for (RankedSong song : finalPlaylist) {
                    if (!recentTracks.contains(song.trackId)) {
                        recentTracks.add(song.trackId);
                    }
                }
                if (recentTracks.size() > 100) {
                    recentTracks = new ArrayList<>(recentTracks.subList(recentTracks.size() - 100, recentTracks.size()));
                }
                prefs.edit().putString("recently_recommended_tracks", android.text.TextUtils.join(",", recentTracks)).apply();

                // Cache it for API fallback
                cachedRecommendations = new ArrayList<>(finalPlaylist);

                // Log debug if enabled
                if (DEBUG_MODE) {
                    for (int i = 0; i < finalPlaylist.size(); i++) {
                        RankedSong s = finalPlaylist.get(i);
                        Log.d(TAG, String.format("Rank %d: %s - %s | Score: %d (M:%d G:%d A:%d L:%d H:%d P:-%d) | Reason: %s",
                                i+1, s.trackName, s.artistName, s.finalScore, s.moodScore, s.genreScore, s.artistScore, s.languageScore, s.historyScore, s.penaltyScore, s.explanation));
                    }
                }

                // Save Stats for Developer Diagnostics
                try {
                    JSONObject stats = new JSONObject();
                    JSONObject languageCounts = new JSONObject();
                    JSONObject artistCounts = new JSONObject();
                    for (RankedSong s : finalPlaylist) {
                        artistCounts.put(s.artistName, artistCounts.optInt(s.artistName, 0) + 1);
                    }
                    stats.put("artistCounts", artistCounts);
                    stats.put("totalSongs", finalPlaylist.size());
                    
                    JSONArray songScores = new JSONArray();
                    for (int i = 0; i < finalPlaylist.size(); i++) {
                        RankedSong s = finalPlaylist.get(i);
                        JSONObject score = new JSONObject();
                        score.put("rank", i + 1);
                        score.put("name", s.trackName);
                        score.put("artist", s.artistName);
                        score.put("totalScore", s.finalScore);
                        score.put("mood", s.moodScore);
                        score.put("lang", s.languageScore);
                        score.put("artistScore", s.artistScore);
                        score.put("genre", s.genreScore);
                        score.put("history", s.historyScore);
                        score.put("penalty", s.penaltyScore);
                        songScores.put(score);
                    }
                    stats.put("scores", songScores);
                    prefs.edit().putString("diagnostics_json", stats.toString()).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                callback.onSuccess(finalPlaylist);

            } catch (Exception e) {
                Log.e(TAG, "Recommendation Error: " + e.getMessage());
                callback.onFailure("Failed to fetch recommendations: " + e.getMessage(), cachedRecommendations);
            }
        });
    }

    private static void scoreSong(RankedSong song, String mood, List<String> userLangs, List<String> userGenres, List<String> userArtists, JSONObject history, List<String> recentTracks) {
        String trackArtist = song.artistName.toLowerCase();
        String trackGenre = song.genre.toLowerCase();
        String trackName = song.trackName.toLowerCase();

        // Mood Match (Max 35)
        if (mood.equals("Stressed")) {
            if (trackGenre.contains("lo-fi") || trackGenre.contains("chill") || trackGenre.contains("ambient") || trackGenre.contains("classical") || trackGenre.contains("acoustic") || trackGenre.contains("soft")) song.moodScore = 35;
            else if (trackGenre.contains("pop") || trackGenre.contains("indie")) song.moodScore = 15;
            else song.moodScore = 5;
        } else if (mood.equals("Resting") || mood.equals("Calm") || mood.equals("Relaxed")) {
            if (trackGenre.contains("acoustic") || trackGenre.contains("singer/songwriter") || trackGenre.contains("r&b") || trackGenre.contains("jazz")) song.moodScore = 35;
            else if (trackGenre.contains("pop") || trackGenre.contains("country")) song.moodScore = 15;
            else song.moodScore = 5;
        } else if (mood.equals("Energetic")) {
            if (trackGenre.contains("dance") || trackGenre.contains("electronic") || trackGenre.contains("hip-hop") || trackGenre.contains("rock") || trackGenre.contains("pop")) song.moodScore = 35;
            else song.moodScore = 5;
        } else {
            song.moodScore = 15;
        }

        // Language Match (Max 25)
        for (String l : userLangs) {
            if (!l.isEmpty() && (trackName.contains(l.toLowerCase()) || trackArtist.contains(l.toLowerCase()) || trackGenre.contains(l.toLowerCase()))) {
                song.languageScore = 25;
                break;
            }
        }
        if (userLangs.isEmpty() || (userLangs.size() == 1 && userLangs.get(0).isEmpty())) {
            song.languageScore = 25; 
        }

        // Artist Match (Max 20)
        for (String a : userArtists) {
            if (!a.isEmpty() && trackArtist.contains(a.toLowerCase())) {
                song.artistScore = 20;
                break;
            }
        }

        // Genre Match (Max 10)
        for (String g : userGenres) {
            if (!g.isEmpty() && (trackGenre.contains(g.toLowerCase()) || g.toLowerCase().contains(trackGenre))) {
                song.genreScore = 10;
                break;
            }
        }

        // History Match (Max 10)
        int artistClicks = history.optInt("artist_" + trackArtist, 0);
        int genreClicks = history.optInt(trackGenre, 0);
        song.historyScore = Math.min(10, (artistClicks * 2) + genreClicks);

        // Recent Tracks Penalty (-50) to force refresh to give new songs
        if (recentTracks.contains(song.trackId)) {
            song.penaltyScore += 50;
        }

        // Sum
        song.finalScore = song.moodScore + song.genreScore + song.artistScore + song.languageScore + song.historyScore - song.penaltyScore;
        if (song.finalScore > 99) song.finalScore = 99;
        
        // Generate Dynamic Explanation
        song.explanation = generateExplanation(song, mood);
    }
    
    // Ignore the old scoreSong body
    private static void scoreSong_OLD(RankedSong song, String mood, List<String> userLangs, List<String> userGenres, List<String> userArtists, JSONObject history) {
        String trackArtist = song.artistName.toLowerCase();
        String trackGenre = song.genre.toLowerCase();
        String trackName = song.trackName.toLowerCase();

        // Mood Match (Max 40)
        if (mood.equals("Stressed")) {
            if (trackGenre.contains("lo-fi") || trackGenre.contains("chill") || trackGenre.contains("ambient") || trackGenre.contains("classical") || trackGenre.contains("acoustic") || trackGenre.contains("soft")) song.moodScore = 40;
            else if (trackGenre.contains("pop") || trackGenre.contains("indie")) song.moodScore = 20;
            else song.moodScore = 10;
        } else if (mood.equals("Resting") || mood.equals("Calm") || mood.equals("Relaxed")) {
            if (trackGenre.contains("acoustic") || trackGenre.contains("singer/songwriter") || trackGenre.contains("r&b") || trackGenre.contains("jazz")) song.moodScore = 40;
            else if (trackGenre.contains("pop") || trackGenre.contains("country")) song.moodScore = 20;
            else song.moodScore = 10;
        } else if (mood.equals("Energetic")) {
            if (trackGenre.contains("dance") || trackGenre.contains("electronic") || trackGenre.contains("hip-hop") || trackGenre.contains("rock") || trackGenre.contains("pop")) song.moodScore = 40;
            else song.moodScore = 10;
        } else {
            song.moodScore = 20;
        }

        // Genre Match (Max 25)
        for (String g : userGenres) {
            if (!g.isEmpty() && (trackGenre.contains(g.toLowerCase()) || g.toLowerCase().contains(trackGenre))) {
                song.genreScore = 25;
                break;
            }
        }

        // Artist Match (Max 15)
        for (String a : userArtists) {
            if (!a.isEmpty() && trackArtist.contains(a.toLowerCase())) {
                song.artistScore = 15;
                break;
            }
        }

        // Language Match (Max 10)
        for (String l : userLangs) {
            if (!l.isEmpty() && (trackName.contains(l.toLowerCase()) || trackArtist.contains(l.toLowerCase()) || trackGenre.contains(l.toLowerCase()))) {
                song.languageScore = 10;
                break;
            }
        }
        if (userLangs.isEmpty() || (userLangs.size() == 1 && userLangs.get(0).isEmpty())) {
            song.languageScore = 10; 
        }

        // History Match (Max 10)
        int artistClicks = history.optInt("artist_" + trackArtist, 0);
        int genreClicks = history.optInt(trackGenre, 0);
        int totalHistoryScore = Math.min(10, (artistClicks * 2) + genreClicks);
        song.historyScore = totalHistoryScore;

        // Sum
        song.finalScore = song.moodScore + song.genreScore + song.artistScore + song.languageScore + song.historyScore;
        if (song.finalScore > 99) song.finalScore = 99;
        
        // Generate Dynamic Explanation
        song.explanation = generateExplanation(song, mood);
    }

    private static String generateExplanation(RankedSong song, String mood) {
        StringBuilder sb = new StringBuilder();

        List<String> reasons = new ArrayList<>();
        if (song.moodScore >= 30) {
            reasons.add("Fits your " + mood + " mood");
        } else {
            reasons.add("Provides a balanced contrast to your " + mood + " state");
        }

        if (song.artistScore > 0) {
            reasons.add("strictly features your preferred artist " + song.artistName);
        } 
        if (song.genreScore > 0) {
            reasons.add("strictly aligns with your taste in " + song.genre + " music");
        }

        if (song.historyScore >= 5) {
            reasons.add("you frequently listen to similar tracks");
        }

        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0 && i == reasons.size() - 1) sb.append(" and ");
            else if (i > 0) sb.append(", ");
            sb.append(reasons.get(i));
        }
        sb.append(".");

        return sb.toString();
    }

    private static List<String> parseCommaString(String input) {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : input.split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }
}
