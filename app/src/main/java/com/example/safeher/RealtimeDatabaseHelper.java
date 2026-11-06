package com.example.safeher;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class RealtimeDatabaseHelper {

    private final DatabaseReference databaseReference;

    public RealtimeDatabaseHelper() {

        databaseReference = FirebaseDatabase
                .getInstance("https://safeher-46172-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");
    }

    private String getCurrentUserId() {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return null;
        }

        return user.getUid();
    }

    public void startSOS(double latitude, double longitude) {

        String userId = getCurrentUserId();

        if (userId == null) return;

        Map<String, Object> sosData = new HashMap<>();

        sosData.put("status", "ACTIVE");
        sosData.put("latitude", latitude);
        sosData.put("longitude", longitude);
        sosData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.child(userId)
                .child("liveSOS")
                .setValue(sosData);
    }

    public void updateLocation(double latitude, double longitude) {

        String userId = getCurrentUserId();

        if (userId == null) return;

        DatabaseReference locationReference =
                databaseReference.child(userId)
                        .child("liveSOS");

        locationReference.child("latitude").setValue(latitude);
        locationReference.child("longitude").setValue(longitude);
        locationReference.child("timestamp")
                .setValue(ServerValue.TIMESTAMP);
    }

    public void stopSOS() {

        String userId = getCurrentUserId();

        if (userId == null) return;

        databaseReference.child(userId)
                .child("liveSOS")
                .child("status")
                .setValue("RESOLVED");
    }
}
