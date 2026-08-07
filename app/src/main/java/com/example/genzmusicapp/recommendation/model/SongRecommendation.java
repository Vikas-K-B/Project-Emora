package com.example.genzmusicapp.recommendation.model;

public class SongRecommendation {
    public String title;
    public String artist;
    public String genre;
    public String reason;
    public int confidence;
    
    // iTunes resolved metadata
    public String artworkUrl;
    public String previewUrl;
    public String trackId;
    
    public SongRecommendation() {}
}
