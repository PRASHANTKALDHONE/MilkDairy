package com.prashant.milkdairy.ui.Advance_Food_Medical;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.PopupMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.prashant.milkdairy.Adapter.DeductionAdapter;
import com.prashant.milkdairy.Model.DeductionModel;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdvanceFoodFragment extends Fragment {

    private ExtendedFloatingActionButton fabAddFood;
    private RecyclerView rvFoodDeduction;
    private DeductionAdapter adapter;

    private MaterialButton btnThisMonth, btnAll, btnRange;
    private EditText etSearch, etFromDate, etToDate;
    private LinearLayout layoutDateRange;
    private View layoutCategoryFilter, layoutStatusFilter, layoutEmpty;
    private TextView tvCategoryFilter, tvStatusFilter, tvClearAll;
    private ProgressBar progressBar;

    private View statTotal, statAmount, statPending, statCleared;

    private final List<DeductionModel> mainList = new ArrayList<>();
    private final List<DeductionModel> filteredList = new ArrayList<>();

    private ListenerRegistration deductionListener;

    private String dateFilter = "MONTH";
    private String selectedCategory = "All";
    private String selectedStatus = "All";
    private String currentSearch = "";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    public AdvanceFoodFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_advance_food, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecycler();
        setupFilters();
        setupSearch();
        setupFab(view);

        showLoading(true);
        listenDeductions();
    }

    private void initViews(View view) {
        fabAddFood = view.findViewById(R.id.fabAddFood);
        rvFoodDeduction = view.findViewById(R.id.rvFoodDeduction);

        btnThisMonth = view.findViewById(R.id.btnThisMonth);
        btnAll = view.findViewById(R.id.btnAll);
        btnRange = view.findViewById(R.id.btnRange);

        layoutDateRange = view.findViewById(R.id.layoutDateRange);
        layoutCategoryFilter = view.findViewById(R.id.layoutCategoryFilter);
        layoutStatusFilter = view.findViewById(R.id.layoutStatusFilter);

        tvCategoryFilter = view.findViewById(R.id.tvCategoryFilter);
        tvStatusFilter = view.findViewById(R.id.tvStatusFilter);
        tvClearAll = view.findViewById(R.id.tvClearAll);

        etSearch = view.findViewById(R.id.etSearch);
        etFromDate = view.findViewById(R.id.etFromDate);
        etToDate = view.findViewById(R.id.etToDate);

        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        statTotal = view.findViewById(R.id.statTotal);
        statAmount = view.findViewById(R.id.statAmount);
        statPending = view.findViewById(R.id.statPending);
        statCleared = view.findViewById(R.id.statCleared);

        setFilterTexts();
        selectDateButton(btnThisMonth);
    }

    private void setupRecycler() {
        adapter = new DeductionAdapter(filteredList, new DeductionAdapter.DeductionActionListener() {
            @Override
            public void onEditClick(DeductionModel deduction) {
                showEditDeductionDialog(deduction);
            }

            @Override
            public void onDeleteClick(DeductionModel deduction) {
                confirmDelete(deduction);
            }
        });

        rvFoodDeduction.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFoodDeduction.setAdapter(adapter);
        rvFoodDeduction.setItemAnimator(new DefaultItemAnimator());
        rvFoodDeduction.setHasFixedSize(false);
    }

    private void setupFilters() {
        btnThisMonth.setOnClickListener(v -> {
            dateFilter = "MONTH";
            layoutDateRange.setVisibility(View.GONE);
            selectDateButton(btnThisMonth);
            applyFilter();
        });

        btnAll.setOnClickListener(v -> {
            dateFilter = "ALL";
            layoutDateRange.setVisibility(View.GONE);
            selectDateButton(btnAll);
            applyFilter();
        });

        btnRange.setOnClickListener(v -> {
            dateFilter = "RANGE";
            layoutDateRange.setVisibility(View.VISIBLE);
            selectDateButton(btnRange);
            applyFilter();
        });

        etFromDate.setOnClickListener(v -> openDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> openDatePicker(etToDate));

        layoutCategoryFilter.setOnClickListener(v -> showCategoryMenu());
        layoutStatusFilter.setOnClickListener(v -> showStatusMenu());

        tvClearAll.setOnClickListener(v -> {
            dateFilter = "MONTH";
            selectedCategory = "All";
            selectedStatus = "All";
            currentSearch = "";

            etSearch.setText("");
            etFromDate.setText("");
            etToDate.setText("");

            layoutDateRange.setVisibility(View.GONE);
            selectDateButton(btnThisMonth);
            setFilterTexts();
            applyFilter();
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    currentSearch = s == null ? "" : s.toString().trim();
                    applyFilter();
                };

                searchHandler.postDelayed(searchRunnable, 220);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showCategoryMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), layoutCategoryFilter);
        menu.getMenu().add("All");
        menu.getMenu().add("Advance");
        menu.getMenu().add("Food");
        menu.getMenu().add("Medical");
        menu.getMenu().add("Inventory");

        menu.setOnMenuItemClickListener(item -> {
            selectedCategory = item.getTitle().toString();
            setFilterTexts();
            applyFilter();
            return true;
        });

        menu.show();
    }

    private void showStatusMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), layoutStatusFilter);
        menu.getMenu().add("All");
        menu.getMenu().add("PENDING");
        menu.getMenu().add("PARTIAL");
        menu.getMenu().add("CLEARED");

        menu.setOnMenuItemClickListener(item -> {
            selectedStatus = item.getTitle().toString();
            setFilterTexts();
            applyFilter();
            return true;
        });

        menu.show();
    }

    private void setFilterTexts() {
        tvCategoryFilter.setText(selectedCategory.equals("All") ? "All" : selectedCategory);
        tvStatusFilter.setText(selectedStatus.equals("All") ? "All" : selectedStatus);
    }

    private void selectDateButton(MaterialButton selected) {
        setDateButton(btnThisMonth, selected == btnThisMonth);
        setDateButton(btnAll, selected == btnAll);
        setDateButton(btnRange, selected == btnRange);
    }

    private void setDateButton(MaterialButton button, boolean selected) {
        if (selected) {
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#009688")));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#009688")));
        } else {
            button.setTextColor(Color.parseColor("#667085"));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F4F7FA")));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
        }
    }

    private void listenDeductions() {
        deductionListener = FirebaseRefs.advanceFood()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    showLoading(false);

                    if (error != null) {
                        Toast.makeText(requireContext(), "Unable to load deductions: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        showEmpty(true);
                        return;
                    }

                    mainList.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            mainList.add(toModel(doc));
                        }
                    }

                    updateDashboard();
                    applyFilter();
                });
    }

    private DeductionModel toModel(DocumentSnapshot doc) {
        DeductionModel m = new DeductionModel();

        m.setId(doc.getId());
        m.setFarmerId(firstString(doc, "farmerId"));
        m.setFarmerCode(firstString(doc, "farmerCode", "code"));
        m.setFarmerName(firstString(doc, "farmerName", "name"));
        m.setDate(firstString(doc, "date"));
        m.setCategory(firstString(doc, "category", "type"));
        m.setDescription(firstString(doc, "description", "title"));
        m.setDateMillis(firstLong(doc, "dateMillis"));
        m.setCreatedAtMillis(getTimestampMillis(doc, "createdAt"));
        m.setUpdatedAtMillis(getTimestampMillis(doc, "updatedAt"));
        m.setAmount(firstDouble(doc, "amount", "totalAmount"));
        m.setRemaining(firstDouble(doc, "remaining", "remainingAmount"));

        String status = firstString(doc, "status");
        if (status.isEmpty()) {
            status = computeStatus(m.getAmount(), m.getRemaining());
        }

        m.setStatus(status.toUpperCase(Locale.US));

        if (m.getDateMillis() == 0 && m.getCreatedAtMillis() > 0) {
            m.setDateMillis(m.getCreatedAtMillis());
        }

        return m;
    }

    private void applyFilter() {
        filteredList.clear();

        String search = currentSearch.toLowerCase(Locale.US);

        for (DeductionModel m : mainList) {
            boolean searchMatch = search.isEmpty()
                    || safe(m.getFarmerName()).toLowerCase(Locale.US).contains(search)
                    || safe(m.getFarmerCode()).toLowerCase(Locale.US).contains(search)
                    || safe(m.getDescription()).toLowerCase(Locale.US).contains(search)
                    || safe(m.getCategory()).toLowerCase(Locale.US).contains(search);

            boolean categoryMatch = "All".equalsIgnoreCase(selectedCategory)
                    || safe(m.getCategory()).equalsIgnoreCase(selectedCategory);

            boolean statusMatch = "All".equalsIgnoreCase(selectedStatus)
                    || safe(m.getStatus()).equalsIgnoreCase(selectedStatus);

            boolean dateMatch = matchesDateFilter(m);

            if (searchMatch && categoryMatch && statusMatch && dateMatch) {
                filteredList.add(m);
            }
        }

        adapter.updateList(filteredList);
        showEmpty(filteredList.isEmpty());
    }

    private boolean matchesDateFilter(DeductionModel m) {
        if ("ALL".equals(dateFilter)) return true;

        Calendar item = Calendar.getInstance();
        item.setTimeInMillis(m.getDateMillis());

        Calendar now = Calendar.getInstance();

        if ("MONTH".equals(dateFilter)) {
            return item.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                    && item.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        }

        if ("RANGE".equals(dateFilter)) {
            try {
                if (etFromDate.getText().toString().trim().isEmpty()
                        || etToDate.getText().toString().trim().isEmpty()) {
                    return true;
                }

                long from = sdf.parse(etFromDate.getText().toString().trim()).getTime();
                long to = sdf.parse(etToDate.getText().toString().trim()).getTime();

                return m.getDateMillis() >= from && m.getDateMillis() <= to;
            } catch (Exception e) {
                return true;
            }
        }

        return true;
    }

    private void updateDashboard() {
        int totalCount = mainList.size();
        int pending = 0;
        int cleared = 0;

        double totalAmount = 0;
        double totalRemaining = 0;

        for (DeductionModel m : mainList) {
            totalAmount += m.getAmount();
            totalRemaining += m.getRemaining();

            if ("CLEARED".equalsIgnoreCase(m.getStatus())) {
                cleared++;
            } else {
                pending++;
            }
        }

        bindStat(statTotal, String.valueOf(totalCount), "TOTAL");
        bindStat(statAmount, "₹" + shortMoney(totalAmount), "AMOUNT");
        bindStat(statPending, String.valueOf(pending), "PENDING");
        bindStat(statCleared, String.valueOf(cleared), "CLEARED");
    }

    private void bindStat(View statView, String number, String label) {
        if (statView == null) return;

        TextView tvNumber = statView.findViewById(R.id.tvDashnum);
        TextView tvLabel = statView.findViewById(R.id.tvdashtext);

        if (tvNumber != null) tvNumber.setText(number);
        if (tvLabel != null) tvLabel.setText(label);
    }

    private void openDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            editText.setText(sdf.format(calendar.getTime()));
            applyFilter();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showEditDeductionDialog(DeductionModel deduction) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, dpToPx(8), padding, 0);

        Spinner spCategory = new Spinner(requireContext());
        String[] categories = {"Advance", "Food", "Medical", "Inventory"};
        spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
        spCategory.setSelection(getIndex(categories, deduction.getCategory()));

        Spinner spStatus = new Spinner(requireContext());
        String[] statuses = {"PENDING", "PARTIAL", "CLEARED"};
        spStatus.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses));
        spStatus.setSelection(getIndex(statuses, deduction.getStatus()));

        EditText etAmount = createEditText("Amount", String.valueOf(deduction.getAmount()));
        EditText etRemaining = createEditText("Remaining", String.valueOf(deduction.getRemaining()));
        EditText etDescription = createEditText("Description", deduction.getDescription());

        layout.addView(spCategory);
        layout.addView(spStatus);
        layout.addView(etAmount);
        layout.addView(etRemaining);
        layout.addView(etDescription);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Deduction")
                .setView(layout)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double amount = parseDouble(etAmount.getText().toString());
            double remaining = parseDouble(etRemaining.getText().toString());

            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                return;
            }

            if (remaining < 0 || remaining > amount) {
                etRemaining.setError("Remaining must be between 0 and amount");
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            updateDeduction(
                    deduction.getId(),
                    spCategory.getSelectedItem().toString(),
                    spStatus.getSelectedItem().toString(),
                    amount,
                    remaining,
                    etDescription.getText().toString().trim(),
                    dialog
            );
        }));

        dialog.show();
    }

    private EditText createEditText(String hint, String value) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setSingleLine(false);
        editText.setText(safe(value));
        return editText;
    }

    private void updateDeduction(String id,
                                 String category,
                                 String status,
                                 double amount,
                                 double remaining,
                                 String description,
                                 AlertDialog dialog) {
        if (id == null || id.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("category", category);
        map.put("amount", amount);
        map.put("remaining", remaining);
        map.put("status", status);
        map.put("description", description);
        map.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseRefs.advanceFood()
                .document(id)
                .update(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Deduction updated", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete(DeductionModel deduction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Deduction")
                .setMessage("Delete deduction for " + safe(deduction.getFarmerName()) + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDeduction(deduction))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDeduction(DeductionModel deduction) {
        FirebaseRefs.advanceFood()
                .document(deduction.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Deduction deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupFab(View view) {
        fabAddFood.setText("Add Deduction");
        fabAddFood.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_nav_advance_food_to_addDeductionFragment)
        );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFoodDeduction.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFoodDeduction.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String computeStatus(double amount, double remaining) {
        if (remaining <= 0) return "CLEARED";
        if (remaining < amount) return "PARTIAL";
        return "PENDING";
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

    private double firstDouble(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value != null) {
                try {
                    return Double.parseDouble(value.toString().replace("₹", "").trim());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private long firstLong(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        return 0;
    }

    private long getTimestampMillis(DocumentSnapshot doc, String key) {
        Timestamp timestamp = doc.getTimestamp(key);
        return timestamp == null ? 0 : timestamp.toDate().getTime();
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Double.parseDouble(value.replace("₹", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String shortMoney(double value) {
        if (value >= 100000) {
            return String.format(Locale.US, "%.1fL", value / 100000);
        }

        if (value >= 1000) {
            return String.format(Locale.US, "%.1fK", value / 1000);
        }

        return String.format(Locale.US, "%.0f", value);
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

        if (deductionListener != null) {
            deductionListener.remove();
            deductionListener = null;
        }

        super.onDestroyView();
    }
}