package com.prashant.milkdairy.ui.Farmer_Management;

import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.prashant.milkdairy.Adapter.FarmerAdapter;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FarmerFragment extends Fragment {

    private RecyclerView recyclerFarmers;
    private ExtendedFloatingActionButton fabAddFarmer;

    private EditText etSearch;
    private TextView tvMilkFilter, tvStatusFilter, tvClearAll;
    private LinearLayout layoutMilkFilter, layoutStatusFilter;

    private View layoutEmptyFarmers;
    private TextView tvEmptyFarmerSub;
    private ProgressBar progressFarmers;

    private View statTotal, statCow, statBuffalo, statActive;

    private FarmerAdapter farmerAdapter;

    private final List<Farmer> fullList = new ArrayList<>();
    private final List<Farmer> filteredList = new ArrayList<>();

    private ListenerRegistration farmersListener;

    private String selectedMilk = "All";
    private String selectedStatus = "All";
    private String currentSearch = "";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    public FarmerFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_farmer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFarmerRecycler();
        setupFilters();
        setupSearch();
        setupFab(view);
        showLoading(true);
        listenFarmersFromFirestore();
    }

    private void initViews(View view) {
        recyclerFarmers = view.findViewById(R.id.recyclerFarmers);
        fabAddFarmer = view.findViewById(R.id.fabAddFarmer);

        etSearch = view.findViewById(R.id.etSearch);
        tvMilkFilter = view.findViewById(R.id.tvMilkFilter);
        tvStatusFilter = view.findViewById(R.id.tvStatusFilter);
        tvClearAll = view.findViewById(R.id.tvClearAll);

        layoutMilkFilter = view.findViewById(R.id.layoutMilkFilter);
        layoutStatusFilter = view.findViewById(R.id.layoutStatusFilter);

        progressFarmers = view.findViewById(R.id.progressFarmers);
        layoutEmptyFarmers = view.findViewById(R.id.layoutEmptyFarmers);
        tvEmptyFarmerSub = view.findViewById(R.id.tvEmptyFarmerSub);

        statTotal = view.findViewById(R.id.statTotal);
        statCow = view.findViewById(R.id.statCow);
        statBuffalo = view.findViewById(R.id.statBuffalo);
        statActive = view.findViewById(R.id.statActive);

        setFilterText();
    }

    private void setupFarmerRecycler() {
        farmerAdapter = new FarmerAdapter(filteredList, new FarmerAdapter.FarmerActionListener() {
            @Override
            public void onEditClick(Farmer farmer) {
                showEditFarmerDialog(farmer);
            }

            @Override
            public void onDeleteClick(Farmer farmer) {
                confirmDeleteFarmer(farmer);
            }

            public void onCallClick(Farmer farmer) {
                confirmCallFarmer(farmer);
            }
        });

        recyclerFarmers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFarmers.setAdapter(farmerAdapter);
        recyclerFarmers.setHasFixedSize(false);
        recyclerFarmers.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupFilters() {
        layoutMilkFilter.setOnClickListener(v -> showMilkFilterMenu());
        layoutStatusFilter.setOnClickListener(v -> showStatusFilterMenu());

        tvClearAll.setOnClickListener(v -> {
            selectedMilk = "All";
            selectedStatus = "All";
            currentSearch = "";
            etSearch.setText("");
            setFilterText();
            applyFilters();
        });
    }

    private void confirmCallFarmer(Farmer farmer) {
        String mobile = safe(farmer.getMobile());

        if (mobile.isEmpty()) {
            Toast.makeText(requireContext(), "Mobile number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Call Farmer")
                .setMessage("Are you sure you want to call " + safe(farmer.getName()) + "?\n\n+91 " + mobile)
                .setPositiveButton("Yes", (dialog, which) -> openDialer(mobile))
                .setNegativeButton("No", null)
                .show();
    }
    private void openDialer(String mobile) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + mobile));
        startActivity(intent);
    }

    private void showMilkFilterMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), layoutMilkFilter);
        popupMenu.getMenu().add("All");
        popupMenu.getMenu().add("Cow");
        popupMenu.getMenu().add("Buffalo");
        popupMenu.getMenu().add("Mix");

        popupMenu.setOnMenuItemClickListener(item -> {
            selectedMilk = item.getTitle().toString();
            setFilterText();
            applyFilters();
            return true;
        });

        popupMenu.show();
    }

    private void showStatusFilterMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), layoutStatusFilter);
        popupMenu.getMenu().add("All");
        popupMenu.getMenu().add("Active");
        popupMenu.getMenu().add("Inactive");

        popupMenu.setOnMenuItemClickListener(item -> {
            selectedStatus = item.getTitle().toString();
            setFilterText();
            applyFilters();
            return true;
        });

        popupMenu.show();
    }

    private void setFilterText() {
        tvMilkFilter.setText(getMilkLabel(selectedMilk));
        tvStatusFilter.setText(getStatusLabel(selectedStatus));
    }

    private String getMilkLabel(String value) {
        if ("Cow".equalsIgnoreCase(value)) return "Cow";
        if ("Buffalo".equalsIgnoreCase(value)) return "Buffalo";
        if ("Mix".equalsIgnoreCase(value)) return "Mix";
        return "All";
    }

    private String getStatusLabel(String value) {
        if ("Active".equalsIgnoreCase(value)) return "● Active";
        if ("Inactive".equalsIgnoreCase(value)) return "● Inactive";
        return "All";
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    currentSearch = s == null ? "" : s.toString().trim();
                    applyFilters();
                };

                searchHandler.postDelayed(searchRunnable, 220);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void listenFarmersFromFirestore() {
        farmersListener = FirebaseRefs.farmers()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    showLoading(false);

                    if (error != null) {
                        Toast.makeText(requireContext(), "Unable to load farmers: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        showEmpty(true, "Check internet connection and try again");
                        return;
                    }

                    fullList.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            fullList.add(toFarmer(doc));
                        }
                    }

                    Collections.sort(fullList, (a, b) ->
                            Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));

                    updateStats();
                    applyFilters();
                });
    }

    private Farmer toFarmer(DocumentSnapshot doc) {
        Farmer farmer = new Farmer();

        farmer.setId(doc.getId());
        farmer.setCode(firstString(doc, "code", "farmerCode"));
        farmer.setName(firstString(doc, "name", "farmerName", "fullName"));
        farmer.setMobile(firstString(doc, "mobile", "mobileNumber", "whatsappNumber"));
        farmer.setMilkType(firstString(doc, "milkType", "type"));
        farmer.setStatus(firstString(doc, "status"));

        if (farmer.getStatus().isEmpty()) {
            farmer.setStatus("Active");
        }

        farmer.setCreatedAtMillis(getMillis(doc, "createdAt"));
        farmer.setUpdatedAtMillis(getMillis(doc, "updatedAt"));

        return farmer;
    }

    private void applyFilters() {
        filteredList.clear();

        String search = currentSearch.toLowerCase(Locale.US);

        for (Farmer farmer : fullList) {
            String code = safe(farmer.getCode());
            String name = safe(farmer.getName());
            String mobile = safe(farmer.getMobile());
            String milkType = safe(farmer.getMilkType());
            String status = safe(farmer.getStatus());

            boolean milkMatch = "All".equalsIgnoreCase(selectedMilk)
                    || milkType.equalsIgnoreCase(selectedMilk);

            boolean statusMatch = "All".equalsIgnoreCase(selectedStatus)
                    || status.equalsIgnoreCase(selectedStatus);

            boolean searchMatch = search.isEmpty()
                    || name.toLowerCase(Locale.US).contains(search)
                    || code.toLowerCase(Locale.US).contains(search)
                    || mobile.contains(search);

            if (milkMatch && statusMatch && searchMatch) {
                filteredList.add(farmer);
            }
        }

        farmerAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            String message = fullList.isEmpty()
                    ? "Add farmer to get started"
                    : "Try changing search or filters";
            showEmpty(true, message);
        } else {
            showEmpty(false, "");
        }
    }

    private void updateStats() {
        int total = fullList.size();
        int cow = 0;
        int buffalo = 0;
        int active = 0;

        for (Farmer farmer : fullList) {
            if ("Cow".equalsIgnoreCase(safe(farmer.getMilkType()))) cow++;
            if ("Buffalo".equalsIgnoreCase(safe(farmer.getMilkType()))) buffalo++;
            if ("Active".equalsIgnoreCase(safe(farmer.getStatus()))) active++;
        }

        bindStat(statTotal, String.valueOf(total), "TOTAL");
        bindStat(statCow, String.valueOf(cow), "COW");
        bindStat(statBuffalo, String.valueOf(buffalo), "BUFFALO");
        bindStat(statActive, String.valueOf(active), "ACTIVE");
    }

    private void bindStat(View statView, String number, String label) {
        if (statView == null) return;

        TextView tvNumber = statView.findViewById(R.id.tvDashnum);
        TextView tvLabel = statView.findViewById(R.id.tvdashtext);

        if (tvNumber != null) tvNumber.setText(number);
        if (tvLabel != null) tvLabel.setText(label);
    }

    private void showLoading(boolean show) {
        if (progressFarmers != null) {
            progressFarmers.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        recyclerFarmers.setVisibility(show ? View.GONE : View.VISIBLE);
        if (layoutEmptyFarmers != null && show) {
            layoutEmptyFarmers.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show, String subtitle) {
        recyclerFarmers.setVisibility(show ? View.GONE : View.VISIBLE);

        if (layoutEmptyFarmers != null) {
            layoutEmptyFarmers.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (tvEmptyFarmerSub != null && subtitle != null) {
            tvEmptyFarmerSub.setText(subtitle);
        }
    }

    private void showEditFarmerDialog(Farmer farmer) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, dpToPx(8), padding, 0);

        EditText etCode = createEditText("Farmer Code", farmer.getCode());
        EditText etName = createEditText("Farmer Name", farmer.getName());
        EditText etMobile = createEditText("Mobile Number", farmer.getMobile());

        Spinner spMilkType = new Spinner(requireContext());
        String[] milkTypes = {"Cow", "Buffalo", "Mix"};
        spMilkType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, milkTypes));
        spMilkType.setSelection(getIndex(milkTypes, farmer.getMilkType()));

        Spinner spStatus = new Spinner(requireContext());
        String[] statuses = {"Active", "Inactive"};
        spStatus.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses));
        spStatus.setSelection(getIndex(statuses, farmer.getStatus()));

        layout.addView(etCode);
        layout.addView(etName);
        layout.addView(etMobile);
        layout.addView(spMilkType);
        layout.addView(spStatus);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Farmer")
                .setView(layout)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String code = etCode.getText().toString().trim();
                String name = etName.getText().toString().trim();
                String mobile = etMobile.getText().toString().trim();

                if (!validateFarmer(code, name, mobile, etCode, etName, etMobile)) {
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                updateFarmer(
                        farmer.getId(),
                        code,
                        name,
                        mobile,
                        spMilkType.getSelectedItem().toString(),
                        spStatus.getSelectedItem().toString(),
                        dialog
                );
            });
        });

        dialog.show();
    }

    private EditText createEditText(String hint, String value) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setText(safe(value));
        editText.setSingleLine(true);
        return editText;
    }

    private boolean validateFarmer(String code,
                                   String name,
                                   String mobile,
                                   EditText etCode,
                                   EditText etName,
                                   EditText etMobile) {
        if (code.isEmpty()) {
            etCode.setError("Farmer code required");
            return false;
        }

        if (name.isEmpty()) {
            etName.setError("Farmer name required");
            return false;
        }

        if (!mobile.matches("\\d{10}")) {
            etMobile.setError("Mobile must be 10 digits");
            return false;
        }

        return true;
    }

    private void updateFarmer(String farmerId,
                              String code,
                              String name,
                              String mobile,
                              String milkType,
                              String status,
                              AlertDialog dialog) {
        if (farmerId == null || farmerId.isEmpty()) {
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("farmerCode", code);
        map.put("name", name);
        map.put("farmerName", name);
        map.put("mobile", mobile);
        map.put("mobileNumber", mobile);
        map.put("milkType", milkType);
        map.put("status", status);
        map.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseRefs.farmers()
                .document(farmerId)
                .update(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Farmer updated", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDeleteFarmer(Farmer farmer) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Farmer")
                .setMessage("Delete " + safe(farmer.getName()) + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteFarmer(farmer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFarmer(Farmer farmer) {
        FirebaseRefs.farmers()
                .document(farmer.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Farmer deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupFab(View view) {
        fabAddFarmer.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_add_farmer)
        );
    }

    private String firstString(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }

        return "";
    }

    private long getMillis(DocumentSnapshot doc, String key) {
        Timestamp timestamp = doc.getTimestamp(key);
        if (timestamp != null) {
            return timestamp.toDate().getTime();
        }

        Object value = doc.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return 0;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int getIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(safe(value))) {
                return i;
            }
        }

        return 0;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (farmersListener != null) {
            farmersListener.remove();
            farmersListener = null;
        }

        super.onDestroyView();
    }
}