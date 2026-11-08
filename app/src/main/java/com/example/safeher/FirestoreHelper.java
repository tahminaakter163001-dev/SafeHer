package com.example.safeher;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore firestore;

    public FirestoreHelper() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Save user profile
    public void saveUserProfile(
            String name,
            String email,
            OnProfileSaveListener listener) {

        FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("No authenticated Firebase user found.");
            return;
        }

        String uid = firebaseUser.getUid();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", uid);
        userProfile.put("name", name);
        userProfile.put("email", email);
        userProfile.put("createdAt", Timestamp.now());

        firestore.collection("users")
                .document(uid)
                .set(userProfile)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(error ->
                        listener.onFailure(error.getMessage()));
    }

    // Check whether Firebase user matches the current app session
    private String getMatchingFirebaseUid(String expectedEmail) {

        FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            return null;
        }

        if (!firebaseUser.getEmail().equalsIgnoreCase(expectedEmail)) {
            return null;
        }

        return firebaseUser.getUid();
    }

    // Add or update emergency contact in Firestore
    public void saveEmergencyContact(
            String expectedEmail,
            EmergencyContact contact,
            OnSyncListener listener) {

        String uid = getMatchingFirebaseUid(expectedEmail);

        if (uid == null) {
            listener.onFailure(
                    "No matching authenticated Firebase account. Cloud sync unavailable.");
            return;
        }

        Map<String, Object> contactData = new HashMap<>();

        contactData.put("localContactId", contact.getId());
        contactData.put("name", contact.getName());
        contactData.put("phoneNumber", contact.getPhoneNumber());
        contactData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(uid)
                .collection("emergencyContacts")
                .document(String.valueOf(contact.getId()))
                .set(contactData)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(error ->
                        listener.onFailure(error.getMessage()));
    }

    // Delete emergency contact from Firestore
    public void deleteEmergencyContact(
            String expectedEmail,
            int contactId,
            OnSyncListener listener) {

        String uid = getMatchingFirebaseUid(expectedEmail);

        if (uid == null) {
            listener.onFailure(
                    "No matching authenticated Firebase account. Cloud sync unavailable.");
            return;
        }

        firestore.collection("users")
                .document(uid)
                .collection("emergencyContacts")
                .document(String.valueOf(contactId))
                .delete()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(error ->
                        listener.onFailure(error.getMessage()));
    }

    public interface OnProfileSaveListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnSyncListener {
        void onSuccess();
        void onFailure(String error);
    }
    // Update user profile in Firestore
    public void updateUserProfile(
            String name,
            String email,
            OnProfileSaveListener listener) {

        FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("No authenticated Firebase user found.");
            return;
        }

        // Update Firebase Authentication email if changed
        Runnable saveToFirestore = () -> {

            String uid = firebaseUser.getUid();

            Map<String, Object> profile = new HashMap<>();
            profile.put("uid", uid);
            profile.put("name", name);
            profile.put("email", email);
            profile.put("updatedAt", FieldValue.serverTimestamp());

            firestore.collection("users")
                    .document(uid)
                    .set(profile, SetOptions.merge())
                    .addOnSuccessListener(unused -> listener.onSuccess())
                    .addOnFailureListener(error ->
                            listener.onFailure(
                                    "Firestore error: " + error.getMessage()
                            ));
        };

        String currentEmail = firebaseUser.getEmail();

        if (currentEmail != null &&
                currentEmail.equalsIgnoreCase(email)) {

            saveToFirestore.run();

        } else {

            firebaseUser.updateEmail(email)
                    .addOnSuccessListener(unused -> saveToFirestore.run())
                    .addOnFailureListener(error ->
                            listener.onFailure(
                                    "Email update failed: " + error.getMessage()
                            ));
        }
    }
}
