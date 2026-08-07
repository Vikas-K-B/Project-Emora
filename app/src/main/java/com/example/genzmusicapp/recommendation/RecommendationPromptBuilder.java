package com.example.genzmusicapp.recommendation;

import java.util.List;

public class RecommendationPromptBuilder {

    public static String buildSystemPrompt() {
        return "You are an intelligent music recommendation assistant.\n" +
                "You must recommend only real commercially released songs. Never invent songs or artists.\n" +
                "Respect the user's preferences.\n" +
                "Your output MUST be strictly valid JSON matching the exact schema requested, with no markdown formatting, no code blocks, and no extra text.";
    }

    public static String buildUserPrompt(String mood, String activity, String timeOfDay, 
                                         String languages, String genres, String artists,
                                         List<String> recent, List<String> skipped, List<String> favorites) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a playlist of 5 songs based on the following context:\n\n");
        
        prompt.append("### Context ###\n");
        prompt.append("- Detected Mood: ").append(mood).append("\n");
        if (activity != null && !activity.isEmpty()) prompt.append("- Activity: ").append(activity).append("\n");
        if (timeOfDay != null && !timeOfDay.isEmpty()) prompt.append("- Time of Day: ").append(timeOfDay).append("\n");
        
        prompt.append("\n### User Preferences ###\n");
        prompt.append("- Preferred Languages: ").append(languages).append("\n");
        prompt.append("- Preferred Genres: ").append(genres).append("\n");
        prompt.append("- Favorite Artists: ").append(artists).append("\n");
        
        prompt.append("\n### Listening History ###\n");
        if (recent != null && !recent.isEmpty()) prompt.append("- Recently Played: ").append(String.join(", ", recent)).append("\n");
        if (skipped != null && !skipped.isEmpty()) prompt.append("- Skipped Songs: ").append(String.join(", ", skipped)).append("\n");
        if (favorites != null && !favorites.isEmpty()) prompt.append("- Favorite Songs: ").append(String.join(", ", favorites)).append("\n");

        prompt.append("\n### Prioritization Rules ###\n");
        prompt.append("1. Priority 1: Preferred Language\n");
        prompt.append("2. Priority 2: Preferred Genres\n");
        prompt.append("3. Priority 3: Favorite Artists\n");
        prompt.append("4. Priority 4: Current Mood\n");
        prompt.append("5. Priority 5: Current Activity\n");
        prompt.append("6. Priority 6: Time of Day\n");
        
        prompt.append("\n### Goal ###\n");
        if ("Stressed".equalsIgnoreCase(mood)) {
            prompt.append("Recommend calming songs. Avoid high-BPM music.\n");
        } else if ("Sad".equalsIgnoreCase(mood)) {
            prompt.append("Gradually improve the user's mood. Avoid extremely depressing songs.\n");
        } else if ("Happy".equalsIgnoreCase(mood)) {
            prompt.append("Maintain the user's positive mood.\n");
        } else if ("Calm".equalsIgnoreCase(mood) || "Resting".equalsIgnoreCase(mood) || "Relaxed".equalsIgnoreCase(mood)) {
            prompt.append("Recommend peaceful and relaxing music.\n");
        } else if ("Energetic".equalsIgnoreCase(mood)) {
            prompt.append("Recommend energetic songs suitable for the user's activity.\n");
        } else {
            prompt.append("Provide a balanced playlist matching their preferences.\n");
        }

        prompt.append("\n### Conflict Resolution ###\n");
        prompt.append("If there is a conflict between preferences and mood, balance both instead of ignoring either. ");
        prompt.append("For example, if the user is stressed but prefers high-energy genres, find the most calming/melodic songs within those genres or languages.\n");

        prompt.append("\n### Diversity Rules ###\n");
        prompt.append("- Maximum 2 songs from the same artist.\n");
        prompt.append("- Recommend songs from different albums whenever possible.\n");
        prompt.append("- Avoid duplicate songs.\n");
        prompt.append("- Do not repeat any songs from the 'Recently Played' list.\n");

        prompt.append("\n### Output Format ###\n");
        prompt.append("Return ONLY a JSON object in the exact following format. Do not use Markdown block syntax (```json). Just the raw JSON string.\n");
        prompt.append("{\n");
        prompt.append("  \"playlistTitle\": \"<Catchy title>\",\n");
        prompt.append("  \"playlistDescription\": \"<Short description>\",\n");
        prompt.append("  \"generatedForMood\": \"").append(mood).append("\",\n");
        prompt.append("  \"overallReason\": \"<Explanation of why this playlist was created for the current mood and preferences>\",\n");
        prompt.append("  \"songs\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"<Song Title>\",\n");
        prompt.append("      \"artist\": \"<Artist Name>\",\n");
        prompt.append("      \"genre\": \"<Genre>\",\n");
        prompt.append("      \"reason\": \"<Why this specific song matches>\",\n");
        prompt.append("      \"confidence\": <0-100>\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }
    
    public static String buildRetryPrompt() {
        return "The previous recommendations could not be validated. " +
               "Recommend different real commercially released songs. " +
               "Do not repeat previous songs. " +
               "Ensure the songs are well-known and likely to exist in the iTunes catalog. " +
               "Return valid JSON only without markdown formatting.";
    }
}
