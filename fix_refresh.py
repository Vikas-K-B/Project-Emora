import sys

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_code = """    private void configurePlayerScreen(View content) {
        TextView refreshButton = content.findViewById(R.id.playerAiRecButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> generateRecommendations());
        }
        
        generateRecommendations();
    }

    private void generateRecommendations() {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Syncing biometrics and preferences...");
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String prefLanguage = prefs.getString("prefLanguage", "").trim();
        String prefGenres = prefs.getString("prefGenres", "").trim();
        
        if (prefLanguage.isEmpty() && prefGenres.isEmpty()) {
            if (subtitle != null) subtitle.setText("Please set your preferences in Wellness first.");
            return;
        }

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        
        if (bpm <= 0) {
            if (subtitle != null) subtitle.setText("Awaiting stable biometric readings...");
            return;
        }

        String stressLevel = currentMoodLabel;
        String moodQuery = "pop";
        if (stressLevel.equals("Stressed")) moodQuery = "relaxing chill";
        else if (stressLevel.equals("Relaxed") || stressLevel.equals("Calm")) moodQuery = "acoustic soft";
        else if (stressLevel.equals("Energetic")) moodQuery = "upbeat energy";

        String prefArtists = prefs.getString("prefArtists", "").trim();
        String prefType = prefs.getString("prefMusicType", "").trim();

        String finalStressLevel = stressLevel;
        lastRecommendedMood = finalStressLevel;
        if (subtitle != null) subtitle.setText("Curating " + finalStressLevel + " recommendations...");
        
        // Use a broader query for iTunes to ensure we get results (iTunes 'term' acts as strict AND)
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(android.view.View.VISIBLE);

        RecommendationEngine.fetchAndRank(this, finalStressLevel, prefLanguage, prefGenres, prefArtists, new RecommendationEngine.RecommendationCallback() {"""

new_code = """    private void configurePlayerScreen(View content) {
        TextView refreshButton = content.findViewById(R.id.playerAiRecButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> generateRecommendations(true));
        }
        
        generateRecommendations(false);
    }

    private void generateRecommendations(boolean forceRefresh) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Syncing biometrics and preferences...");
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String prefLanguage = prefs.getString("prefLanguage", "").trim();
        String prefGenres = prefs.getString("prefGenres", "").trim();
        
        if (prefLanguage.isEmpty() && prefGenres.isEmpty()) {
            if (subtitle != null) subtitle.setText("Please set your preferences in Wellness first.");
            return;
        }

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        
        if (bpm <= 0) {
            if (subtitle != null) subtitle.setText("Awaiting stable biometric readings...");
            return;
        }

        String stressLevel = currentMoodLabel;
        String moodQuery = "pop";
        if (stressLevel.equals("Stressed")) moodQuery = "relaxing chill";
        else if (stressLevel.equals("Relaxed") || stressLevel.equals("Calm")) moodQuery = "acoustic soft";
        else if (stressLevel.equals("Energetic")) moodQuery = "upbeat energy";

        String prefArtists = prefs.getString("prefArtists", "").trim();
        String prefType = prefs.getString("prefMusicType", "").trim();

        String finalStressLevel = stressLevel;
        
        // --- NEW LOGIC: Only fetch if forced or mood changed ---
        if (!forceRefresh && RecommendationEngine.cachedRecommendations != null && !RecommendationEngine.cachedRecommendations.isEmpty() && finalStressLevel.equals(lastRecommendedMood)) {
            renderRecommendations(RecommendationEngine.cachedRecommendations, finalStressLevel, prefLanguage);
            return;
        }
        // -------------------------------------------------------

        lastRecommendedMood = finalStressLevel;
        if (subtitle != null) subtitle.setText("Curating " + finalStressLevel + " recommendations...");
        
        // Use a broader query for iTunes to ensure we get results (iTunes 'term' acts as strict AND)
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(android.view.View.VISIBLE);

        RecommendationEngine.fetchAndRank(this, finalStressLevel, prefLanguage, prefGenres, prefArtists, new RecommendationEngine.RecommendationCallback() {"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated MainActivity to cache player state between tabs.")
