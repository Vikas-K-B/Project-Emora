import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_lang_filter = """                    // 1. STRICT LANGUAGE MATCH
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
                    }"""

new_lang_filter = """                    // 1. CONFIDENCE-BASED LANGUAGE MATCH
                    // iTunes metadata often lacks explicit language tags (especially for "English" or regional Indian songs).
                    // So we only drop the song if it EXPLICITLY contains evidence of a WRONG language.
                    if (!langs.isEmpty() && !langs.get(0).isEmpty()) {
                        java.util.List<String> knownLanguages = java.util.Arrays.asList("telugu", "tamil", "kannada", "malayalam", "hindi", "punjabi", "bengali", "marathi", "gujarati", "spanish", "french", "german", "korean", "japanese", "english", "bollywood", "tollywood", "kollywood", "sandalwood");
                        for (String known : knownLanguages) {
                            if (rawData.contains(known)) {
                                boolean userWantsThis = false;
                                for (String userLang : langs) {
                                    if (userLang.toLowerCase().contains(known) || known.contains(userLang.toLowerCase())) {
                                        userWantsThis = true;
                                        break;
                                    }
                                    // Treat sandalwood as kannada, bollywood as hindi, etc.
                                    if (userLang.equalsIgnoreCase("kannada") && known.equals("sandalwood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("hindi") && known.equals("bollywood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("telugu") && known.equals("tollywood")) userWantsThis = true;
                                    if (userLang.equalsIgnoreCase("tamil") && known.equals("kollywood")) userWantsThis = true;
                                }
                                if (!userWantsThis) {
                                    drop = true;
                                    break;
                                }
                            }
                        }
                    }"""

content = content.replace(old_lang_filter, new_lang_filter)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Reverted Language Filtering to confidence-based to fix starvation.")
