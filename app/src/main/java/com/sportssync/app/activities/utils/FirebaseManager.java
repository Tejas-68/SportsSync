package com.sportssync.app.activities.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }
}