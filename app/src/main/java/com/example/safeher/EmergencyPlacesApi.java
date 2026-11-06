
package com.example.safeher;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EmergencyPlacesApi {

    public interface PlacesCallback {
        void onSuccess(List<EmergencyPlace> places);
        void onError(String error);
    }

    private static final String[] API_URLS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
    };

    private static String getAddress(JSONObject tags) {

        String[] addressFields = {
                "addr:full",
                "addr:housenumber",
                "addr:street",
                "addr:suburb",
                "addr:neighbourhood",
                "addr:quarter",
                "addr:city",
                "addr:district",
                "addr:state",
                "addr:postcode"
        };

        StringBuilder address = new StringBuilder();

        for (String field : addressFields) {

            String value = tags.optString(field, "").trim();

            if (!value.isEmpty()) {

                if (address.length() > 0) {
                    address.append(", ");
                }

                address.append(value);
            }
        }

        return address.toString();
    }

    public static void fetchNearbyPlaces(
            double latitude,
            double longitude,
            PlacesCallback callback) {

        new Thread(() -> {

            String lastError = "All Overpass servers are unavailable.";

            String query =
                    "[out:json][timeout:25];("
                            + "node(around:5000," + latitude + "," + longitude + ")[amenity=hospital];"
                            + "way(around:5000," + latitude + "," + longitude + ")[amenity=hospital];"
                            + "relation(around:5000," + latitude + "," + longitude + ")[amenity=hospital];"

                            + "node(around:5000," + latitude + "," + longitude + ")[amenity=clinic];"
                            + "way(around:5000," + latitude + "," + longitude + ")[amenity=clinic];"
                            + "relation(around:5000," + latitude + "," + longitude + ")[amenity=clinic];"

                            + "node(around:5000," + latitude + "," + longitude + ")[amenity=police];"
                            + "way(around:5000," + latitude + "," + longitude + ")[amenity=police];"
                            + "relation(around:5000," + latitude + "," + longitude + ")[amenity=police];"
                            + ");out center tags;";

            for (int i = 0; i < API_URLS.length; i++) {

                try {

                    List<EmergencyPlace> places =
                            requestPlaces(API_URLS[i], query);

                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onSuccess(places)
                    );

                    return;

                } catch (Exception e) {

                    lastError = e.getMessage();

                    android.util.Log.w(
                            "SafeHerAPI",
                            "Server " + (i + 1) + " failed: " + lastError
                    );
                }
            }

            final String error = lastError;

            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onError(error)
            );

        }).start();
    }

    private static List<EmergencyPlace> requestPlaces(
            String apiUrl,
            String query) throws Exception {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(apiUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(45000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "SafeHer/1.0 (Android application)"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            String postData =
                    "data=" + URLEncoder.encode(query, "UTF-8");

            try (OutputStream outputStream =
                         connection.getOutputStream()) {

                outputStream.write(
                        postData.getBytes(StandardCharsets.UTF_8)
                );
            }

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {

                throw new Exception(
                        "API Error: " + responseCode
                );
            }

            StringBuilder response = new StringBuilder();

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(
                                 inputStream,
                                 StandardCharsets.UTF_8
                         ))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject jsonObject =
                    new JSONObject(response.toString());

            JSONArray elements =
                    jsonObject.getJSONArray("elements");

            List<EmergencyPlace> places = new ArrayList<>();

            for (int i = 0; i < elements.length(); i++) {

                JSONObject element = elements.getJSONObject(i);

                JSONObject tags = element.optJSONObject("tags");

                if (tags == null) {
                    continue;
                }

                String name =
                        tags.optString("name", "Unnamed Place");

                String type =
                        tags.optString("amenity", "Emergency Service");

                double placeLatitude;
                double placeLongitude;

                if (element.has("lat") && element.has("lon")) {

                    placeLatitude = element.getDouble("lat");
                    placeLongitude = element.getDouble("lon");

                } else if (element.has("center")) {

                    JSONObject center = element.getJSONObject("center");

                    placeLatitude = center.getDouble("lat");
                    placeLongitude = center.getDouble("lon");

                } else {
                    continue;
                }

                String address = getAddress(tags);

                places.add(
                        new EmergencyPlace(
                                name,
                                type,
                                placeLatitude,
                                placeLongitude,
                                address
                        )
                );
            }

            return places;

        } catch (SocketTimeoutException e) {

            throw new Exception("Connection timeout");

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
