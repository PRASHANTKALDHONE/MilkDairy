package com.prashant.milkdairy.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prashant.milkdairy.R;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private TextView tvFullName, tvRole, tvMobile, tvEmail, tvDairyName, tvAddress;
    private MaterialButton btnEditProfile, btnChangePassword, btnLogout;

    private ProgressBar progressProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private Uri selectedImageUri;
    private Dialog editDialog;
    private boolean saving = false;

    private String uid = "";
    private String fullName = "";
    private String mobile = "";
    private String email = "";
    private String dairyName = "";
    private String address = "";
    private String role = "";
    private String profileImage = "";

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                selectedImageUri = uri;
                imgProfile.setImageURI(uri);

                if (editDialog != null && editDialog.isShowing()) {
                    ShapeableImageView dialogImage = editDialog.findViewById(R.id.imgEditProfile);
                    if (dialogImage != null) {
                        dialogImage.setImageURI(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        uid = currentUser.getUid();
        userRef = db.collection("users").document(uid);

        initViews();

        btnLogout.setOnClickListener(v -> {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(" ⏻ Logout")
                    .setMessage("Are you sure you want to logout from your account?")

                    .setPositiveButton("Logout", (dialog, which) -> {
                        logoutUser();
                    })

                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })

                    .show();
        });
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> {

            new MaterialAlertDialogBuilder(this)
                    .setTitle("\uD83D\uDD11 Change Password")
                    .setMessage("After changing your password, you will be logged out for security reasons and need to login again using your new password.")

                    // OPEN CHANGE PASSWORD DIALOG
                    .setPositiveButton("Change", (dialogInterface, which) -> {
                        showChangePasswordDialog();
                    })

                    // CANCEL ACTION
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                    })

                    .show();
        });

        if (!isPasswordLoginUser()) {
            btnChangePassword.setVisibility(View.GONE);
        }

        imgProfile.setOnClickListener(v -> imagePicker.launch("image/*"));

        loadProfile();
    }

    private void initViews() {
        imgProfile = findViewById(R.id.imgProfile);
        tvFullName = findViewById(R.id.tvFullName);
        tvRole = findViewById(R.id.tvRole);
        tvMobile = findViewById(R.id.tvMobile);
        tvEmail = findViewById(R.id.tvEmail);
        tvDairyName = findViewById(R.id.tvDairyName);
        tvAddress = findViewById(R.id.tvAddress);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        progressProfile = findViewById(R.id.progressProfile);
    }

    private void loadProfile() {
        showLoading(true);

        userRef.get(Source.CACHE).addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                bindProfile(snapshot.getData());
            }
        });

        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    showLoading(false);

                    if (snapshot.exists()) {
                        bindProfile(snapshot.getData());
                    } else {
                        createDefaultProfile();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Unable to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createDefaultProfile() {
        email = currentUser.getEmail() == null ? "" : currentUser.getEmail();

        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("userId", uid);
        map.put("fullName", "");
        map.put("mobile", "");
        map.put("mobileNumber", "");
        map.put("whatsappNumber", "");
        map.put("email", email);
        map.put("dairyName", "");
        map.put("address", "");
        map.put("dairyAddress", "");
        map.put("role", "Milk Dairy Administrator");
        map.put("profileImage", "");
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("updatedAt", FieldValue.serverTimestamp());

        userRef.set(map)
                .addOnSuccessListener(unused -> bindProfile(map))
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void bindProfile(Map<String, Object> data) {
        if (data == null) return;

        fullName = firstValue(data, "fullName", "name");

        mobile = firstValue(data, "mobile", "mobileNumber", "whatsappNumber");

        email = firstValue(data, "email");
        if (email.isEmpty() && currentUser.getEmail() != null) {
            email = currentUser.getEmail();
        }

        dairyName = firstValue(data, "dairyName", "businessName");

        address = firstValue(data, "address", "dairyAddress");

        role = firstValue(data, "role");
        if (role.isEmpty()) {
            role = "Milk Dairy Administrator";
        }

        profileImage = firstValue(data, "profileImage", "profileImageUrl");

        tvFullName.setText(fullName.isEmpty() ? "Profile Name" : fullName);
        tvRole.setText(role);
        tvMobile.setText(mobile.isEmpty() ? "-" : "+91 " + mobile);
        tvEmail.setText(email.isEmpty() ? "-" : email);
        tvDairyName.setText(dairyName.isEmpty() ? "-" : dairyName);
        tvAddress.setText(address.isEmpty() ? "-" : address);

        if (!profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.applogo)
                    .error(R.drawable.applogo)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.applogo);
        }
    }

    private String firstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);

            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }

        return "";
    }

    private void showEditProfileDialog() {
        selectedImageUri = null;

        editDialog = new Dialog(this);
        editDialog.setContentView(R.layout.dialog_edit_profile);

        ShapeableImageView imgEditProfile = editDialog.findViewById(R.id.imgEditProfile);
        TextView tvEditEmail = editDialog.findViewById(R.id.tvEditEmail);

        EditText etName = editDialog.findViewById(R.id.etEditFullName);
        EditText etMobile = editDialog.findViewById(R.id.etEditMobile);
        EditText etDairy = editDialog.findViewById(R.id.etEditDairyName);
        EditText etAddress = editDialog.findViewById(R.id.etEditAddress);

        MaterialButton btnCancel = editDialog.findViewById(R.id.btnCancelProfileEdit);
        MaterialButton btnSave = editDialog.findViewById(R.id.btnSaveProfileEdit);

        View btnClose = editDialog.findViewById(R.id.btnCloseProfileDialog);

        tvEditEmail.setText("Email: " + (email.isEmpty() ? "-" : email));

        etName.setText(fullName);
        etMobile.setText(mobile);
        etDairy.setText(dairyName);
        etAddress.setText(address);

        imgEditProfile.setImageResource(R.drawable.applogo);

        if (!profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.applogo)
                    .error(R.drawable.applogo)
                    .into(imgEditProfile);
        }

        imgEditProfile.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnClose.setOnClickListener(v -> editDialog.dismiss());
        btnCancel.setOnClickListener(v -> editDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newMobile = etMobile.getText().toString().trim();
            String newDairy = etDairy.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();

            if (!validateProfile(newName, newMobile, newDairy, newAddress,
                    etName, etMobile, etDairy, etAddress)) {
                return;
            }

            saveProfile(newName, newMobile, newDairy, newAddress, btnSave);
        });

        editDialog.show();

        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private boolean validateProfile(String name,
                                    String mobileNo,
                                    String dairy,
                                    String addr,
                                    EditText etName,
                                    EditText etMobile,
                                    EditText etDairy,
                                    EditText etAddress) {
        if (name.isEmpty()) {
            etName.setError("Full name required");
            return false;
        }

        if (mobileNo.isEmpty()) {
            etMobile.setError("Mobile required");
            return false;
        }

        if (!mobileNo.matches("\\d{10}")) {
            etMobile.setError("Mobile must be 10 digits");
            return false;
        }

        if (dairy.isEmpty()) {
            etDairy.setError("Dairy name required");
            return false;
        }

        if (addr.isEmpty()) {
            etAddress.setError("Address required");
            return false;
        }

        return true;
    }

    private void saveProfile(String newName,
                             String newMobile,
                             String newDairy,
                             String newAddress,
                             MaterialButton btnSave) {
        if (saving) return;

        saving = true;
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (selectedImageUri != null) {
            uploadProfileImage(newName, newMobile, newDairy, newAddress, btnSave);
        } else {
            updateProfileFirestore(newName, newMobile, newDairy, newAddress, profileImage, btnSave);
        }
    }
    private boolean isPasswordLoginUser() {
        if (currentUser == null) return false;

        for (UserInfo info : currentUser.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    private void showChangePasswordDialog() {
        if (currentUser == null || currentUser.getEmail() == null || currentUser.getEmail().isEmpty()) {
            Toast.makeText(this, "Password change is available only for email login", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_password);

        EditText etCurrent = dialog.findViewById(R.id.etCurrentPassword);
        EditText etNew = dialog.findViewById(R.id.etNewPassword);
        EditText etConfirm = dialog.findViewById(R.id.etConfirmPassword);

        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelPassword);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSavePassword);
        View btnClose = dialog.findViewById(R.id.btnClosePasswordDialog);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPassword = etCurrent.getText().toString().trim();
            String newPassword = etNew.getText().toString().trim();
            String confirmPassword = etConfirm.getText().toString().trim();

            if (!validatePasswordChange(currentPassword, newPassword, confirmPassword,
                    etCurrent, etNew, etConfirm)) {
                return;
            }

            updatePassword(currentPassword, newPassword, btnSave, dialog);
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private boolean validatePasswordChange(String currentPassword,
                                           String newPassword,
                                           String confirmPassword,
                                           EditText etCurrent,
                                           EditText etNew,
                                           EditText etConfirm) {
        if (currentPassword.isEmpty()) {
            etCurrent.setError("Current password required");
            return false;
        }

        if (newPassword.isEmpty()) {
            etNew.setError("New password required");
            return false;
        }

        if (newPassword.length() < 6) {
            etNew.setError("Password must be at least 6 characters");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            etConfirm.setError("Confirm password required");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirm.setError("Passwords do not match");
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            etNew.setError("New password cannot be same as current password");
            return false;
        }

        return true;
    }

    private void updatePassword(String currentPassword,
                                String newPassword,
                                MaterialButton btnSave,
                                Dialog dialog) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Updating...");

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(),
                currentPassword
        );

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> currentUser.updatePassword(newPassword)
                        .addOnSuccessListener(done -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Update");

                            dialog.dismiss();

                            Toast.makeText(this, "Password updated successfully. Please login again.", Toast.LENGTH_LONG).show();

                            resetDrawerPreviewForCurrentUser();
                            auth.signOut();
                            goToLogin();
                        })
                        .addOnFailureListener(e -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Update");
                            Toast.makeText(this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Update");
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                });
    }


    private void uploadProfileImage(String newName,
                                    String newMobile,
                                    String newDairy,
                                    String newAddress,
                                    MaterialButton btnSave) {
        try {
            byte[] imageBytes = compressImage(selectedImageUri);

            StorageReference ref = storage.getReference()
                    .child("profile_images")
                    .child(uid + ".jpg");

            ref.putBytes(imageBytes)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }

                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri ->
                            updateProfileFirestore(newName, newMobile, newDairy, newAddress, uri.toString(), btnSave))
                    .addOnFailureListener(e -> {
                        saving = false;
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            saving = false;
            btnSave.setEnabled(true);
            btnSave.setText("Save");
            Toast.makeText(this, "Image error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] compressImage(Uri imageUri) throws Exception {
        Bitmap bitmap;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
            bitmap = ImageDecoder.decodeBitmap(source);
        } else {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        }

        int maxSize = 800;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxSize || height > maxSize) {
            float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);

        return outputStream.toByteArray();
    }

    private void updateProfileFirestore(String newName,
                                        String newMobile,
                                        String newDairy,
                                        String newAddress,
                                        String imageUrl,
                                        MaterialButton btnSave) {
        Map<String, Object> map = new HashMap<>();

        map.put("uid", uid);
        map.put("userId", uid);

        map.put("fullName", newName);

        map.put("mobile", newMobile);
        map.put("mobileNumber", newMobile);
        map.put("whatsappNumber", newMobile);

        map.put("email", email);

        map.put("dairyName", newDairy);

        map.put("address", newAddress);
        map.put("dairyAddress", newAddress);

        map.put("role", role.isEmpty() ? "Milk Dairy Administrator" : role);
        map.put("profileImage", imageUrl == null ? "" : imageUrl);
        map.put("updatedAt", FieldValue.serverTimestamp());

        userRef.update(map)
                .addOnSuccessListener(unused -> {
                    saving = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");

                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    if (editDialog != null) {
                        editDialog.dismiss();
                    }

                    loadProfile();
                })
                .addOnFailureListener(e -> {
                    saving = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private int resolveColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void showLoading(boolean show) {
        if (progressProfile != null) {
            progressProfile.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (btnEditProfile != null) {
            btnEditProfile.setEnabled(!show);
        }
    }
    private void resetDrawerPreviewForCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        getSharedPreferences("MilkDairyDrawerPreview", MODE_PRIVATE)
                .edit()
                .remove("drawer_preview_seen_" + user.getUid())
                .apply();
    }

    private void logoutUser() {
        resetDrawerPreviewForCurrentUser();

        auth.signOut();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
