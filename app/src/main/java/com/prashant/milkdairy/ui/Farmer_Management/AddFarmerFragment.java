package com.prashant.milkdairy.ui.Farmer_Management;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddFarmerFragment extends Fragment {

    private TextView btnBack;
    private EditText etFarmerName, etMobile, etAddress;
    private MaterialButton btnCow, btnBuffalo, btnMix, btnCancel, btnReset, btnSave;

    private String selectedMilkType = "Cow";

    public AddFarmerFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_farmer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        etFarmerName = view.findViewById(R.id.etFarmerName);
        etMobile = view.findViewById(R.id.etMobile);
        etAddress = view.findViewById(R.id.etAddress);

        btnCow = view.findViewById(R.id.btnCow);
        btnBuffalo = view.findViewById(R.id.btnBuffalo);
        btnMix = view.findViewById(R.id.btnMix);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnReset = view.findViewById(R.id.btnReset);
        btnSave = view.findViewById(R.id.btnSave);

        updateMilkButtons();

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        btnCow.setOnClickListener(v -> {
            selectedMilkType = "Cow";
            updateMilkButtons();
        });

        btnBuffalo.setOnClickListener(v -> {
            selectedMilkType = "Buffalo";
            updateMilkButtons();
        });

        btnMix.setOnClickListener(v -> {
            selectedMilkType = "Mix";
            updateMilkButtons();
        });


        btnReset.setOnClickListener(v -> resetForm());

        btnSave.setOnClickListener(v -> validateAndSaveFarmer());
    }

    private void validateAndSaveFarmer() {
        String name = etFarmerName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFarmerName.setError("Farmer name is required");
            return;
        }

        if (TextUtils.isEmpty(mobile)) {
            etMobile.setError("Mobile number is required");
            return;
        }

        if (mobile.length() != 10) {
            etMobile.setError("Enter 10 digit mobile number");
            return;
        }

        saveFarmer(name, mobile, address);
    }

    private void saveFarmer(String name, String mobile, String address) {
        btnSave.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String farmerId = FirebaseRefs.farmers().document().getId();

        db.runTransaction(transaction -> {
            Long currentCount = transaction.get(FirebaseRefs.currentUserDoc()).getLong("farmerCounter");

            long nextCount = currentCount == null ? 1 : currentCount + 1;
            String farmerCode = String.format(Locale.US, "F%04d", nextCount);

            Map<String, Object> farmerMap = new HashMap<>();
            farmerMap.put("id", farmerId);
            farmerMap.put("code", farmerCode);
            farmerMap.put("name", name);
            farmerMap.put("mobile", mobile);
            farmerMap.put("address", address);
            farmerMap.put("milkType", selectedMilkType);
            farmerMap.put("status", "Active");
            farmerMap.put("createdAt", FieldValue.serverTimestamp());
            farmerMap.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(FirebaseRefs.farmers().document(farmerId), farmerMap);
            transaction.update(FirebaseRefs.currentUserDoc(), "farmerCounter", nextCount);

            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(requireContext(), "Farmer added successfully", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }).addOnFailureListener(e -> {
            btnSave.setEnabled(true);
            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void resetForm() {
        etFarmerName.setText("");
        etMobile.setText("");
        etAddress.setText("");
        selectedMilkType = "Cow";
        updateMilkButtons();
    }

    private void updateMilkButtons() {
        int selectedColor = Color.parseColor("#2E8B7D");
        int unselectedColor = Color.parseColor("#F6F3EA");

        setMilkButtonStyle(btnCow, selectedMilkType.equals("Cow"), selectedColor, unselectedColor);
        setMilkButtonStyle(btnBuffalo, selectedMilkType.equals("Buffalo"), selectedColor, unselectedColor);
        setMilkButtonStyle(btnMix, selectedMilkType.equals("Mix"), selectedColor, unselectedColor);
    }

    private void setMilkButtonStyle(MaterialButton button, boolean selected,
                                    int selectedColor, int unselectedColor) {
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(unselectedColor));
            button.setTextColor(Color.BLACK);
        }
    }

}
