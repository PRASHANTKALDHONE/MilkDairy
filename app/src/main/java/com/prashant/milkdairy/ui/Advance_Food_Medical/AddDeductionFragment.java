package com.prashant.milkdairy.ui.Advance_Food_Medical;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddDeductionFragment extends Fragment {

    private EditText etDate, etAmount, etRemaining, etDescription;
    private AutoCompleteTextView etFarmer;
    private Spinner spCategory;
    private MaterialCardView btnBack;
    private Button btnCancel, btnSave;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    private final List<Farmer> farmers = new ArrayList<>();
    private Farmer selectedFarmer;
    private ListenerRegistration farmerListener;

    public AddDeductionFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_deduction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDate = view.findViewById(R.id.etDate);
        etFarmer = view.findViewById(R.id.etFarmer);
        spCategory = view.findViewById(R.id.spCategory);
        etAmount = view.findViewById(R.id.etAmount);
        etRemaining = view.findViewById(R.id.etRemaining);
        etDescription = view.findViewById(R.id.etDescription);
        btnBack = view.findViewById(R.id.btnBack);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        etDate.setText(sdf.format(calendar.getTime()));
        etDate.setOnClickListener(v -> openDatePicker());

        spCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Advance", "Food", "Medical"}
        ));

        setupFarmerSearch();
        setupAmountWatcher();

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        btnCancel.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        btnSave.setOnClickListener(v -> saveDeduction());
    }

    private void setupFarmerSearch() {
        farmerListener = FirebaseRefs.farmers().addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            farmers.clear();
            List<String> suggestions = new ArrayList<>();

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Farmer farmer = new Farmer();
                    farmer.setId(doc.getId());
                    farmer.setCode(firstString(doc, "code", "farmerCode"));
                    farmer.setName(firstString(doc, "name", "farmerName", "fullName"));
                    farmer.setMobile(firstString(doc, "mobile", "mobileNumber", "whatsappNumber"));

                    farmers.add(farmer);
                    suggestions.add(safe(farmer.getCode()) + " - " + safe(farmer.getName()));
                }
            }

            etFarmer.setAdapter(new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    suggestions
            ));
            etFarmer.setThreshold(1);
        });

        etFarmer.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = parent.getItemAtPosition(position).toString();
            selectedFarmer = null;

            for (Farmer farmer : farmers) {
                String display = safe(farmer.getCode()) + " - " + safe(farmer.getName());

                if (display.equals(selectedText)) {
                    selectedFarmer = farmer;
                    break;
                }
            }
        });

        etFarmer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedFarmer = null;
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                double amount = parseDouble(s.toString());
                etRemaining.setText(String.format(Locale.US, "₹ %.2f", amount));
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void openDatePicker() {
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            etDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveDeduction() {
        if (selectedFarmer == null) {
            Toast.makeText(requireContext(), "Please select farmer from suggestions", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountText = etAmount.getText().toString().trim();

        if (amountText.isEmpty()) {
            etAmount.setError("Amount required");
            return;
        }

        double amount = parseDouble(amountText);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        String deductionId = FirebaseRefs.advanceFood().document().getId();

        Map<String, Object> map = new HashMap<>();
        map.put("id", deductionId);
        map.put("deductionId", deductionId);
        map.put("farmerId", selectedFarmer.getId());
        map.put("farmerCode", selectedFarmer.getCode());
        map.put("farmerName", selectedFarmer.getName());
        map.put("date", etDate.getText().toString());
        map.put("dateMillis", calendar.getTimeInMillis());
        map.put("category", spCategory.getSelectedItem().toString());
        map.put("amount", amount);
        map.put("remaining", amount);
        map.put("status", "PENDING");
        map.put("description", etDescription.getText().toString().trim());
        map.put("source", "MANUAL");
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("updatedAt", FieldValue.serverTimestamp());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        FirebaseRefs.advanceFood().document(deductionId).set(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Deduction saved", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Deduction");
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String firstString(QueryDocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }

        return "";
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Double.parseDouble(value.replace("₹", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onDestroyView() {
        if (farmerListener != null) {
            farmerListener.remove();
            farmerListener = null;
        }

        super.onDestroyView();
    }
}