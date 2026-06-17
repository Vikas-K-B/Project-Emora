import sys

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip_mode = False
skip_level = 0
current_level = 0

for i, line in enumerate(lines):
    # track bracket levels
    open_c = line.count('{')
    close_c = line.count('}')
    
    level_before = current_level
    current_level += (open_c - close_c)
    
    if "private void fetchItunesSongs(" in line:
        skip_mode = True
        skip_level = level_before
        continue
        
    if "private void renderRecommendations(List<org.json.JSONObject> songs" in line:
        skip_mode = True
        skip_level = level_before
        new_lines.append("""    private void renderRecommendations(List<RecommendationEngine.RankedSong> songs, String stressLevel, String langPref) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(View.GONE);
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Found " + songs.size() + " matches for your " + stressLevel + " state.");

        LinearLayout container = currentContent.findViewById(R.id.playerPlaylistContainer);
        if (container == null) return;
        container.removeAllViews();
        
        if (songs.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No songs found. Please change your preference settings.");
            emptyView.setTextColor(android.graphics.Color.WHITE);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 50);
            container.addView(emptyView);
            return;
        }

        for (RecommendationEngine.RankedSong song : songs) {
            try {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_recommendation, container, false);
                
                setTextIfPresent(itemView, R.id.itemSongName, song.trackName);
                setTextIfPresent(itemView, R.id.itemArtistName, song.artistName);
                setTextIfPresent(itemView, R.id.itemGenre, song.genre);
                setTextIfPresent(itemView, R.id.itemLanguage, langPref.isEmpty() ? "Global" : langPref);
                setTextIfPresent(itemView, R.id.itemReason, song.explanation);
                setTextIfPresent(itemView, R.id.itemMatchScore, song.finalScore + "% Match");
                
                TextView matchScoreView = itemView.findViewById(R.id.itemMatchScore);
                if (matchScoreView != null) {
                    if (song.finalScore >= 90) matchScoreView.setTextColor(android.graphics.Color.parseColor("#a5d6a7"));
                    else if (song.finalScore >= 70) matchScoreView.setTextColor(android.graphics.Color.parseColor("#fff59d"));
                    else matchScoreView.setTextColor(android.graphics.Color.parseColor("#ffcc80"));
                }

                TextView notInterested = itemView.findViewById(R.id.itemNotInterested);
                if (notInterested != null) {
                    notInterested.setOnClickListener(v -> {
                        container.removeView(itemView);
                        android.widget.Toast.makeText(MainActivity.this, "We won't recommend this again.", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }

                android.widget.ImageView albumArt = itemView.findViewById(R.id.itemAlbumArt);
                if (albumArt != null && !song.artworkUrl.isEmpty()) {
                    loadImage(song.artworkUrl, albumArt);
                }

                String finalLang = langPref.isEmpty() ? "Global" : langPref;
                itemView.setOnClickListener(v -> {
                    logUserInteraction(song.genre, finalLang, song.artistName);
                    resolveSpotifyTrack(song.trackId, song.trackName, song.artistName);
                });
                container.addView(itemView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
""")
        continue
        
    if "private void logUserInteraction(String genre, String lang) {" in line:
        skip_mode = True
        skip_level = level_before
        new_lines.append("""    private void logUserInteraction(String genre, String lang, String artist) {
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        try {
            org.json.JSONObject freqMap = new org.json.JSONObject(prefs.getString("user_learned_preferences", "{}"));
            if (!genre.isEmpty() && !"Unknown".equals(genre)) {
                freqMap.put(genre, freqMap.optInt(genre, 0) + 1);
            }
            if (!lang.isEmpty() && !"Global".equals(lang)) {
                freqMap.put(lang, freqMap.optInt(lang, 0) + 1);
            }
            if (!artist.isEmpty() && !"Unknown Artist".equals(artist)) {
                freqMap.put("artist_" + artist.toLowerCase(), freqMap.optInt("artist_" + artist.toLowerCase(), 0) + 1);
            }
            prefs.edit().putString("user_learned_preferences", freqMap.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
""")
        continue
        
    if "fetchItunesSongs(searchQuery, finalStressLevel, prefGenres, prefLanguage, prefArtists, prefType);" in line:
        new_lines.append("""        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(android.view.View.VISIBLE);

        RecommendationEngine.fetchAndRank(this, finalStressLevel, prefLanguage, prefGenres, prefArtists, new RecommendationEngine.RecommendationCallback() {
            @Override
            public void onSuccess(List<RecommendationEngine.RankedSong> recommendations) {
                handler.post(() -> renderRecommendations(recommendations, finalStressLevel, prefLanguage));
            }

            @Override
            public void onFailure(String error, List<RecommendationEngine.RankedSong> cachedRecommendations) {
                handler.post(() -> {
                    android.widget.Toast.makeText(MainActivity.this, error, android.widget.Toast.LENGTH_SHORT).show();
                    if (cachedRecommendations != null && !cachedRecommendations.isEmpty()) {
                        renderRecommendations(cachedRecommendations, finalStressLevel, prefLanguage);
                    } else {
                        if (loading != null) loading.setVisibility(android.view.View.GONE);
                    }
                });
            }
        });
""")
        continue
        
    if "String firstGenre = prefGenres.contains(\",\") ? prefGenres.split(\",\")[0] : prefGenres;" in line:
        continue
    if "String searchQuery = firstGenre.isEmpty() ? moodQuery : moodQuery + \" \" + firstGenre;" in line:
        continue

    if skip_mode:
        if current_level == skip_level:
            skip_mode = False
        continue

    new_lines.append(line)

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
print("Done refactoring recommendations.")
