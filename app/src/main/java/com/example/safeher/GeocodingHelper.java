
package com.example.safeher;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeocodingHelper {

    public interface AddressCallback {
        void onSuccess(String address);
        void onError(String error);
    }

    public static void getAddress(
            double latitude,
            double longitude,
            AddressCallback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                String apiUrl =
                        "https://nominatim.openstreetmap.org/reverse"
                                + "?format=jsonv2"
                                + "&lat=" + latitude
                                + "&lon=" + longitude
                                + "&zoom=18"
                                + "&addressdetails=1";

                URL url = new URL(apiUrl);

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "User-Agent",
                        "SafeHer/1.0 (Android application)"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode == 200) {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream(),
                                            StandardCharsets.UTF_8
                                    )
                            );

                    StringBuilder response =
                            new StringBuilder();

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    JSONObject jsonObject =
                            new JSONObject(response.toString());

                    String address =
                            jsonObject.optString(
                                    "display_name",
                                    ""
                            );

                    if (address.isEmpty()) {
                        throw new Exception("Address not found.");
                    }

                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onSuccess(address)
                    );

                } else {
                    throw new Exception(
                            "API Error: " + responseCode
                    );
                }

            } catch (Exception e) {

                String errorMessage = e.getMessage();

                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onError(errorMessage)
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
