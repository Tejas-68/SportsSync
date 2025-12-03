package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etUucms, etResetCode, etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etUucms = findViewById(R.id.etUucms);
        etResetCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> attemptReset());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptReset() {
        String uucms = etUucms.getText().toString().trim().toUpperCase();
        String code = etResetCode.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(uucms)) {
            etUucms.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(code)) {
            etResetCode.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etNewPassword.setError("Min 6 chars");
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords mismatch");
            return;
        }

        // Disable button
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Verifying...");

        // 1. Find user by UUCMS
        db.collection("users")
                .whereEqualTo("uucms", uucms)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showError("User not found");
                        return;
                    }

                    DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                    String storedCode = userDoc.getString("resetCode");

                    // 2. Verify code
                    if (storedCode != null && storedCode.equals(code)) {
                        // Code matches!
                        String email = userDoc.getString("email");
                        if (email == null) email = uucms.toLowerCase() + "@sportssync.app";
                        
                        // 3. Reset password
                        // Since we can't easily update another user's password without their old one or being logged in,
                        // we'll use a workaround:
                        // The "resetCode" pattern implies the Admin GAVE this code.
                        // Ideally, we would use Firebase Admin SDK, but that's server-side.
                        // Client-side workaround:
                        // We can't use updatePassword() because we aren't logged in as that user.
                        // We can't use sendPasswordResetEmail() because these are fake emails.
                        
                        // ALTERNATIVE:
                        // Since we verified the code (which only Admin could set), we can treat this as "Authorized".
                        // BUT Firebase Client SDK doesn't allow setting password for another user.
                        //
                        // SOLUTION:
                        // The only way to reset a password for a "fake email" user without the old password
                        // is to delete and recreate the user (which loses UID but keeps data if we map it back),
                        // OR if the user is currently logged in (which they aren't).
                        //
                        // Wait, if the user forgot their password, they can't login.
                        // If we use "fake emails", we are stuck unless we have a backend.
                        //
                        // HOWEVER, for this project context:
                        // Maybe the "Admin Code" IS the temporary password?
                        // "admin can set the rest password code"
                        //
                        // If the admin sets a temporary password in Auth, that works.
                        // But Admin Client SDK can't set passwords for others.
                        //
                        // Let's re-read: "forgot password when students forgot the password there wll be a code generated in the admin side if the enter that they can reset the password"
                        //
                        // If we can't change the Auth password, we might have to re-register them?
                        // Or maybe we just update the "password" field in Firestore (if we were storing it there, but we shouldn't).
                        //
                        // WAIT! In LoginActivity, I saw:
                        // "registerNewStudent(email, password, uucms, name)"
                        // And "deleteAndRecreateAccount" was used before.
                        //
                        // If we can't use Admin SDK, the only way to "Reset" is to DELETE the Auth user and RE-CREATE it with the new password.
                        // But we must preserve the Firestore data (User ID).
                        //
                        // Problem: Re-creating Auth user generates a NEW UID.
                        // Our Firestore data is linked by Document ID (which is usually random or UID).
                        // In LoginActivity: `db.collection("users").add(user)` -> Random Document ID.
                        // The `firebaseUid` field is stored in the document.
                        //
                        // So if we delete the Auth user and create a new one, we get a new UID.
                        // We just need to update the `firebaseUid` field in the existing Firestore document!
                        //
                        // YES! That's the solution.
                        // 1. Delete old Auth user? We can't delete another user from client SDK.
                        // 2. We can't delete the old Auth user without their credentials.
                        //
                        // This is a tricky limitation of Firebase Client SDK.
                        //
                        // Workaround for this specific project (assuming no backend):
                        // The "Reset Code" could be used as a "One Time Password" to LOGIN?
                        // If we store the reset code in Firestore, we can't use it to authenticate with Firebase Auth.
                        //
                        // Maybe the "Admin" actually performs the reset?
                        // "in the admin side admin can set the rest password code"
                        //
                        // If the Admin sets the password directly?
                        // Admin enters "New Password" for student.
                        // But Admin can't change another user's password without their credentials either.
                        //
                        // OK, let's look at `deleteAndRecreateAccount` in `LoginActivity`.
                        // It deletes the Firestore data and re-registers. That loses data.
                        //
                        // What if we use `signInWithEmailAndPassword` with the OLD password? We don't have it.
                        //
                        // Is there ANY way?
                        // 1. Email link (we use fake emails).
                        // 2. Admin SDK (we don't have a server).
                        //
                        // HYBRID APPROACH:
                        // The "Reset Code" allows the user to "Claim" the account.
                        // Since we can't recover the Auth account, we MUST create a new one.
                        // BUT we can link it to the OLD Firestore data.
                        //
                        // Flow:
                        // 1. Verify Code.
                        // 2. Create NEW Firebase Auth User (with same email? No, email must be unique. Old user still exists).
                        //    - We can't delete the old user without credentials.
                        //    - We can't create a new user with the same email.
                        //
                        // This implies we need a way to delete the old user.
                        //
                        // Maybe the "Reset Code" IS the password?
                        // If the Admin changes the password to "123456" manually?
                        // Admin can't do that.
                        //
                        // Let's assume the "Reset Code" is used to verify identity, and then...
                        // We might have to use a Cloud Function?
                        // The prompt says "code generated in the admin side".
                        //
                        // Let's assume there is a Cloud Function or we use a "Shadow Account" system?
                        // No, that's too complex.
                        //
                        // What if we just update the Firestore "password" field?
                        // `LoginActivity` checks:
                        // `FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)`
                        //
                        // If that fails, it checks Firestore?
                        // No, it relies on Auth.
                        //
                        // Wait, `LoginActivity` had:
                        // `if (userNotFound) { registerNewStudent... }`
                        // `else if (wrongPassword) { ... }`
                        //
                        // If we can't delete the old user, we are stuck.
                        //
                        // UNLESS: We use a different email for the new account?
                        // e.g. `uucms + timestamp @sportssync.app`?
                        // And update the Firestore document to point to this new UID and Email.
                        //
                        // YES! This works.
                        // 1. Verify Code.
                        // 2. Create NEW Auth User with `uucms + "_" + timestamp + "@sportssync.app"`.
                        // 3. Update the EXISTING Firestore document:
                        //    - `firebaseUid` = new UID
                        //    - `email` = new email
                        //    - `resetCode` = null (clear it)
                        // 4. Login the user.
                        //
                        // This effectively "Resets" the account access while preserving data.
                        // The old Auth user becomes an orphan (garbage), which is acceptable for a prototype/MVP without backend.
                        
                        performSafeReset(userDoc.getId(), uucms, password);
                        
                    } else {
                        showError("Invalid or expired reset code");
                    }
                })
                .addOnFailureListener(e -> showError(ErrorMessageHelper.getUserFriendlyMessage(e)));
    }

    private void performSafeReset(String firestoreDocId, String uucms, String newPassword) {
        // Create a unique email alias to avoid conflict with the old orphaned auth user
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newEmail = uucms.toLowerCase() + "." + timestamp + "@sportssync.app";

        auth.createUserWithEmailAndPassword(newEmail, newPassword)
                .addOnSuccessListener(authResult -> {
                    String newUid = authResult.getUser().getUid();

                    // Update existing Firestore document to point to new Auth User
                    db.collection("users").document(firestoreDocId)
                            .update(
                                    "firebaseUid", newUid,
                                    "email", newEmail,
                                    "resetCode", null // Clear code
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Password reset successful!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // If update fails, we have a dangling auth user, but that's okay
                                showError("Failed to update account link: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> showError(ErrorMessageHelper.getUserFriendlyMessage(e)));
    }

    private void showError(String message) {
        btnResetPassword.setEnabled(true);
        btnResetPassword.setText("Reset Password");
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
