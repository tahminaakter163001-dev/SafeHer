
package com.example.safeher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "safeher.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_USERS = "users";

    public static final String COL_ID = "id";
    public static final String COL_NAME = "full_name";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    public static final String COL_PHONE = "phone_number";
    public static final String COL_ADDRESS = "address";
    public static final String COL_DOB = "date_of_birth";
    public static final String COL_BLOOD_GROUP = "blood_group";

    private static final String TABLE_CONTACTS = "emergency_contacts";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create database tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_NAME + " TEXT NOT NULL, " +
                        COL_EMAIL + " TEXT NOT NULL UNIQUE, " +
                        COL_PASSWORD + " TEXT NOT NULL, " +
                        COL_PHONE + " TEXT, " +
                        COL_ADDRESS + " TEXT, " +
                        COL_DOB + " TEXT, " +
                        COL_BLOOD_GROUP + " TEXT)";

        db.execSQL(createUsersTable);

        createEmergencyContactsTable(db);
    }

    // Create emergency contacts table
    private void createEmergencyContactsTable(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "phone_number TEXT NOT NULL, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")"
        );
    }

    // Upgrade database without deleting existing user data
    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {

        if (oldVersion < 2) {
            createEmergencyContactsTable(db);
        }

        if (oldVersion < 3) {

            db.execSQL(
                    "ALTER TABLE users ADD COLUMN " +
                            COL_PHONE + " TEXT"
            );

            db.execSQL(
                    "ALTER TABLE users ADD COLUMN " +
                            COL_ADDRESS + " TEXT"
            );

            db.execSQL(
                    "ALTER TABLE users ADD COLUMN " +
                            COL_DOB + " TEXT"
            );

            db.execSQL(
                    "ALTER TABLE users ADD COLUMN " +
                            COL_BLOOD_GROUP + " TEXT"
            );
        }
    }

    // Register a normal user
    public boolean registerUser(String name,
                                String email,
                                String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COL_NAME, name);
            values.put(COL_EMAIL, email);

            String hashedPassword = hashPassword(password);

            if (hashedPassword == null) {
                return false;
            }

            values.put(COL_PASSWORD, hashedPassword);

            long result = db.insert(TABLE_USERS, null, values);

            return result != -1;

        } finally {
            db.close();
        }
    }

    // Register a Firebase user locally
    public boolean registerFirebaseUser(String name, String email) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COL_NAME, name);
            values.put(COL_EMAIL, email);
            values.put(COL_PASSWORD, "FIREBASE_AUTH");

            long result = db.insert(TABLE_USERS, null, values);

            return result != -1;

        } finally {
            db.close();
        }
    }

    // Delete local account by email
    public boolean deleteLocalAccountByEmail(String email) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();

        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COL_ID},
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (!cursor.moveToFirst()) {
                return false;
            }

            int userId = cursor.getInt(0);

            cursor.close();
            cursor = null;

            db.delete(
                    TABLE_CONTACTS,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)}
            );

            int deletedRows = db.delete(
                    TABLE_USERS,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(userId)}
            );

            if (deletedRows > 0) {
                db.setTransactionSuccessful();
                return true;
            }

            return false;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.endTransaction();
            db.close();
        }
    }

    // Check whether account uses Firebase
    public boolean isFirebaseAccount(String email) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COL_PASSWORD},
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (cursor.moveToFirst()) {

                String storedPassword = cursor.getString(0);

                return "FIREBASE_AUTH".equals(storedPassword);
            }

            return false;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Read user by email
    public User getUserByEmail(String email) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    getProfileColumns(),
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (cursor.moveToFirst()) {
                return readUserFromCursor(cursor);
            }

            return null;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    // Check login credentials
    public boolean checkUser(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COL_PASSWORD},
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (cursor.moveToFirst()) {

                String storedHash = cursor.getString(0);
                String enteredHash = hashPassword(password);

                return enteredHash != null
                        && enteredHash.equals(storedHash);
            }

            return false;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Check whether email already exists
    public boolean emailExists(String email) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COL_ID},
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            return cursor.moveToFirst();

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Authenticate local user
    public User getAuthenticatedUser(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{
                            COL_ID,
                            COL_NAME,
                            COL_EMAIL,
                            COL_PASSWORD,
                            COL_PHONE,
                            COL_ADDRESS,
                            COL_DOB,
                            COL_BLOOD_GROUP
                    },
                    COL_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (cursor.moveToFirst()) {

                String storedHash = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_PASSWORD)
                );

                String enteredHash = hashPassword(password);

                if (enteredHash != null &&
                        enteredHash.equals(storedHash)) {

                    return readUserFromCursor(cursor);
                }
            }

            return null;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Read user by ID
    public User getUserById(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    getProfileColumns(),
                    COL_ID + " = ?",
                    new String[]{String.valueOf(userId)},
                    null, null, null
            );

            if (cursor.moveToFirst()) {
                return readUserFromCursor(cursor);
            }

            return null;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }
    }

    // Common profile columns
    private String[] getProfileColumns() {

        return new String[]{
                COL_ID,
                COL_NAME,
                COL_EMAIL,
                COL_PHONE,
                COL_ADDRESS,
                COL_DOB,
                COL_BLOOD_GROUP
        };
    }

    // Convert database cursor into User object
    private User readUserFromCursor(Cursor cursor) {

        int id = cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_ID)
        );

        String name = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_NAME)
        );

        String email = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_EMAIL)
        );

        String phone = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_PHONE)
        );

        String address = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ADDRESS)
        );

        String dob = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_DOB)
        );

        String bloodGroup = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_BLOOD_GROUP)
        );

        return new User(
                id, name, email,
                phone, address, dob, bloodGroup
        );
    }

    // Update complete user profile
    public boolean updateUserProfile(
            int userId,
            String name,
            String email,
            String phone,
            String address,
            String dob,
            String bloodGroup) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COL_NAME, name);
            values.put(COL_EMAIL, email);
            values.put(COL_PHONE, phone);
            values.put(COL_ADDRESS, address);
            values.put(COL_DOB, dob);
            values.put(COL_BLOOD_GROUP, bloodGroup);

            int rowsUpdated = db.update(
                    TABLE_USERS,
                    values,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(userId)}
            );

            return rowsUpdated > 0;

        } catch (android.database.sqlite.SQLiteConstraintException e) {

            return false;

        } finally {
            db.close();
        }
    }

    // Backward-compatible profile update method
    public boolean updateUserProfile(
            int userId, String name, String email) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COL_NAME, name);
            values.put(COL_EMAIL, email);

            int rowsUpdated = db.update(
                    TABLE_USERS,
                    values,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(userId)}
            );

            return rowsUpdated > 0;

        } catch (android.database.sqlite.SQLiteConstraintException e) {

            return false;

        } finally {
            db.close();
        }
    }

    // Add emergency contact
    public long addEmergencyContact(int userId,
                                    String name,
                                    String phoneNumber) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put("user_id", userId);
            values.put("name", name);
            values.put("phone_number", phoneNumber);

            return db.insert(TABLE_CONTACTS, null, values);

        } finally {
            db.close();
        }
    }

    // Get emergency contacts for a specific user
    public ArrayList<EmergencyContact> getEmergencyContacts(int userId) {

        ArrayList<EmergencyContact> contacts = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_CONTACTS,
                    new String[]{
                            "id",
                            "user_id",
                            "name",
                            "phone_number"
                    },
                    "user_id = ?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    "name ASC"
            );

            while (cursor.moveToNext()) {

                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                int ownerId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("user_id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                String phone = cursor.getString(
                        cursor.getColumnIndexOrThrow("phone_number")
                );

                contacts.add(
                        new EmergencyContact(id, ownerId, name, phone)
                );
            }

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }

        return contacts;
    }

    // Update emergency contact
    public boolean updateEmergencyContact(
            int contactId,
            int userId,
            String name,
            String phoneNumber) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put("name", name);
            values.put("phone_number", phoneNumber);

            int rowsUpdated = db.update(
                    TABLE_CONTACTS,
                    values,
                    "id = ? AND user_id = ?",
                    new String[]{
                            String.valueOf(contactId),
                            String.valueOf(userId)
                    }
            );

            return rowsUpdated > 0;

        } finally {
            db.close();
        }
    }

    // Delete emergency contact
    public boolean deleteEmergencyContact(int contactId, int userId) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            int rowsDeleted = db.delete(
                    TABLE_CONTACTS,
                    "id = ? AND user_id = ?",
                    new String[]{
                            String.valueOf(contactId),
                            String.valueOf(userId)
                    }
            );

            return rowsDeleted > 0;

        } finally {
            db.close();
        }
    }
}
