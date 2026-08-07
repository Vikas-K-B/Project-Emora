package com.example.genzmusicapp.recommendation.model;

import java.util.List;

public class PlaylistResponse {
    public String playlistTitle;
    public String playlistDescription;
    public String generatedForMood;
    public String overallReason;
    public List<SongRecommendation> songs;
    
    public PlaylistResponse() {}
}
