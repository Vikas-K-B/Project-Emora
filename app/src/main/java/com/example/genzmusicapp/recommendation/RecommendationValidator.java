package com.example.genzmusicapp.recommendation;

import com.example.genzmusicapp.recommendation.model.PlaylistResponse;
import com.example.genzmusicapp.recommendation.model.SongRecommendation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RecommendationValidator {
    private static final int MIN_REQUIRED_SONGS = 3;
    private final ITunesService iTunesService;

    public RecommendationValidator(ITunesService iTunesService) {
        this.iTunesService = iTunesService;
    }

    /**
     * Validates the playlist. Removes duplicates and unresolvable songs.
     * @return true if the playlist is valid (has enough songs), false if it should be retried or fallback.
     */
    public boolean validate(PlaylistResponse response) {
        if (response == null || response.songs == null || response.songs.isEmpty()) {
            return false;
        }

        List<SongRecommendation> validSongs = new ArrayList<>();
        Set<String> seenTracks = new HashSet<>();

        for (SongRecommendation song : response.songs) {
            if (song.title == null || song.artist == null) {
                continue;
            }

            // Create a unique key for deduplication
            String uniqueKey = (song.title.trim().toLowerCase() + " - " + song.artist.trim().toLowerCase());
            
            if (seenTracks.contains(uniqueKey)) {
                continue; // Skip duplicate
            }

            // Attempt to resolve on iTunes
            boolean resolved = iTunesService.resolveSong(song);
            if (resolved) {
                seenTracks.add(uniqueKey);
                validSongs.add(song);
            }
        }

        // Replace the raw list with the strictly valid list
        response.songs = validSongs;

        // Ensure we have a minimum number of valid songs
        return validSongs.size() >= MIN_REQUIRED_SONGS;
    }
}
