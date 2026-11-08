
package com.example.safeher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EmergencyPlacesApi {

    public interface PlacesCallback {
        void onSuccess(List<EmergencyPlace> places);
        void onError(String error);
    }

    // Replace this with your own Overpass.ch API key.
    private static final String API_KEY = "ovp_NuX5pkDNiN3B80c06wKVfkgwmssulp2n";

    private static final String API_URL =
            "https://api.overpass.ch/v1/" + API_KEY + "/interpreter";

    private static final int SEARCH_RADIUS = 3000;
    private static final int MAX_PER_CATEGORY = 5;

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private static String getAddress(JSONObject tags) {

        String[] fields = {
                "addr:full",
                "addr:housenumber",
                "addr:street",
                "addr:suburb",
                "addr:neighbourhood",
                "addr:city",
                "addr:district"
        };

        StringBuilder address = new StringBuilder();

        for (String field : fields) {
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

            HttpURLConnection connection = null;

            try {
                if (API_KEY.equals("PASTE_YOUR_API_KEY_HERE")
                        || API_KEY.trim().isEmpty()) {
                    throw new Exception("Please add your Overpass.ch API key.");
                }

                String query =
                        "[out:json][timeout:25];(" +
                                "nwr(around:" + SEARCH_RADIUS + "," +
                                latitude + "," + longitude +
                                ")[amenity~\"^(police|clinic)$\"];" +
                                ");out center tags;";

                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
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
                            "Overpass.ch response: " + responseCode
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

                List<EmergencyPlace> policePlaces = new ArrayList<>();
                List<EmergencyPlace> clinicPlaces = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {

                    JSONObject element = elements.getJSONObject(i);
                    JSONObject tags = element.optJSONObject("tags");

                    if (tags == null) {
                        continue;
                    }

                    String type = tags.optString("amenity", "");

                    if (!type.equals("police") && !type.equals("clinic")) {
                        continue;
                    }

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

                    String name = tags.optString(
                            "name",
                            type.equals("police")
                                    ? "Unnamed Police Station"
                                    : "Unnamed Clinic"
                    );

                    String address = getAddress(tags);

                    EmergencyPlace place = new EmergencyPlace(
                            name,
                            type,
                            placeLatitude,
                            placeLongitude,
                            address
                    );

                    if (type.equals("police")) {
                        policePlaces.add(place);
                    } else {
                        clinicPlaces.add(place);
                    }
                }

                // Keep the five nearest places in each category.
                sortByDistance(policePlaces, latitude, longitude);
                sortByDistance(clinicPlaces, latitude, longitude);

                List<EmergencyPlace> finalPlaces = new ArrayList<>();

                addFirstFive(finalPlaces, policePlaces);
                addFirstFive(finalPlaces, clinicPlaces);

                Log.d(
                        "SafeHerAPI",
                        "Police: " + Math.min(policePlaces.size(), MAX_PER_CATEGORY)
                                + ", Clinics: " +
                                Math.min(clinicPlaces.size(), MAX_PER_CATEGORY)
                );

                mainHandler.post(() -> callback.onSuccess(finalPlaces));

            } catch (Exception e) {

                String error = e.getMessage() != null
                        ? e.getMessage()
                        : "Unable to load nearby places.";

                Log.e("SafeHerAPI", "Nearby places error: " + error, e);

                mainHandler.post(() -> callback.onError(error));

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private static void addFirstFive(
            List<EmergencyPlace> destination,
            List<EmergencyPlace> source) {

        int count = Math.min(source.size(), MAX_PER_CATEGORY);

        for (int i = 0; i < count; i++) {
            destination.add(source.get(i));
        }
    }

    private static void sortByDistance(
            List<EmergencyPlace> places,
            double userLatitude,
            double userLongitude) {

        Collections.sort(places, new Comparator<EmergencyPlace>() {
            @Override
            public int compare(EmergencyPlace first, EmergencyPlace second) {

                double firstDistance = distanceInMeters(
                        userLatitude,
                        userLongitude,
                        first.getLatitude(),
                        first.getLongitude()
                );

                double secondDistance = distanceInMeters(
                        userLatitude,
                        userLongitude,
                        second.getLatitude(),
                        second.getLongitude()
                );

                return Double.compare(firstDistance, secondDistance);
            }
        });
    }

    private static double distanceInMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double earthRadius = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
