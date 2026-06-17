import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_strict_logic = """                // Strict Adherence Filter
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                    
                    // We only enforce Artist and Genre. Language is hard to enforce reliably due to missing metadata.
                    // However, because we only fetch using the exact queries above, the returned songs WILL implicitly match the language if it was requested.
                    boolean drop = false;
                    if (!artists.isEmpty() && rankedSong.artistScore == 0) drop = true;
                    if (!genres.isEmpty() && rankedSong.genreScore == 0) drop = true;

                    if (!drop) {
                        rankedSongs.add(rankedSong);
                    }
                }

                // If strict drop killed everything, we relax Genre but NEVER ARTIST!
                if (rankedSongs.isEmpty()) {
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        
                        boolean drop = false;
                        if (!artists.isEmpty() && rankedSong.artistScore == 0) drop = true;

                        if (!drop) {
                            rankedSongs.add(rankedSong);
                        }
                    }
                }

                // If it's still empty, just use whatever we fetched.
                if (rankedSongs.isEmpty()) {
                    for (JSONObject songData : allFetchedSongs.values()) {
                        RankedSong rankedSong = new RankedSong(songData);
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        rankedSongs.add(rankedSong);
                    }
                }"""

new_strict_logic = """                // Flexible Adherence Filter (OR Logic)
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                    
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
                        scoreSong(rankedSong, currentMood, langs, genres, artists, history);
                        rankedSongs.add(rankedSong);
                    }
                }"""

content = content.replace(old_strict_logic, new_strict_logic)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java with Flexible Filtering")
