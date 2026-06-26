package com.prashant.milkdairy.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prashant.milkdairy.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private EditText etMobileNumber, etWhatsappNumber, etDairyName, etDairyAddress;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etWhatsappNumber = findViewById(R.id.etWhatsappNumber);
        etDairyName = findViewById(R.id.etDairyName);
        etDairyAddress = findViewById(R.id.etDairyAddress);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String mobileNumber = etMobileNumber.getText().toString().trim();
        String whatsappNumber = etWhatsappNumber.getText().toString().trim();
        String dairyName = etDairyName.getText().toString().trim();
        String dairyAddress = etDairyAddress.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (TextUtils.isEmpty(mobileNumber)) {
            etMobileNumber.setError("Mobile number is required");
            return;
        }

        if (TextUtils.isEmpty(whatsappNumber)) {
            etWhatsappNumber.setError("WhatsApp number is required");
            return;
        }

        if (TextUtils.isEmpty(dairyName)) {
            etDairyName.setError("Dairy name is required");
            return;
        }

        if (TextUtils.isEmpty(dairyAddress)) {
            etDairyAddress.setError("Dairy address is required");
            return;
        }

        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", userId);
                    userMap.put("fullName", fullName);
                    userMap.put("email", email);
                    userMap.put("mobileNumber", mobileNumber);
                    userMap.put("whatsappNumber", whatsappNumber);
                    userMap.put("dairyName", dairyName);
                    userMap.put("dairyAddress", dairyAddress);
                    userMap.put("createdAt", FieldValue.serverTimestamp());
                    userMap.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
