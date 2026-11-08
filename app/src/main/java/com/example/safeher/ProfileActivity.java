
package com.example.safeher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    private static final int GALLERY_PERMISSION_REQUEST = 101;
    private static final int GALLERY_IMAGE_REQUEST = 102;

    private static final String PROFILE_IMAGE_FILE =
            "profile_picture.jpg";

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
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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

        try {

            InputStream inputStream =
                    getContentResolver().openInputStream(imageUri);

            if (inputStream == null) {
                throw new Exception("Unable to open selected image.");
            }

            File imageFile = new File(
                    getFilesDir(),
                    PROFILE_IMAGE_FILE
            );

            FileOutputStream outputStream =
                    new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

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
        }
    }

    private void loadSavedProfileImage() {

        File imageFile = new File(
                getFilesDir(),
                PROFILE_IMAGE_FILE
        );

        if (imageFile.exists()) {

            Bitmap bitmap = BitmapFactory.decodeFile(
                    imageFile.getAbsolutePath()
            );

            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            }
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

    private void goToLogin() {

        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
