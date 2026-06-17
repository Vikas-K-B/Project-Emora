import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_queries = """                // Generate search queries in strict order of priority
                List<String> searchQueries = new ArrayList<>();
                
                String l = langs.isEmpty() ? "" : langs.get(0);
                String a = artists.isEmpty() ? "" : artists.get(0);
                String g = genres.isEmpty() ? "" : genres.get(0);

                // STRICT QUERY GENERATION (Fixes wrong language fetching)
                // If user specifies language and artist, we strictly search for both.
                if (!l.isEmpty() && !a.isEmpty()) {
                    searchQueries.add((l + " " + a + " " + g + " " + moodQuery).trim());
                    searchQueries.add((l + " " + a + " " + g).trim());
                    searchQueries.add((l + " " + a + " " + moodQuery).trim());
                    searchQueries.add((l + " " + a).trim());
                } else if (!l.isEmpty()) {
                    // Only Language
                    searchQueries.add((l + " " + g + " " + moodQuery).trim());
                    searchQueries.add((l + " " + g).trim());
                    searchQueries.add((l + " " + moodQuery).trim());
                    searchQueries.add(l);
                } else if (!a.isEmpty()) {
                    // Only Artist
                    searchQueries.add((a + " " + g + " " + moodQuery).trim());
                    searchQueries.add((a + " " + g).trim());
                    searchQueries.add((a + " " + moodQuery).trim());
                    searchQueries.add(a);
                } else if (!g.isEmpty()) {
                    // Only Genre
                    searchQueries.add((g + " " + moodQuery).trim());
                    searchQueries.add(g);
                } else {
                    // No preferences
                    searchQueries.add(moodQuery);
                    searchQueries.add("music");
                }"""

new_queries = """                // Generate search queries handling MULTIPLE artists and MULTIPLE languages
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
                }"""

content = content.replace(old_queries, new_queries)

old_fetch_loop = """                for (String query : uniqueQueries) {
                    if (allFetchedSongs.size() >= 100) break; // Fetch a large pool so refresh gives new songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=50");"""

new_fetch_loop = """                for (String query : uniqueQueries) {
                    if (allFetchedSongs.size() >= 150) break; // Fetch a massive pool so refresh gives genuinely new songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=25");"""

content = content.replace(old_fetch_loop, new_fetch_loop)

old_final_playlist = """                // Take top 25 with randomness to make Refresh button provide fresh songs
                List<RankedSong> finalPlaylist = new ArrayList<>();
                if (diversePlaylist.size() <= 25) {
                    finalPlaylist.addAll(diversePlaylist);
                } else {
                    // Always keep the top 5 for highest relevance
                    for (int i = 0; i < 5; i++) finalPlaylist.add(diversePlaylist.get(i));
                    
                    // Shuffle the remaining pool and pick 20 random ones
                    List<RankedSong> remaining = new ArrayList<>(diversePlaylist.subList(5, diversePlaylist.size()));
                    Collections.shuffle(remaining);
                    
                    for (int i = 0; i < 20 && i < remaining.size(); i++) {
                        finalPlaylist.add(remaining.get(i));
                    }
                    
                    // Re-sort the final playlist by score descending
                    Collections.sort(finalPlaylist, (s1, s2) -> Integer.compare(s2.finalScore, s1.finalScore));
                }"""

new_final_playlist = """                // Take top 25 with massive randomness to make Refresh button provide entirely new songs
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
                }"""

content = content.replace(old_final_playlist, new_final_playlist)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java for multiple combinations and massive refresh shuffling.")
