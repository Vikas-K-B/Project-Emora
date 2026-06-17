import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("private static List<RankedSong> cachedRecommendations = new ArrayList<>();", "public static List<RankedSong> cachedRecommendations = new ArrayList<>();")

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("                    generateRecommendations();", "                    generateRecommendations(false);")

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed access level and compilation errors")
