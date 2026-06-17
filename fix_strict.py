import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_filtering = """                List<String> knownLanguages = Arrays.asList("telugu", "tamil", "kannada", "malayalam", "hindi", "punjabi", "bengali", "marathi", "gujarati", "spanish", "french", "german", "korean", "japanese", "english");

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
                }"""

new_filtering = """                // STRICT Filtering & Scoring
                for (JSONObject songData : allFetchedSongs.values()) {
                    RankedSong rankedSong = new RankedSong(songData);
                    scoreSong(rankedSong, currentMood, langs, genres, artists, history, recentTracks);
                    
                    boolean drop = false;
                    String rawData = songData.toString().toLowerCase();

                    // 1. STRICT LANGUAGE MATCH
                    // If the user requested languages, the song MUST explicitly contain at least one of them in its metadata.
                    if (!langs.isEmpty() && !langs.get(0).isEmpty()) {
                        boolean matchesLang = false;
                        for (String l : langs) {
                            if (!l.isEmpty() && rawData.contains(l.toLowerCase())) {
                                matchesLang = true;
                                break;
                            }
                        }
                        if (!matchesLang) {
                            drop = true;
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
                // instead of showing garbage wrong-language/wrong-artist songs."""

content = content.replace(old_filtering, new_filtering)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java to use strict filtering.")
