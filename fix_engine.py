import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix search queries to prioritize language+genre without mood
old_queries_code = """                Set<String> searchQueries = new LinkedHashSet<>();
                // Primary queries: highly specific
                for (String g : genres) {
                    if (!g.isEmpty()) searchQueries.add(g + " " + moodQuery);
                }
                for (String a : artists) {
                    if (!a.isEmpty()) searchQueries.add(a + " " + moodQuery);
                }
                for (String l : langs) {
                    if (!l.isEmpty()) searchQueries.add(l + " " + moodQuery);
                }
                
                // Fallback queries: broader
                if (genres.isEmpty() && artists.isEmpty() && langs.isEmpty()) {
                    searchQueries.add(moodQuery);
                    searchQueries.add("music");
                } else {
                    for (String g : genres) { if (!g.isEmpty()) searchQueries.add(g); }
                    for (String a : artists) { if (!a.isEmpty()) searchQueries.add(a); }
                    for (String l : langs) { if (!l.isEmpty()) searchQueries.add(l); }
                }"""

new_queries_code = """                Set<String> searchQueries = new LinkedHashSet<>();
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
                }"""

content = content.replace(old_queries_code, new_queries_code)

# Fix genre matching
old_score_code = """        // Genre Match (Max 25)
        for (String g : userGenres) {
            if (!g.isEmpty() && trackGenre.contains(g.toLowerCase())) {
                song.genreScore = 25;
                break;
            }
        }"""

new_score_code = """        // Genre Match (Max 25)
        for (String g : userGenres) {
            if (!g.isEmpty() && (trackGenre.contains(g.toLowerCase()) || g.toLowerCase().contains(trackGenre))) {
                song.genreScore = 25;
                break;
            }
        }"""

content = content.replace(old_score_code, new_score_code)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated RecommendationEngine.java")
