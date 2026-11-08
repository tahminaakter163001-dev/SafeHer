
package com.example.safeher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText;
    private TextView emailText;
    private TextView phoneText;
    private TextView addressText;
    private TextView dobText;
    private TextView bloodGroupText;

    private ImageView profileImage;

    private Button deleteAccountButton;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    private static final int GALLERY_PERMISSION_REQUEST = 101;
    private static final int GALLERY_IMAGE_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameText = findViewById(R.id.profileName);
        emailText = findViewById(R.id.profileEmail);
        phoneText = findViewById(R.id.profilePhone);
        addressText = findViewById(R.id.profileAddress);
        dobText = findViewById(R.id.profileDob);
        bloodGroupText = findViewById(R.id.profileBloodGroup);

        profileImage = findViewById(R.id.profileImage);

        Button editButton = findViewById(R.id.editProfileButton);
        Button backButton = findViewById(R.id.backButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        loadProfile();
        loadSavedProfileImage();

        profileImage.setOnClickListener(v -> checkGalleryPermission());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    EditProfileActivity.class
            );
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());

        deleteAccountButton.setOnClickListener(v ->
                showDeleteConfirmation()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && sessionManager != null
                && sessionManager.isLoggedIn()) {

            loadProfile();
            loadSavedProfileImage();
        }
    }

    // Get a separate image file for each user
    private File getProfileImageFile() {

        int userId = sessionManager.getUserId();

        return new File(
                getFilesDir(),
                "profile_picture_" + userId + ".jpg"
        );
    }

    private void checkGalleryPermission() {

        String permission;

        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {

            openGallery();

        } else {

            requestPermissions(
                    new String[]{permission},
                    GALLERY_PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == GALLERY_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openGallery();

            } else {

                Toast.makeText(
                        this,
                        "Gallery permission is required to select a profile photo.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void openGallery() {

        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );

        intent.setType("image/*");

        startActivityForResult(intent, GALLERY_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            saveProfileImage(data.getData());
        }
    }

    private void saveProfileImage(Uri imageUri) {

        FileOutputStream outputStream = null;
        InputStream inputStream = null;

        try {

            inputStream = getContentResolver().openInputStream(imageUri);

            if (inputStream == null) {
                throw new Exception("Unable to open selected image.");
            }

            File imageFile = getProfileImageFile();

            outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            loadSavedProfileImage();

            Toast.makeText(
                    this,
                    "Profile picture updated successfully!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not save profile picture.",
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();

        } finally {

            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadSavedProfileImage() {

        File imageFile = getProfileImageFile();

        if (imageFile.exists()) {

            Bitmap bitmap = BitmapFactory.decodeFile(
                    imageFile.getAbsolutePath()
            );

            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            } else {
                profileImage.setImageResource(
                        R.drawable.ic_launcher_foreground
                );
            }

        } else {

            profileImage.setImageResource(
                    R.drawable.ic_launcher_foreground
            );
        }
    }

    private void loadProfile() {

        int userId = sessionManager.getUserId();

        User user = databaseHelper.getUserById(userId);

        if (user == null) {

            Toast.makeText(
                    this,
                    "Unable to load profile. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            sessionManager.clearSession();
            goToLogin();
            return;
        }

        nameText.setText(displayValue(user.getFullName()));
        emailText.setText(displayValue(user.getEmail()));
        phoneText.setText(displayValue(user.getPhoneNumber()));
        addressText.setText(displayValue(user.getAddress()));
        dobText.setText(displayValue(user.getDateOfBirth()));
        bloodGroupText.setText(displayValue(user.getBloodGroup()));

        sessionManager.saveUser(
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    private String displayValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }

        return value;
    }

    // Show permanent deletion confirmation
    private void showDeleteConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage(
                        "Are you sure you want to permanently delete your SafeHer account?\n\n"
                                + "Your profile, emergency contacts and saved data will be deleted. "
                                + "This action cannot be undone."
                )
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Continue", (dialog, which) ->
                        showPasswordDialog()
                )
                .show();
    }

    // Ask for password before deleting
    private void showPasswordDialog() {

        EditText passwordInput = new EditText(this);

        passwordInput.setHint("Enter your current password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        passwordInput.setPadding(
                padding, padding, padding, padding
        );

        AlertDialog passwordDialog = new AlertDialog.Builder(this)
                .setTitle("Verify Your Identity")
                .setMessage("Enter your password to continue.")
                .setView(passwordInput)
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Verify & Delete", null)
                .create();

        passwordDialog.setOnShowListener(dialog -> {

            passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String password = passwordInput.getText()
                                .toString();

                        if (password.isEmpty()) {

                            passwordInput.setError(
                                    "Password is required"
                            );
                            return;
                        }

                        String email = sessionManager.getUserEmail();

                        if (email == null || email.trim().isEmpty()) {

                            Toast.makeText(
                                    this,
                                    "Unable to identify your account.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        deleteAccountButton.setEnabled(false);
                        passwordDialog.dismiss();

                        if (databaseHelper.isFirebaseAccount(email)) {

                            deleteFirebaseAccount(email, password);

                        } else {

                            deleteLocalAccount(email, password);
                        }
                    });
        });

        passwordDialog.show();
    }

    // Delete a local SQLite-only account
    private void deleteLocalAccount(
            String email,
            String password
    ) {

        User authenticatedUser =
                databaseHelper.getAuthenticatedUser(email, password);

        if (authenticatedUser == null) {

            deleteAccountButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "Incorrect password. Account was not deleted.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        stopLocationTracking();

        boolean deleted =
                databaseHelper.deleteLocalAccountByEmail(email);

        if (!deleted) {

            deleteAccountButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "Unable to delete local account. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        finishAccountDeletion(false);
    }

    // Re-authenticate Firebase account before permanent deletion
    private void deleteFirebaseAccount(
            String email,
            String password
    ) {

        FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null
                || firebaseUser.getEmail() == null
                || !firebaseUser.getEmail().equalsIgnoreCase(email)) {

            deleteAccountButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "No matching Firebase account is signed in.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(email, password);

        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    stopLocationTracking();

                    deleteFirebaseCloudData(email, firebaseUser);

                })
                .addOnFailureListener(error -> {

                    deleteAccountButton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Password verification failed: "
                                    + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Delete Firestore first, then Realtime Database
    private void deleteFirebaseCloudData(
            String email,
            FirebaseUser firebaseUser
    ) {

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        firestoreHelper.deleteCurrentUserData(
                email,
                new FirestoreHelper.OnSyncListener() {

                    @Override
                    public void onSuccess() {

                        RealtimeDatabaseHelper realtimeHelper =
                                new RealtimeDatabaseHelper();

                        realtimeHelper.deleteCurrentUserData(
                                email,
                                new RealtimeDatabaseHelper.RealtimeDeleteListener() {

                                    @Override
                                    public void onSuccess() {

                                        deleteFirebaseAuthAccount(
                                                firebaseUser
                                        );
                                    }

                                    @Override
                                    public void onFailure(String error) {

                                        showDeletionError(
                                                "Realtime Database deletion failed: "
                                                        + error
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(String error) {

                        showDeletionError(
                                "Firestore deletion failed: " + error
                        );
                    }
                }
        );
    }

    // Delete Firebase Authentication account last
    private void deleteFirebaseAuthAccount(
            FirebaseUser firebaseUser
    ) {

        firebaseUser.delete()
                .addOnSuccessListener(unused -> {

                    String email = firebaseUser.getEmail();

                    boolean localDeleted =
                            databaseHelper.deleteLocalAccountByEmail(email);

                    if (!localDeleted) {

                        Toast.makeText(
                                this,
                                "Cloud account deleted, but local cleanup failed.",
                                Toast.LENGTH_LONG
                        ).show();

                        cleanupSessionAndExit();
                        return;
                    }

                    finishAccountDeletion(true);
                })
                .addOnFailureListener(error -> {

                    showDeletionError(
                            "Firebase account deletion failed: "
                                    + error.getMessage()
                    );
                });
    }

    // Stop active foreground location tracking
    private void stopLocationTracking() {

        Intent serviceIntent = new Intent(
                ProfileActivity.this,
                LocationTrackingService.class
        );

        stopService(serviceIntent);
    }

    // Remove this user's local profile picture
    private void deleteLocalProfilePicture() {

        File imageFile = getProfileImageFile();

        if (imageFile.exists() && !imageFile.delete()) {
            imageFile.deleteOnExit();
        }
    }

    // Finish local and Firebase account deletion
    private void finishAccountDeletion(boolean firebaseAccount) {

        deleteLocalProfilePicture();

        if (firebaseAccount) {
            FirebaseAuth.getInstance().signOut();
        }

        cleanupSessionAndExit();

        Toast.makeText(
                this,
                "Your SafeHer account has been permanently deleted.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void cleanupSessionAndExit() {

        sessionManager.clearSession();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showDeletionError(String message) {

        deleteAccountButton.setEnabled(true);

        new AlertDialog.Builder(this)
                .setTitle("Account Deletion Failed")
                .setMessage(
                        message
                                + "\n\nYour Firebase Authentication account "
                                + "has not been deleted. You can try again."
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void goToLogin() {

        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
