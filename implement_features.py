import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add recent tracks reading and score weighting changes
score_song_old = """    private static void scoreSong(RankedSong song, String mood, List<String> userLangs, List<String> userGenres, List<String> userArtists, JSONObject history) {"""

score_song_new = """    private static void scoreSong(RankedSong song, String mood, List<String> userLangs, List<String> userGenres, List<String> userArtists, JSONObject history, List<String> recentTracks) {
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
    private static void scoreSong_OLD(RankedSong song, String mood, List<String> userLangs, List<String> userGenres, List<String> userArtists, JSONObject history) {"""

content = content.replace(score_song_old, score_song_new)

# Fix where scoreSong is called
content = content.replace("scoreSong(rankedSong, currentMood, langs, genres, artists, history);", "scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);")

# Update filtering and bucketing
old_filtering = """                // 3. Score the songs
                SharedPreferences prefs = context.getSharedPreferences("MusicZPrefs", Context.MODE_PRIVATE);
                JSONObject history = new JSONObject(prefs.getString("user_learned_preferences", "{}"));

                List<RankedSong> rankedSongs = new ArrayList<>();
                
                // Flexible Adherence Filter (OR Logic)
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                    
                    boolean drop = false;
                    
                    boolean hasStrongPrefs = !artists.isEmpty() || !genres.isEmpty();
                    boolean matchesArtist = !artists.isEmpty() && rankedSong.artistScore > 0;
                    boolean matchesGenre = !genres.isEmpty() && rankedSong.genreScore > 0;
                    
                    // If the user specified Artist or Genre, the song MUST match at least one of them!
                    // This allows a song by a preferred artist to be kept even if the genre is slightly off.
                    if (hasStrongPrefs && !matchesArtist && !matchesGenre) {
                        drop = true;
                    }

                    if (!drop) {
                        rankedSongs.add(rankedSong);
                    }
                }

                // Ultimate Fallback: If filtering dropped everything, just use whatever iTunes returned
                if (rankedSongs.isEmpty()) {
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                        rankedSongs.add(rankedSong);
                    }
                }

                // Shuffle before sort to randomize ties and make refresh button give new results
                Collections.shuffle(rankedSongs);
                // Sort by final score descending
                Collections.sort(rankedSongs, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));

                // 4. Apply Diversity Penalties (avoid consecutive identical artists)
                List<RankedSong> diversePlaylist = new ArrayList<>();
                String lastArtist = "";
                for (RankedSong song : rankedSongs) {
                    if (song.artistName.equalsIgnoreCase(lastArtist)) {
                        song.penaltyScore = 15;
                        song.finalScore -= song.penaltyScore;
                    }
                    diversePlaylist.add(song);
                    lastArtist = song.artistName;
                }

                // Re-sort after penalties
                Collections.sort(diversePlaylist, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));

                // Take top 25 with massive randomness to make Refresh button provide entirely new songs
                List<RankedSong> finalPlaylist = new ArrayList<>();
                if (diversePlaylist.size() <= 25) {
                    // If we somehow only got 25 songs, shuffle them so at least the order changes!
                    Collections.shuffle(diversePlaylist);
                    finalPlaylist.addAll(diversePlaylist);
                } else {
                    // Take the top 50 highly relevant songs, and SHUFFLE them completely!
                    List<RankedSong> topPool = new ArrayList<>(diversePlaylist.subList(0, Math.min(50, diversePlaylist.size())));
                    Collections.shuffle(topPool);
                    
                    // Pick 25 random songs from the highly relevant pool
                    for (int i = 0; i < 25 && i < topPool.size(); i++) {
                        finalPlaylist.add(topPool.get(i));
                    }
                    
                    // Re-sort the final 25 by score descending so the best ones are at the top of the UI
                    Collections.sort(finalPlaylist, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));
                }
                
                // Cache it
                cachedRecommendations = new ArrayList<>(finalPlaylist);"""

new_filtering = """                // 3. Score the songs
                SharedPreferences prefs = context.getSharedPreferences("MusicZPrefs", Context.MODE_PRIVATE);
                JSONObject history = new JSONObject(prefs.getString("user_learned_preferences", "{}"));
                
                String recentTracksStr = prefs.getString("recently_recommended_tracks", "");
                List<String> recentTracks = new ArrayList<>(Arrays.asList(recentTracksStr.split(",")));

                List<RankedSong> rankedSongs = new ArrayList<>();
                
                List<String> knownLanguages = Arrays.asList("telugu", "tamil", "kannada", "malayalam", "hindi", "punjabi", "bengali", "marathi", "gujarati", "spanish", "french", "german", "korean", "japanese", "english");

                // Confidence-Based Language Filtering & Scoring
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                    
                    boolean drop = false;
                    String rawData = songData.toString().toLowerCase();

                    // Check for explicit WRONG language evidence
                    for (String known : knownLanguages) {
                        if (rawData.contains(known)) {
                            boolean userWantsThis = false;
                            for (String userLang : langs) {
                                if (userLang.toLowerCase().contains(known) || known.contains(userLang.toLowerCase())) {
                                    userWantsThis = true;
                                    break;
                                }
                            }
                            // Reject ONLY songs with evidence of a wrong language.
                            if (!userWantsThis && !langs.isEmpty() && !langs.get(0).isEmpty()) {
                                drop = true;
                                break;
                            }
                        }
                    }
                    
                    boolean hasStrongPrefs = !artists.isEmpty() || !genres.isEmpty();
                    boolean matchesArtist = !artists.isEmpty() && rankedSong.artistScore > 0;
                    boolean matchesGenre = !genres.isEmpty() && rankedSong.genreScore > 0;
                    
                    // If the user specified Artist or Genre, the song MUST match at least one of them!
                    if (hasStrongPrefs && !matchesArtist && !matchesGenre) {
                        drop = true;
                    }

                    if (!drop) {
                        rankedSongs.add(rankedSong);
                    }
                }

                // Ultimate Fallback
                if (rankedSongs.isEmpty()) {
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                        rankedSongs.add(rankedSong);
                    }
                }

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
                cachedRecommendations = new ArrayList<>(finalPlaylist);"""

content = content.replace(old_filtering, new_filtering)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java with User Approved features.")
