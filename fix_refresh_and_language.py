import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_queries = """                // Priority 1: Exact matches (Language + Artist + Genre + Mood)
                if (!l.isEmpty() && !a.isEmpty() && !g.isEmpty()) {
                    searchQueries.add((l + " " + a + " " + g + " " + moodQuery).trim());
                    searchQueries.add((l + " " + a + " " + g).trim());
                }
                
                // Priority 2: Missing one parameter
                if (!l.isEmpty() && !a.isEmpty()) {
                    searchQueries.add((l + " " + a + " " + moodQuery).trim());
                    searchQueries.add((l + " " + a).trim());
                }
                if (!a.isEmpty() && !g.isEmpty()) {
                    searchQueries.add((a + " " + g + " " + moodQuery).trim());
                    searchQueries.add((a + " " + g).trim());
                }
                if (!l.isEmpty() && !g.isEmpty()) {
                    searchQueries.add((l + " " + g + " " + moodQuery).trim());
                    searchQueries.add((l + " " + g).trim());
                }

                // Priority 3: Base single preferences
                if (!a.isEmpty()) {
                    searchQueries.add((a + " " + moodQuery).trim());
                    searchQueries.add(a);
                }
                if (!l.isEmpty()) {
                    searchQueries.add((l + " " + moodQuery).trim());
                    searchQueries.add(l);
                }
                if (!g.isEmpty()) {
                    searchQueries.add((g + " " + moodQuery).trim());
                    searchQueries.add(g);
                }
                
                // Priority 4: Fallback
                searchQueries.add(moodQuery);
                searchQueries.add("music");"""

new_queries = """                // STRICT QUERY GENERATION (Fixes wrong language fetching)
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

content = content.replace(old_queries, new_queries)

old_fetch_loop = """                for (String query : uniqueQueries) {
                    if (allFetchedSongs.size() >= 30) break; // Stop early if we found enough highly specific songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=25");"""

new_fetch_loop = """                for (String query : uniqueQueries) {
                    if (allFetchedSongs.size() >= 100) break; // Fetch a large pool so refresh gives new songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=50");"""

content = content.replace(old_fetch_loop, new_fetch_loop)

old_final_playlist = """                // Take top 25
                List<RankedSong> finalPlaylist = diversePlaylist.size() > 25 ? diversePlaylist.subList(0, 25) : diversePlaylist;"""

new_final_playlist = """                // Take top 25 with randomness to make Refresh button provide fresh songs
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

content = content.replace(old_final_playlist, new_final_playlist)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java for strict language queries and refresh shuffling.")
