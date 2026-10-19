package com.example.safeher;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "safeher.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "full_name";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_NAME + " TEXT NOT NULL, " +
                        COL_EMAIL + " TEXT NOT NULL UNIQUE, " +
                        COL_PASSWORD + " TEXT NOT NULL)";

        db.execSQL(createUsersTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        // Database schema upgrades will be handled here.
    }


    public boolean registerUser(String name,
                                String email,
                                String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_EMAIL, email);

            // Store a password hash instead of the plain password.
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

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }

            return hex.toString();

        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = null;

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

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = null;

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

    public User getAuthenticatedUser(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{
                            COL_ID,
                            COL_NAME,
                            COL_EMAIL,
                            COL_PASSWORD
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

                if (enteredHash != null && enteredHash.equals(storedHash)) {

                    int id = cursor.getInt(
                            cursor.getColumnIndexOrThrow(COL_ID)
                    );

                    String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_NAME)
                    );

                    String userEmail = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_EMAIL)
                    );

                    return new User(id, name, userEmail);
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

    public User getUserById(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COL_ID, COL_NAME, COL_EMAIL},
                    COL_ID + " = ?",
                    new String[]{String.valueOf(userId)},
                    null, null, null
            );

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COL_ID)
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_NAME)
                );

                String email = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_EMAIL)
                );

                return new User(id, name, email);
            }

            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

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
            // The email may already belong to another account.
            return false;

        } finally {
            db.close();
        }
    }


}
