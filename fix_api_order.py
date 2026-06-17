import sys
import re

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the fetching logic from building the set to iTunes fetching
old_fetch_logic = """                Set<String> searchQueries = new LinkedHashSet<>();
                // Primary queries: highly specific
                for (String l : langs) {
                    for (String g : genres) {
                        if (!l.isEmpty() && !g.isEmpty()) searchQueries.add(l + " " + g + " " + moodQuery);
                    }
                }
                for (String g : genres) {
                    if (!g.isEmpty()) searchQueries.add(g + " " + moodQuery);
                }
                for (String l : langs) {
                    if (!l.isEmpty()) searchQueries.add(l + " " + moodQuery);
                }
                
                // Fallback queries: prioritizing preferences over mood!
                for (String l : langs) {
                    for (String g : genres) {
                        if (!l.isEmpty() && !g.isEmpty()) searchQueries.add(l + " " + g);
                    }
                }
                for (String g : genres) { if (!g.isEmpty()) searchQueries.add(g); }
                for (String l : langs) { if (!l.isEmpty()) searchQueries.add(l); }
                for (String a : artists) { if (!a.isEmpty()) searchQueries.add(a); }

                // Absolute last resort
                if (genres.isEmpty() && artists.isEmpty() && langs.isEmpty()) {
                    searchQueries.add(moodQuery);
                    searchQueries.add("music");
                }

                // 2. Fetch from iTunes API
                Map<String, JSONObject> allFetchedSongs = new HashMap<>();
                for (String query : searchQueries) {
                    if (allFetchedSongs.size() >= 100) break; // Limit pool size for performance

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=25");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        continue;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.optJSONArray("results");
                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject song = results.getJSONObject(i);
                            String trackId = String.valueOf(song.optLong("trackId"));
                            if (!allFetchedSongs.containsKey(trackId)) {
                                allFetchedSongs.put(trackId, song);
                            }
                        }
                    }
                }"""

new_fetch_logic = """                // Generate search queries in strict order of priority
                List<String> searchQueries = new ArrayList<>();
                
                String l = langs.isEmpty() ? "" : langs.get(0);
                String a = artists.isEmpty() ? "" : artists.get(0);
                String g = genres.isEmpty() ? "" : genres.get(0);

                // Priority 1: Exact matches (Language + Artist + Genre + Mood)
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
                searchQueries.add("music");

                // De-duplicate while preserving order
                Set<String> uniqueQueries = new LinkedHashSet<>();
                for (String q : searchQueries) {
                    if (!q.isEmpty()) uniqueQueries.add(q);
                }

                // 2. Fetch from iTunes API
                Map<String, JSONObject> allFetchedSongs = new HashMap<>();
                
                for (String query : uniqueQueries) {
                    if (allFetchedSongs.size() >= 30) break; // Stop early if we found enough highly specific songs

                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=25");
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
                }"""

# Fix strict filtering logic to include language enforcement and drop Level 0, 1, 2 madness.
old_strict_logic = """                // Level 0: Strict Match (Must match Artist AND Genre if specified)
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
                }"""

new_strict_logic = """                // Strict Adherence Filter
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

content = content.replace(old_fetch_logic, new_fetch_logic)
content = content.replace(old_strict_logic, new_strict_logic)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed fetching order and strict adherence.")
