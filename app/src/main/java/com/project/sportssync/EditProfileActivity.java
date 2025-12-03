package com.project.sportssync;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUucms, etName, etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnSave, btnCancel, btnUploadPhoto;
    private ImageView ivProfilePicture;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String userId, uucms, currentName, profilePictureUrl;
    private Uri selectedImageUri;
    
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        // Display selected image
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(ivProfilePicture);
                    }
                });

        // Initialize views
        etUucms = findViewById(R.id.etUucms);
        etName = findViewById(R.id.etName);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        // Get user info
        userId = sessionManager.getUserId();
        uucms = sessionManager.getUucms();
        currentName = sessionManager.getName();

        // Critical: Check if session is valid
        if (userId == null || uucms == null) {
            // Session corrupted or cleared - redirect to login
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Session Expired")
                    .setMessage("Your session has expired or is invalid. Please login again.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {
                        sessionManager.logout();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .show();
            return;
        }

        // Pre-fill fields
        etUucms.setText(uucms);
        etName.setText(currentName);

        // Load user data including profile picture
        loadUserData();

        btnUploadPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        profilePictureUrl = doc.getString("profilePictureUrl");
                        
                        // Load profile picture if exists
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_profile)
                                    .into(ivProfilePicture);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, ErrorMessageHelper.getUserFriendlyMessage(e), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveChanges() {
        String newName = etName.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password change if provided
        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Upload profile picture if selected
        if (selectedImageUri != null) {
            uploadProfilePicture(newName, newPassword);
        } else {
            // No new image, update profile with existing URL (or empty string if null)
            updateProfile(newName, newPassword, profilePictureUrl != null ? profilePictureUrl : "");
        }
    }

    private void uploadProfilePicture(String newName, String newPassword) {
        // Show progress
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        // Compress image
        byte[] imageData = ImageUtils.compressImage(this, selectedImageUri);
        if (imageData == null) {
            // Image compression failed - give user option to continue without image
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Image Processing Failed")
                    .setMessage("Failed to process the selected image. Would you like to save your profile changes without updating the photo?")
                    .setPositiveButton("Save Without Photo", (dialog, which) -> {
                        // Update profile without changing photo
                        updateProfile(newName, newPassword, profilePictureUrl != null ? profilePictureUrl : "");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Upload to Firebase Storage
        StorageReference profileRef = storage.getReference()
                .child("profile_pictures")
                .child(userId)
                .child("profile.jpg");

        profileRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot ->
                        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            updateProfile(newName, newPassword, downloadUrl);
                        })
                )
                .addOnFailureListener(e -> {
                        // Upload failed - give user option to continue without image
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Upload Failed")
                                .setMessage("Failed to upload photo: " + e.getMessage() + "\n\nWould you like to save your other changes without updating the photo?")
                                .setPositiveButton("Save Without Photo", (dialog, which) -> {
                                    updateProfile(newName, newPassword, profilePictureUrl != null ? profilePictureUrl : "");
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                });
    }

    private void updateProfile(String newName, String newPassword, String newProfilePictureUrl) {
        // Ensure we never set null - use empty string as fallback
        String safeProfilePictureUrl = newProfilePictureUrl != null ? newProfilePictureUrl : "";
        
        // Update Firestore
        db.collection("users").document(userId)
                .update(
                        "name", newName,
                        "profilePictureUrl", safeProfilePictureUrl
                )
                .addOnSuccessListener(aVoid -> {
                    // Update Firebase Auth password if changed
                    if (!TextUtils.isEmpty(newPassword) && auth.getCurrentUser() != null) {
                        auth.getCurrentUser().updatePassword(newPassword)
                                .addOnSuccessListener(v -> {
                                    sessionManager.createLoginSession(userId, uucms, "student", newName);
                                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to update password: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()
                                );
                    } else {
                        sessionManager.createLoginSession(userId, uucms, "student", newName);
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
