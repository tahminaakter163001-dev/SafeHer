
package com.example.safeher;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EmergencyContactsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private FirestoreHelper firestoreHelper;

    private EmergencyContactAdapter adapter;
    private ArrayList<EmergencyContact> contacts;

    private RecyclerView recyclerView;
    private TextView emptyText;
    private int currentUserId;

    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private static class PhoneContact {
        String name;
        String phone;

        PhoneContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        firestoreHelper = new FirestoreHelper();

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        currentUserId = sessionManager.getUserId();

        // Initialize Android Contact Picker
        contactPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() == RESULT_OK
                                    && result.getData() != null) {

                                Uri contactUri =
                                        result.getData().getData();

                                if (contactUri != null) {
                                    readSelectedContact(contactUri);
                                }
                            }
                        }
                );

        recyclerView = findViewById(
                R.id.emergencyContactsRecyclerView);

        emptyText = findViewById(R.id.emptyContactsText);

        Button addButton = findViewById(
                R.id.addEmergencyContactButton);

        Button importButton = findViewById(
                R.id.importPhoneContactsButton);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        contacts = new ArrayList<>();

        adapter = new EmergencyContactAdapter(
                contacts,
                new EmergencyContactAdapter.ContactActionListener() {

                    @Override
                    public void onEdit(EmergencyContact contact) {
                        showContactDialog(contact);
                    }

                    @Override
                    public void onDelete(EmergencyContact contact) {
                        confirmDelete(contact);
                    }
                }
        );

        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(
                v -> showContactDialog(null));

        importButton.setOnClickListener(
                v -> openPhoneContactPicker());

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null
                && sessionManager.isLoggedIn()
                && adapter != null) {
            loadContacts();
        }
    }

    // Open the phone's Contacts picker
    private void openPhoneContactPicker() {

        Intent intent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        );

        contactPickerLauncher.launch(intent);
    }

    // Read only the selected contact
    private void readSelectedContact(Uri contactUri) {

        String name = null;
        String phone = null;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = getContentResolver().query(
                contactUri,
                projection,
                null,
                null,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {

                name = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                );

                phone = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not read selected contact.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (name == null || phone == null
                || name.trim().isEmpty()
                || phone.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Contact information unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PhoneContact selected = new PhoneContact(
                name.trim(),
                phone.trim()
        );

        new AlertDialog.Builder(this)
                .setTitle("Add Emergency Contact?")
                .setMessage(
                        "Name: " + selected.name
                                + "\nPhone: " + selected.phone
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dialog, which) -> {

                    if (!isValidPhone(selected.phone)) {

                        Toast.makeText(
                                this,
                                "Invalid phone number. Please add manually.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    saveImportedContact(selected);
                })
                .show();
    }

    // Save imported contact to SQLite
    private void saveImportedContact(PhoneContact contact) {

        long newId = databaseHelper.addEmergencyContact(
                currentUserId,
                contact.name,
                contact.phone
        );

        if (newId > 0 && newId <= Integer.MAX_VALUE) {

            EmergencyContact saved = new EmergencyContact(
                    (int) newId,
                    currentUserId,
                    contact.name,
                    contact.phone
            );

            loadContacts();
            syncContactToCloud(saved);

            Toast.makeText(
                    this,
                    "Contact imported successfully!",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Could not import contact. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Load contacts from SQLite
    private void loadContacts() {

        contacts.clear();

        contacts.addAll(
                databaseHelper.getEmergencyContacts(currentUserId)
        );

        adapter.notifyDataSetChanged();

        boolean isEmpty = contacts.isEmpty();

        emptyText.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE);

        recyclerView.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE);
    }

    // Add or edit emergency contact
    private void showContactDialog(EmergencyContact existingContact) {

        boolean isEditing = existingContact != null;

        EditText nameInput = new EditText(this);
        nameInput.setHint("Contact person's name");
        nameInput.setSingleLine(true);

        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone number");
        phoneInput.setSingleLine(true);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        android.widget.LinearLayout layout =
                new android.widget.LinearLayout(this);

        layout.setOrientation(
                android.widget.LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        layout.setPadding(padding, padding / 2, padding, 0);

        layout.addView(nameInput);
        layout.addView(phoneInput);

        if (isEditing) {
            nameInput.setText(existingContact.getName());
            phoneInput.setText(existingContact.getPhoneNumber());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing
                        ? "Edit Emergency Contact"
                        : "Add Emergency Contact")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(isEditing ? "Save" : "Add", null)
                .create();

        dialog.setOnShowListener(ignored -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String name = nameInput.getText()
                                .toString().trim();

                        String phone = phoneInput.getText()
                                .toString().trim();

                        if (name.length() < 2) {
                            nameInput.setError(
                                    "Enter at least 2 characters");
                            nameInput.requestFocus();
                            return;
                        }

                        if (!isValidPhone(phone)) {
                            phoneInput.setError(
                                    "Enter a valid phone number");
                            phoneInput.requestFocus();
                            return;
                        }

                        EmergencyContact updatedContact;
                        boolean success;

                        if (isEditing) {

                            success = databaseHelper.updateEmergencyContact(
                                    existingContact.getId(),
                                    currentUserId,
                                    name,
                                    phone
                            );

                            updatedContact = new EmergencyContact(
                                    existingContact.getId(),
                                    currentUserId,
                                    name,
                                    phone
                            );

                        } else {

                            long newContactId =
                                    databaseHelper.addEmergencyContact(
                                            currentUserId,
                                            name,
                                            phone
                                    );

                            success = newContactId > 0
                                    && newContactId <= Integer.MAX_VALUE;

                            updatedContact = success
                                    ? new EmergencyContact(
                                    (int) newContactId,
                                    currentUserId,
                                    name,
                                    phone)
                                    : null;
                        }

                        if (success) {

                            loadContacts();
                            dialog.dismiss();
                            syncContactToCloud(updatedContact);

                        } else {

                            Toast.makeText(
                                    this,
                                    "Could not save contact. Please try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        dialog.show();
    }

    // Sync contact to Firestore
    private void syncContactToCloud(EmergencyContact contact) {

        String email = sessionManager.getUserEmail();

        firestoreHelper.saveEmergencyContact(
                email,
                contact,
                new FirestoreHelper.OnSyncListener() {

                    @Override
                    public void onSuccess() {

                        if (!isFinishing()) {
                            Toast.makeText(
                                    EmergencyContactsActivity.this,
                                    "Contact synced to cloud!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {

                        if (!isFinishing()) {

                            if (error != null && error.contains(
                                    "No matching authenticated Firebase account")) {

                                Toast.makeText(
                                        EmergencyContactsActivity.this,
                                        "Contact saved locally.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        EmergencyContactsActivity.this,
                                        "Saved locally, but cloud sync failed.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    }
                }
        );
    }

    // Phone validation
    private boolean isValidPhone(String phone) {

        String normalized = phone.replaceAll("[\\s()-]", "");

        return normalized.matches("\\+?[0-9]{7,15}");
    }

    // Delete confirmation
    private void confirmDelete(EmergencyContact contact) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Contact?")
                .setMessage("Remove " + contact.getName()
                        + " from your emergency contacts?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {

                    boolean deleted =
                            databaseHelper.deleteEmergencyContact(
                                    contact.getId(),
                                    currentUserId
                            );

                    if (deleted) {

                        loadContacts();

                        String email = sessionManager.getUserEmail();

                        firestoreHelper.deleteEmergencyContact(
                                email,
                                contact.getId(),
                                new FirestoreHelper.OnSyncListener() {

                                    @Override
                                    public void onSuccess() {

                                        if (!isFinishing()) {
                                            Toast.makeText(
                                                    EmergencyContactsActivity.this,
                                                    "Contact deleted and cloud synced!",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {

                                        if (!isFinishing()) {

                                            if (error != null && error.contains(
                                                    "No matching authenticated Firebase account")) {

                                                Toast.makeText(
                                                        EmergencyContactsActivity.this,
                                                        "Contact deleted locally.",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                            } else {

                                                Toast.makeText(
                                                        EmergencyContactsActivity.this,
                                                        "Deleted locally, but cloud delete failed.",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                        }
                                    }
                                }
                        );

                    } else {

                        Toast.makeText(
                                this,
                                "Could not delete contact",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .show();
    }

    // Navigate to login
    private void goToLogin() {

        Intent intent = new Intent(
                EmergencyContactsActivity.this,
                LoginActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}
