package com.project.sportssync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private EditText etNewLoginCode, etNewQrCode;
    private Button btnUpdateCodes, btnLogout, btnNewRegisterRequests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);
        
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupListeners();
        
        return view;
    }

    private void initViews(View view) {
        etNewLoginCode = view.findViewById(R.id.etNewLoginCode);
        etNewQrCode = view.findViewById(R.id.etNewQrCode);
        btnUpdateCodes = view.findViewById(R.id.btnUpdateCodes);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnNewRegisterRequests = view.findViewById(R.id.btnNewRegisterRequests);
    }

    private void setupListeners() {
        btnUpdateCodes.setOnClickListener(v -> updateCodes());
        btnLogout.setOnClickListener(v -> logout());
        btnNewRegisterRequests.setOnClickListener(v -> openApprovalRequests());
    }

    private void updateCodes() {
        String newLogin = etNewLoginCode.getText().toString().trim();
        String newQr = etNewQrCode.getText().toString().trim();

        if (TextUtils.isEmpty(newLogin) && TextUtils.isEmpty(newQr)) {
            Toast.makeText(getContext(), "Enter code or QR to update", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (!TextUtils.isEmpty(newLogin)) updates.put("loginCode", newLogin);
        if (!TextUtils.isEmpty(newQr)) updates.put("qrCode", newQr);

        db.collection("settings").document("app")
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Updated successfully", Toast.LENGTH_SHORT).show();
                    etNewLoginCode.setText("");
                    etNewQrCode.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void logout() {
        // Clear shared preferences
        SharedPreferences prefs = getContext().getSharedPreferences("SportsSyncPrefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    private void openApprovalRequests() {
        Intent i = new Intent(getContext(), AdminApprovalRequestsActivity.class);
        startActivity(i);
    }
}
