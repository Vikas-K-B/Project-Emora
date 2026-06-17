import sys

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Add fields
old_fields = """    private String lastRecommendedMood = "";"""
new_fields = """    private String lastRecommendedMood = "";
    private int devModeTapCount = 0;
    private long lastDevModeTapTime = 0;"""

content = content.replace(old_fields, new_fields)

# Replace topBrand listener
old_listener = """        TextView topBrand = content.findViewById(R.id.topBrand);
        if (topBrand != null) {
            topBrand.setOnClickListener(view -> showScreen(SCREEN_HOME));
        }"""
new_listener = """        TextView topBrand = content.findViewById(R.id.topBrand);
        if (topBrand != null) {
            topBrand.setOnClickListener(view -> {
                long now = System.currentTimeMillis();
                if (now - lastDevModeTapTime > 1000) {
                    devModeTapCount = 0;
                }
                lastDevModeTapTime = now;
                devModeTapCount++;
                
                SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
                boolean isDevMode = prefs.getBoolean("developer_mode", false);
                
                if (isDevMode) {
                    showDiagnosticsDialog();
                } else {
                    if (devModeTapCount >= 7) {
                        prefs.edit().putBoolean("developer_mode", true).apply();
                        Toast.makeText(this, "Developer Mode Unlocked!", Toast.LENGTH_SHORT).show();
                        showDiagnosticsDialog();
                    } else if (devModeTapCount >= 4) {
                        Toast.makeText(this, "Tap " + (7 - devModeTapCount) + " more times to unlock developer mode.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }"""

content = content.replace(old_listener, new_listener)

# Add showDiagnosticsDialog method
old_method = """    private void showScreen(String screen) {"""
new_method = """    private void showDiagnosticsDialog() {
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String statsJson = prefs.getString("diagnostics_json", "{}");
        
        try {
            org.json.JSONObject stats = new org.json.JSONObject(statsJson);
            StringBuilder msg = new StringBuilder();
            
            org.json.JSONObject artistCounts = stats.optJSONObject("artistCounts");
            if (artistCounts != null) {
                msg.append("--- Playlist Balance ---\\n");
                java.util.Iterator<String> keys = artistCounts.keys();
                while (keys.hasNext()) {
                    String artist = keys.next();
                    msg.append(artist).append(": ").append(artistCounts.getInt(artist)).append(" songs\\n");
                }
                msg.append("\\n");
            }
            
            org.json.JSONArray scores = stats.optJSONArray("scores");
            if (scores != null && scores.length() > 0) {
                msg.append("--- Top Tracks Breakdown ---\\n");
                for (int i = 0; i < Math.min(10, scores.length()); i++) {
                    org.json.JSONObject s = scores.getJSONObject(i);
                    msg.append(s.getInt("rank")).append(". ").append(s.getString("name")).append("\\n");
                    msg.append("   Score: ").append(s.getInt("totalScore"))
                       .append(" (M:").append(s.getInt("mood"))
                       .append(" L:").append(s.getInt("lang"))
                       .append(" A:").append(s.getInt("artistScore"))
                       .append(" G:").append(s.getInt("genre"))
                       .append(" H:").append(s.getInt("history"))
                       .append(" P:-").append(s.getInt("penalty")).append(")\\n\\n");
                }
            } else {
                msg.append("No recent recommendations found.");
            }
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Developer Diagnostics")
                .setMessage(msg.toString())
                .setPositiveButton("Close", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Diagnostics unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void showScreen(String screen) {"""

content = content.replace(old_method, new_method)

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added diagnostics UI to MainActivity.java")
