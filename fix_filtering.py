import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_scoring_loop = """                List<RankedSong> rankedSongs = new ArrayList<>();
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                    rankedSongs.add(rankedSong);
                }

                // Shuffle before sort to randomize ties and make refresh button give new results
                Collections.shuffle(rankedSongs);
                // Sort by final score descending
                Collections.sort(rankedSongs, (a, b) -> Integer.compare(b.finalScore, a.finalScore));"""

new_scoring_loop = """                List<RankedSong> rankedSongs = new ArrayList<>();
                
                // Level 0: Strict Match (Must match Artist AND Genre if specified)
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                    
                    boolean drop = false;
                    if (!genres.isEmpty() && rankedSong.genreScore == 0) drop = true;
                    if (!artists.isEmpty() && rankedSong.artistScore == 0) drop = true;
                    
                    if (!drop) {
                        rankedSongs.add(rankedSong);
                    }
                }
                
                // Level 1: If strictly matching both yields < 10, relax Genre slightly,
                // BUT WE NEVER RELAX ARTIST. Artist must always match!
                if (rankedSongs.size() < 10) {
                    rankedSongs.clear();
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        
                        boolean drop = false;
                        if (!artists.isEmpty() && rankedSong.artistScore == 0) drop = true;
                        // If user provided no artist but provided genre, we must enforce genre
                        if (artists.isEmpty() && !genres.isEmpty() && rankedSong.genreScore == 0) drop = true;
                        
                        if (!drop) {
                            if (!rankedSongs.contains(rankedSong)) {
                                rankedSongs.add(rankedSong);
                            }
                        }
                    }
                }
                
                // Level 2: Absolute Fallback (if still < 5, just use anything that matches AT LEAST ONE preference)
                if (rankedSongs.size() < 5) {
                    rankedSongs.clear();
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        
                        boolean hasAnyPreference = !genres.isEmpty() || !artists.isEmpty() || !langs.isEmpty();
                        boolean matchesAnyPreference = (rankedSong.genreScore > 0) || (rankedSong.artistScore > 0) || (rankedSong.languageScore > 0);
                        
                        if (!hasAnyPreference || matchesAnyPreference) {
                            if (!rankedSongs.contains(rankedSong)) {
                                rankedSongs.add(rankedSong);
                            }
                        }
                    }
                }
                
                // Ultimate Fallback: Just return whatever we have if it's completely empty
                if (rankedSongs.isEmpty()) {
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        rankedSongs.add(rankedSong);
                    }
                }

                // Shuffle before sort to randomize ties and make refresh button give new results
                Collections.shuffle(rankedSongs);
                // Sort by final score descending
                Collections.sort(rankedSongs, (a, b) -> Integer.compare(b.finalScore, a.finalScore));"""

content = content.replace(old_scoring_loop, new_scoring_loop)

# Fix Explanation to emphasize the strict matching
old_explanation = """        if (song.artistScore > 0) {
            reasons.add("features your preferred artist " + song.artistName);
        } else if (song.genreScore > 0) {
            reasons.add("aligns with your taste in " + song.genre + " music");
        }"""
        
new_explanation = """        if (song.artistScore > 0) {
            reasons.add("strictly features your preferred artist " + song.artistName);
        } 
        if (song.genreScore > 0) {
            reasons.add("strictly aligns with your taste in " + song.genre + " music");
        }"""
        
content = content.replace(old_explanation, new_explanation)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java with Strict Filtering")
