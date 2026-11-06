package com.example.safeher;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

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

        recyclerView = findViewById(
                R.id.emergencyContactsRecyclerView);

        emptyText = findViewById(R.id.emptyContactsText);

        Button addButton = findViewById(
                R.id.addEmergencyContactButton);

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
    private void showContactDialog(
            EmergencyContact existingContact) {

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

        layout.setPadding(
                padding, padding / 2, padding, 0);

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
                .setPositiveButton(
                        isEditing ? "Save" : "Add", null)
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

                            success =
                                    databaseHelper.updateEmergencyContact(
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

                            success = newContactId > 0;

                            if (success && newContactId <= Integer.MAX_VALUE) {

                                updatedContact = new EmergencyContact(
                                        (int) newContactId,
                                        currentUserId,
                                        name,
                                        phone
                                );

                            } else {
                                updatedContact = null;
                                success = false;
                            }
                        }

                        if (success) {

                            loadContacts();
                            dialog.dismiss();

                            syncContactToCloud(updatedContact);

                        } else {

                            Toast.makeText(this,
                                    "Could not save contact. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // Sync added or updated contact
    private void syncContactToCloud(
            EmergencyContact contact) {

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

        String normalized =
                phone.replaceAll("[\\s()-]", "");

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

                        Toast.makeText(this,
                                "Could not delete contact",
                                Toast.LENGTH_SHORT).show();
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
