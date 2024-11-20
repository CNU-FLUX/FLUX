package com.flux.warning.service;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapService {
    @Value("${google.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient();

    public String getDirections(String origin, String destination) throws Exception {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s",
                origin, destination, apiKey
        );

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch directions: " + response);
            }
            return response.body().string();
        }
    }

    public String getCurrentLocationInfo(double lat, double lng) throws Exception {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                lat, lng, apiKey
        );

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch location info: " + response);
            }
            return response.body().string();
        }
    }
}
