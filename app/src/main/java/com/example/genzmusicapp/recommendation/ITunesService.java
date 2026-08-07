package com.example.genzmusicapp.recommendation;

import android.util.Log;
import com.example.genzmusicapp.recommendation.model.SongRecommendation;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;

public class ITunesService {
    private static final String TAG = "ITunesService";

    /**
     * Resolves iTunes metadata (artwork, previewUrl, trackId) synchronously.
     * MUST be called from a background thread.
     * Returns true if successfully resolved, false otherwise.
     */
    public boolean resolveSong(SongRecommendation song) {
        try {
            String query = song.title + " " + song.artist;
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=5");
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray results = jsonResponse.optJSONArray("results");

            if (results != null && results.length() > 0) {
                // Find best match or just take the first one
                JSONObject bestMatch = results.getJSONObject(0);
                
                String previewUrl = bestMatch.optString("previewUrl", "");
                if (previewUrl.isEmpty()) {
                    return false; // We need a preview URL for it to be playable
                }

                String artwork = bestMatch.optString("artworkUrl100", "");
                // Upgrade to higher res artwork if possible
                if (!artwork.isEmpty()) {
                    artwork = artwork.replace("100x100bb.jpg", "500x500bb.jpg");
                }
                
                song.previewUrl = previewUrl;
                song.artworkUrl = artwork;
                song.trackId = String.valueOf(bestMatch.optLong("trackId", 0));
                
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to resolve song in iTunes: " + song.title, e);
        }
        return false;
    }
}
