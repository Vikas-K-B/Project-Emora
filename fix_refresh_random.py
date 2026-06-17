import sys

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_sort = """                // Sort by final score descending
                Collections.sort(rankedSongs, (a, b) -> Integer.compare(b.finalScore, a.finalScore));"""

new_sort = """                // Shuffle before sort to randomize ties and make refresh button give new results
                Collections.shuffle(rankedSongs);
                // Sort by final score descending
                Collections.sort(rankedSongs, (a, b) -> Integer.compare(b.finalScore, a.finalScore));"""

content = content.replace(old_sort, new_sort)

with open('app/src/main/java/com/example/genzmusicapp/RecommendationEngine.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added random shuffle to RecommendationEngine.")
