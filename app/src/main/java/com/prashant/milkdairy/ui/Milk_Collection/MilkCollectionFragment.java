package com.prashant.milkdairy.ui.Milk_Collection;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prashant.milkdairy.Adapter.MilkCollectionAdapter;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.Model.MilkCollectionModel;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MilkCollectionFragment extends Fragment {

    private static final String FILTER_DAILY = "Daily";
    private static final String FILTER_WEEKLY = "Weekly";
    private static final String FILTER_MONTHLY = "Monthly";

    private MaterialCardView btnAddMilkEntry;
    private View layoutFilter, layoutDatePicker;
    private TextView tvSelectedFilter, tvFilterDate, tvRecordCount;
    private TextView tvMorning, tvEvening;

    private TextView tvCowLiters, tvCowRatio, tvCowMorning, tvCowEvening;
    private TextView tvBuffaloLiters, tvBuffaloRatio, tvBuffaloMorning, tvBuffaloEvening;
    private TextView tvMixLiters, tvMixRatio, tvMixMorning, tvMixEvening;

    private RecyclerView recyclerMilkCollection;
    private ProgressBar progressMilkCollection;
    private LinearLayout layoutEmptyMilkCollection;
    private TextView tvEmptyMilkCollectionSub;
    private MilkCollectionAdapter adapter;

    private View cowMilkFill, buffaloMilkFill, mixMilkFill;
    private TextView tvCanCapacity;
    private ImageView btnCanSettings;

    private double cowCanCapacity = 40;
    private double buffaloCanCapacity = 40;
    private double mixCanCapacity = 40;

    private EditText etDate, etFarmerCode, etLiters, etFat, etSnf, etRate, etTotal, etRemarks;
    private AutoCompleteTextView etFarmer;
    private Spinner spShift;
    private MaterialButton layoutCow, layoutBuffalo, layoutMix, btnSave;

    private BottomSheetDialog addEntryDialog;

    private final List<Farmer> farmers = new ArrayList<>();
    private final List<MilkCollectionModel> mainList = new ArrayList<>();
    private final List<MilkCollectionModel> filteredList = new ArrayList<>();

    private Farmer selectedFarmer;
    private String selectedMilkType = "Cow";
    private String selectedFilter = FILTER_DAILY;
    private String selectedShiftSummary = "Morning";

    private ListenerRegistration farmerListener, milkListener;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    public MilkCollectionFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_milk_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecycler();
        setupHeaderActions();

        loadCanCapacity();
        showLoading(true);
        setupFarmersListener();
        listenMilkCollections();

        tvFilterDate.setText(dateFormat.format(calendar.getTime()));
        tvSelectedFilter.setText(selectedFilter);
        setShiftSummary("Morning");
    }

    private void initViews(View view) {
        btnAddMilkEntry = view.findViewById(R.id.btnAddMilkEntry);
        layoutFilter = view.findViewById(R.id.layoutFilter);
        layoutDatePicker = view.findViewById(R.id.layoutDatePicker);

        tvSelectedFilter = view.findViewById(R.id.tvSelectedFilter);
        tvFilterDate = view.findViewById(R.id.etFilterDate);
        tvRecordCount = view.findViewById(R.id.tvRecordCount);

        tvMorning = view.findViewById(R.id.tvMorning);
        tvEvening = view.findViewById(R.id.tvEvening);

        tvCowLiters = view.findViewById(R.id.tvCowLiters);
        tvCowRatio = view.findViewById(R.id.tvCowRatio);
        tvCowMorning = view.findViewById(R.id.tvCowMorning);
        tvCowEvening = view.findViewById(R.id.tvCowEvening);

        tvBuffaloLiters = view.findViewById(R.id.tvBuffaloLiters);
        tvBuffaloRatio = view.findViewById(R.id.tvBuffaloRatio);
        tvBuffaloMorning = view.findViewById(R.id.tvBuffaloMorning);
        tvBuffaloEvening = view.findViewById(R.id.tvBuffaloEvening);

        cowMilkFill = view.findViewById(R.id.cowMilkFill);
        buffaloMilkFill = view.findViewById(R.id.buffaloMilkFill);
        mixMilkFill = view.findViewById(R.id.mixMilkFill);

        tvMixLiters = view.findViewById(R.id.tvMixLiters);
        tvMixRatio = view.findViewById(R.id.tvMixRatio);
        tvMixMorning = view.findViewById(R.id.tvMixMorning);
        tvMixEvening = view.findViewById(R.id.tvMixEvening);
        tvCanCapacity = view.findViewById(R.id.tvCanCapacity);
        btnCanSettings = view.findViewById(R.id.btnCanSettings);

        recyclerMilkCollection = view.findViewById(R.id.recyclerMilkCollection);

        progressMilkCollection =
                view.findViewById(R.id.progressMilkCollection);

        layoutEmptyMilkCollection =
                view.findViewById(R.id.layoutEmptyMilkCollection);

        tvEmptyMilkCollectionSub =
                view.findViewById(R.id.tvEmptyMilkCollectionSub);
    }

    private void setupRecycler() {

        adapter = new MilkCollectionAdapter(filteredList);

        recyclerMilkCollection.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerMilkCollection.setAdapter(adapter);

        recyclerMilkCollection.setHasFixedSize(false);

        recyclerMilkCollection.setNestedScrollingEnabled(true);

        recyclerMilkCollection.setItemAnimator(null);
    }

    private void setupHeaderActions() {
        btnAddMilkEntry.setOnClickListener(v -> showAddEntryBottomSheet());

        layoutFilter.setOnClickListener(v -> showFilterMenu());

        layoutDatePicker.setOnClickListener(v ->
                showDatePicker(tvFilterDate, () -> {
                    selectedFilter = FILTER_DAILY;
                    tvSelectedFilter.setText(selectedFilter);

                    applyFilterWithLoader();
                })
        );
        btnCanSettings.setOnClickListener(v -> {
            showCanCapacityDialog();
        });

        tvMorning.setOnClickListener(v -> setShiftSummary("Morning"));
        tvEvening.setOnClickListener(v -> setShiftSummary("Evening"));
    }

    private void showFilterMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), layoutFilter);
        menu.getMenu().add(FILTER_DAILY);
        menu.getMenu().add(FILTER_WEEKLY);
        menu.getMenu().add(FILTER_MONTHLY);

        menu.setOnMenuItemClickListener(item -> {

            selectedFilter = item.getTitle().toString();

            tvSelectedFilter.setText(selectedFilter);

            applyFilterWithLoader();

            return true;
        });

        menu.show();
    }

    private void setShiftSummary(String shift) {
        selectedShiftSummary = shift;

        boolean morning = "Morning".equalsIgnoreCase(shift);
        tvMorning.setBackgroundResource(morning ? R.drawable.bg_summary_card : android.R.color.transparent);
        tvEvening.setBackgroundResource(morning ? android.R.color.transparent : R.drawable.bg_summary_card);

        tvMorning.setTextColor(ContextCompat.getColor(requireContext(),
                morning ? android.R.color.white : android.R.color.black));
        tvEvening.setTextColor(ContextCompat.getColor(requireContext(),
                morning ? android.R.color.black : android.R.color.white));

        updatePlatformSummary();
    }

    private void showAddEntryBottomSheet() {
        addEntryDialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottomsheet_add_collection, null);

        addEntryDialog.setContentView(sheet);

        initBottomSheetViews(sheet);
        setupBottomSheetDefaults();
        setupBottomSheetActions(sheet);

        addEntryDialog.show();
    }

    private void initBottomSheetViews(View sheet) {
        etDate = sheet.findViewById(R.id.etDate);
        etFarmer = sheet.findViewById(R.id.etFarmer);
        etFarmerCode = sheet.findViewById(R.id.etFarmerCode);
        etLiters = sheet.findViewById(R.id.etLiters);
        etFat = sheet.findViewById(R.id.etFat);
        etSnf = sheet.findViewById(R.id.etSnf);
        etRate = sheet.findViewById(R.id.etRate);
        etTotal = sheet.findViewById(R.id.etTotal);
        etRemarks = sheet.findViewById(R.id.etRemarks);

        spShift = sheet.findViewById(R.id.spShift);

        layoutCow = sheet.findViewById(R.id.layoutCow);
        layoutBuffalo = sheet.findViewById(R.id.layoutBuffalo);
        layoutMix = sheet.findViewById(R.id.layoutMix);

        btnSave = sheet.findViewById(R.id.btnSave);
    }

    private void setupBottomSheetDefaults() {
        selectedFarmer = null;
        selectedMilkType = "Cow";

        etDate.setText(dateFormat.format(Calendar.getInstance().getTime()));
        etDate.setOnClickListener(v -> showDatePicker(etDate, null));

        spShift.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Morning", "Evening"}
        ));

        setupFarmerSearchInSheet();
        setupMilkTypeSelectionInSheet();
        setupCalculationWatcher();
        updateMilkSelection();
    }

    private void setupBottomSheetActions(View sheet) {
        View close = sheet.findViewById(R.id.btnCloseForm);
        if (close != null) {
            close.setOnClickListener(v -> addEntryDialog.dismiss());
        }

        btnSave.setOnClickListener(v -> saveCollection());
    }

    private void setupFarmerSearchInSheet() {
        List<String> suggestions = new ArrayList<>();

        for (Farmer farmer : farmers) {
            suggestions.add(displayFarmer(farmer));
        }

        etFarmer.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        ));
        etFarmer.setThreshold(1);

        etFarmer.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = parent.getItemAtPosition(position).toString();
            selectedFarmer = findFarmerByDisplayText(selectedText);

            if (selectedFarmer != null) {
                etFarmerCode.setText(safe(selectedFarmer.getCode()));

                String farmerMilkType = safe(selectedFarmer.getMilkType());
                selectedMilkType = farmerMilkType.isEmpty() ? "Cow" : farmerMilkType;

                updateMilkSelection();
                calculateTotal();
            }
        });

        etFarmer.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedFarmer = null;
                etFarmerCode.setText("");
            }
            public void afterTextChanged(Editable s) {}
        });
    }
    private void loadCanCapacity() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("MilkCansettings")
                .document("milk_can_capacity")
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        Double cow = doc.getDouble("cowCapacity");
                        Double buffalo = doc.getDouble("buffaloCapacity");
                        Double mix = doc.getDouble("mixCapacity");

                        cowCanCapacity = cow == null ? 40 : cow;
                        buffaloCanCapacity = buffalo == null ? 40 : buffalo;
                        mixCanCapacity = mix == null ? 40 : mix;
                    }

                    updateCapacityText();
                    updatePlatformSummary();
                });
    }
    private void updateCapacityText() {

        if (tvCanCapacity == null) return;

        tvCanCapacity.setText(
                String.format(
                        Locale.US,
                        "Cow %.0fL • Buffalo %.0fL • Mix %.0fL",
                        cowCanCapacity,
                        buffaloCanCapacity,
                        mixCanCapacity
                )
        );
    }

    private void setupMilkTypeSelectionInSheet() {
        layoutCow.setOnClickListener(v -> {
            selectedMilkType = "Cow";
            updateMilkSelection();
            calculateTotal();
        });

        layoutBuffalo.setOnClickListener(v -> {
            selectedMilkType = "Buffalo";
            updateMilkSelection();
            calculateTotal();
        });

        layoutMix.setOnClickListener(v -> {
            selectedMilkType = "Mix";
            updateMilkSelection();
            calculateTotal();
        });
    }

    private void updateMilkSelection() {
        if (layoutCow == null || layoutBuffalo == null || layoutMix == null) return;

        setMilkButton(layoutCow, "Cow");
        setMilkButton(layoutBuffalo, "Buffalo");
        setMilkButton(layoutMix, "Mix");
    }

    private void setMilkButton(MaterialButton button, String type) {
        boolean selected = type.equalsIgnoreCase(selectedMilkType);

        int bgColor = selected ? color("#ECFDF5") : color("#F4F7FA");
        int textColor = selected ? color("#10B981") : color("#667085");

        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private void setupCalculationWatcher() {
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }
            public void afterTextChanged(Editable s) {}
        };

        etLiters.addTextChangedListener(watcher);
        etFat.addTextChangedListener(watcher);
        etSnf.addTextChangedListener(watcher);
    }

    private void calculateTotal() {
        if (etLiters == null || etFat == null || etSnf == null) return;

        String litersText = etLiters.getText().toString().trim();
        String fatText = etFat.getText().toString().trim();
        String snfText = etSnf.getText().toString().trim();

        if (litersText.isEmpty() || fatText.isEmpty() || snfText.isEmpty()) {
            etRate.setText("");
            etTotal.setText("");
            return;
        }

        double liters = parseDouble(litersText);
        double fat = parseDouble(fatText);
        double snf = parseDouble(snfText);

        if (liters <= 0 || fat <= 0 || snf <= 0) {
            etRate.setText("");
            etTotal.setText("");
            return;
        }

        loadRateFromChart(selectedMilkType, fat, snf, rate -> {
            if (!isAdded()) return;

            if (rate <= 0) {
                etRate.setText("");
                etTotal.setText("");
                return;
            }

            double total = liters * rate;
            etRate.setText(String.format(Locale.US, "%.2f", rate));
            etTotal.setText(String.format(Locale.US, "%.2f", total));
        });
    }

    private void setupFarmersListener() {
        farmerListener = FirebaseRefs.farmers().addSnapshotListener((snapshots, error) -> {
            if (!isAdded()) return;

            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            farmers.clear();

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Farmer farmer = new Farmer();
                    farmer.setId(doc.getId());
                    farmer.setCode(doc.getString("code"));
                    farmer.setName(doc.getString("name"));
                    farmer.setMobile(doc.getString("mobile"));
                    farmer.setMilkType(doc.getString("milkType"));
                    farmer.setStatus(doc.getString("status"));
                    farmers.add(farmer);
                }
            }
        });
    }

    private void listenMilkCollections() {
        milkListener = FirebaseRefs.milkCollection().addSnapshotListener((snapshots, error) -> {
            showLoading(false);
            if (!isAdded()) return;

            if (error != null) {

                Toast.makeText(
                        requireContext(),
                        error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                showEmpty(
                        true,
                        "Check internet connection"
                );

                return;
            }

            mainList.clear();

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    MilkCollectionModel m = new MilkCollectionModel();
                    m.setId(doc.getId());
                    m.setFarmerId(doc.getString("farmerId"));
                    m.setDate(doc.getString("date"));
                    m.setShift(doc.getString("shift"));
                    m.setFarmerCode(doc.getString("farmerCode"));
                    m.setFarmerName(doc.getString("farmerName"));
                    m.setMobile(doc.getString("mobile"));
                    m.setMilkType(doc.getString("milkType"));
                    m.setRemarks(doc.getString("remarks"));
                    m.setLiters(getDouble(doc, "liters"));
                    m.setFat(getDouble(doc, "fat"));
                    m.setSnf(getDouble(doc, "snf"));
                    m.setRate(getDouble(doc, "rate"));
                    m.setTotal(getDouble(doc, "total"));
                    mainList.add(m);
                }
            }

            Collections.sort(mainList, (a, b) ->
                    Long.compare(parseDateMillis(b.getDate()), parseDateMillis(a.getDate())));

            applyFilterAndSummary();
        });
    }

    private void saveCollection() {
        Farmer farmer = findFarmerFromInput();

        if (farmer == null) {
            Toast.makeText(requireContext(), "Please select a valid farmer", Toast.LENGTH_SHORT).show();
            return;
        }

        String litersText = etLiters.getText().toString().trim();
        String fatText = etFat.getText().toString().trim();
        String snfText = etSnf.getText().toString().trim();
        String rateText = etRate.getText().toString().trim();
        String totalText = etTotal.getText().toString().trim();

        if (litersText.isEmpty() || fatText.isEmpty() || snfText.isEmpty()
                || rateText.isEmpty() || totalText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all milk details and valid rate", Toast.LENGTH_SHORT).show();
            return;
        }

        double liters = parseDouble(litersText);
        double fat = parseDouble(fatText);
        double snf = parseDouble(snfText);
        double rate = parseDouble(rateText);
        double total = parseDouble(totalText);

        if (liters <= 0 || fat <= 0 || snf <= 0 || rate <= 0 || total <= 0) {
            Toast.makeText(requireContext(), "Invalid milk details or rate", Toast.LENGTH_SHORT).show();
            return;
        }

        String entryId = FirebaseRefs.milkCollection().document().getId();
        String date = etDate.getText().toString().trim();

        Map<String, Object> map = new HashMap<>();
        map.put("id", entryId);
        map.put("farmerId", farmer.getId());
        map.put("farmerCode", farmer.getCode());
        map.put("farmerName", farmer.getName());
        map.put("mobile", farmer.getMobile());
        map.put("date", date);
        map.put("dateMillis", parseDateMillis(date));
        map.put("shift", spShift.getSelectedItem().toString());
        map.put("milkType", selectedMilkType);
        map.put("rateChartType", milkTypeToChartDocId(selectedMilkType));
        map.put("rateChartKey", chartKey(round1(fat), round1(snf)));
        map.put("liters", liters);
        map.put("fat", fat);
        map.put("snf", snf);
        map.put("rate", rate);
        map.put("total", total);
        map.put("remarks", etRemarks.getText().toString().trim());
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("updatedAt", FieldValue.serverTimestamp());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        saveMilkWithDeductionRecovery(entryId, farmer.getId(), map, total);
    }

    private void saveMilkWithDeductionRecovery(String entryId,
                                               String farmerId,
                                               Map<String, Object> milkMap,
                                               double farmerEarning) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseRefs.advanceFood()
                .whereEqualTo("farmerId", farmerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(querySnapshot.getDocuments());

                    Collections.sort(docs, Comparator.comparingLong(d -> {
                        Long value = d.getLong("dateMillis");
                        return value == null ? 0 : value;
                    }));

                    db.runTransaction(transaction -> {
                        List<DocumentSnapshot> latestDocs = new ArrayList<>();

                        for (DocumentSnapshot doc : docs) {
                            latestDocs.add(transaction.get(doc.getReference()));
                        }

                        double earningLeft = farmerEarning;
                        double totalDeducted = 0;

                        for (DocumentSnapshot doc : latestDocs) {
                            String status = doc.getString("status");
                            Double remainingValue = doc.getDouble("remaining");

                            if ("CLEARED".equalsIgnoreCase(status)) continue;

                            double remaining = remainingValue == null ? 0 : remainingValue;

                            if (remaining <= 0 || earningLeft <= 0) continue;

                            double deduction = Math.min(earningLeft, remaining);
                            double newRemaining = Math.max(0, remaining - deduction);

                            earningLeft -= deduction;
                            totalDeducted += deduction;

                            String newStatus = newRemaining == 0 ? "CLEARED" : "PARTIAL";

                            transaction.update(doc.getReference(), "remaining", newRemaining);
                            transaction.update(doc.getReference(), "status", newStatus);
                            transaction.update(doc.getReference(), "updatedAt", FieldValue.serverTimestamp());
                        }

                        milkMap.put("grossAmount", farmerEarning);
                        milkMap.put("deductedAmount", totalDeducted);
                        milkMap.put("netPayable", earningLeft);

                        transaction.set(FirebaseRefs.milkCollection().document(entryId), milkMap);

                        return null;
                    }).addOnSuccessListener(unused -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");

                        Toast.makeText(requireContext(), "Milk collection saved", Toast.LENGTH_SHORT).show();

                        tvFilterDate.setText(String.valueOf(milkMap.get("date")));
                        selectedFilter = FILTER_DAILY;
                        tvSelectedFilter.setText(selectedFilter);

                        if (addEntryDialog != null) {
                            addEntryDialog.dismiss();
                        }

                        applyFilterAndSummary();
                    }).addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Collection");
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilterAndSummary() {
        filteredList.clear();

        for (MilkCollectionModel m : mainList) {
            if (matchesSelectedFilter(m)) {
                filteredList.add(m);
            }
        }

        adapter.updateList(filteredList);

        tvRecordCount.setText(
                filteredList.size() +
                        (filteredList.size() == 1
                                ? " Record"
                                : " Records")
        );

        if (filteredList.isEmpty()) {

            String msg = mainList.isEmpty()
                    ? "Add milk collection to get started"
                    : "No records found for selected filter";

            showEmpty(true, msg);

        } else {

            showEmpty(false, "");
        }

        updatePlatformSummary();
    }
    private void applyFilterWithLoader() {

        showLoading(true);

        recyclerMilkCollection.setVisibility(View.GONE);

        if (layoutEmptyMilkCollection != null) {
            layoutEmptyMilkCollection.setVisibility(View.GONE);
        }

        new android.os.Handler().postDelayed(() -> {

            if (!isAdded()) return;

            showLoading(false);

            recyclerMilkCollection.setVisibility(View.VISIBLE);

            applyFilterAndSummary();

        }, 1000);
    }
    private void showLoading(boolean show) {

        if (progressMilkCollection != null) {

            progressMilkCollection.setVisibility(
                    show ? View.VISIBLE : View.GONE
            );
        }

        if (show) {

            recyclerMilkCollection.setVisibility(View.GONE);

            if (layoutEmptyMilkCollection != null) {
                layoutEmptyMilkCollection.setVisibility(View.GONE);
            }

        }
    }
    private void showEmpty(boolean show, String subtitle) {

        recyclerMilkCollection.setVisibility(
                show ? View.GONE : View.VISIBLE
        );

        if (layoutEmptyMilkCollection != null) {
            layoutEmptyMilkCollection.setVisibility(
                    show ? View.VISIBLE : View.GONE
            );
        }

        if (tvEmptyMilkCollectionSub != null) {
            tvEmptyMilkCollectionSub.setText(subtitle);
        }
    }

    private boolean matchesSelectedFilter(MilkCollectionModel m) {
        long selectedMillis = parseDateMillis(tvFilterDate.getText().toString());
        long itemMillis = parseDateMillis(m.getDate());

        if (FILTER_DAILY.equals(selectedFilter)) {
            return isSameDay(selectedMillis, itemMillis);
        }

        if (FILTER_WEEKLY.equals(selectedFilter)) {
            return isSameWeek(selectedMillis, itemMillis);
        }

        if (FILTER_MONTHLY.equals(selectedFilter)) {
            return isSameMonth(selectedMillis, itemMillis);
        }

        return true;
    }

    private void updatePlatformSummary() {
        double cowM = 0, cowE = 0, buffaloM = 0, buffaloE = 0, mixM = 0, mixE = 0;

        for (MilkCollectionModel m : mainList) {
            if (!isSameDay(parseDateMillis(tvFilterDate.getText().toString()), parseDateMillis(m.getDate()))) {
                continue;
            }

            if ("Cow".equalsIgnoreCase(m.getMilkType()) && "Morning".equalsIgnoreCase(m.getShift())) {
                cowM += m.getLiters();
            }

            if ("Cow".equalsIgnoreCase(m.getMilkType()) && "Evening".equalsIgnoreCase(m.getShift())) {
                cowE += m.getLiters();
            }

            if ("Buffalo".equalsIgnoreCase(m.getMilkType()) && "Morning".equalsIgnoreCase(m.getShift())) {
                buffaloM += m.getLiters();
            }

            if ("Buffalo".equalsIgnoreCase(m.getMilkType()) && "Evening".equalsIgnoreCase(m.getShift())) {
                buffaloE += m.getLiters();
            }

            if ("Mix".equalsIgnoreCase(m.getMilkType()) && "Morning".equalsIgnoreCase(m.getShift())) {
                mixM += m.getLiters();
            }

            if ("Mix".equalsIgnoreCase(m.getMilkType()) && "Evening".equalsIgnoreCase(m.getShift())) {
                mixE += m.getLiters();
            }
        }

        setPlatform(cowMilkFill, tvCowLiters, tvCowRatio, tvCowMorning, tvCowEvening, cowM, cowE,cowCanCapacity);
        setPlatform(buffaloMilkFill, tvBuffaloLiters, tvBuffaloRatio, tvBuffaloMorning, tvBuffaloEvening, buffaloM, buffaloE,buffaloCanCapacity);
        setPlatform(mixMilkFill, tvMixLiters, tvMixRatio, tvMixMorning, tvMixEvening, mixM, mixE,mixCanCapacity);
    }

    private void setPlatform(
            View fillView,
            TextView totalView,
            TextView ratioView,
            TextView morningView,
            TextView eveningView,
            double morning,
            double evening,
            double capacity) {

        double visibleLiters =
                "Evening".equalsIgnoreCase(selectedShiftSummary)
                        ? evening
                        : morning;

        totalView.setText(
                String.format(Locale.US,
                        "%.1fL",
                        visibleLiters));

        ratioView.setText(
                String.format(Locale.US,
                        "%.1f / %.0fL",
                        visibleLiters,
                        capacity));

        morningView.setText(
                String.format(Locale.US,
                        "%.1f",
                        morning));

        eveningView.setText(
                String.format(Locale.US,
                        "%.1f",
                        evening));

        updateCanFill(
                fillView,
                visibleLiters,
                capacity
        );
    }
    private void updateCanFill(
            View fillView,
            double liters,
            double capacity) {

        if (fillView == null) return;

        fillView.post(() -> {

            View parent = (View) fillView.getParent();

            if (parent == null) return;

            // Keep some space for tank border and rounded corners
            int usableHeight = parent.getHeight() - 4;

            if (usableHeight <= 0) return;

            double percentage = liters / capacity;

            // Prevent overflow
            percentage = Math.max(0.0,
                    Math.min(1.0, percentage));

            int targetHeight =
                    (int) (usableHeight * percentage);

            ViewGroup.LayoutParams params =
                    fillView.getLayoutParams();

            params.height = targetHeight;

            fillView.setLayoutParams(params);
        });
    }
    private void showCanCapacityDialog() {

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottomsheet_can_capacity, null);

        dialog.setContentView(view);

        TextInputEditText etCow =
                view.findViewById(R.id.etCowCapacity);

        TextInputEditText etBuffalo =
                view.findViewById(R.id.etBuffaloCapacity);

        TextInputEditText etMix =
                view.findViewById(R.id.etMixCapacity);

        MaterialButton btnSave =
                view.findViewById(R.id.btnSaveCapacity);

        etCow.setText(String.valueOf((int) cowCanCapacity));
        etBuffalo.setText(String.valueOf((int) buffaloCanCapacity));
        etMix.setText(String.valueOf((int) mixCanCapacity));

        btnSave.setOnClickListener(v -> {

            double cow =
                    parseDouble(etCow.getText().toString());

            double buffalo =
                    parseDouble(etBuffalo.getText().toString());

            double mix =
                    parseDouble(etMix.getText().toString());

            if (cow <= 0 || buffalo <= 0 || mix <= 0) {
                Toast.makeText(
                        requireContext(),
                        "Capacity must be greater than 0",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Map<String,Object> map =
                    new HashMap<>();

            map.put("cowCapacity", cow);
            map.put("buffaloCapacity", buffalo);
            map.put("mixCapacity", mix);

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("MilkCansettings")
                    .document("milk_can_capacity")
                    .set(map)
                    .addOnSuccessListener(unused -> {

                        cowCanCapacity = cow;
                        buffaloCanCapacity = buffalo;
                        mixCanCapacity = mix;

                        updateCapacityText();
                        updatePlatformSummary();

                        dialog.dismiss();

                        Toast.makeText(
                                requireContext(),
                                "Capacity Updated",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    requireContext(),
                                    e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        dialog.show();
    }

    private void showDatePicker(TextView target, Runnable afterSelect) {
        Calendar cal = Calendar.getInstance();
        long existing = parseDateMillis(target.getText().toString());

        if (existing > 0) {
            cal.setTimeInMillis(existing);
        }

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            target.setText(dateFormat.format(cal.getTime()));

            if (afterSelect != null) {
                afterSelect.run();
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadRateFromChart(String milkType, double fat, double snf, RateCallback callback) {
        String chartDocId = milkTypeToChartDocId(milkType);
        String exactKey = chartKey(round1(fat), round1(snf));

        FirebaseRefs.rateCharts()
                .document(chartDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onRateLoaded(0);
                        return;
                    }

                    Map<String, Object> chart =
                            (Map<String, Object>) documentSnapshot.get("generatedChart");

                    if (chart == null || chart.isEmpty()) {
                        callback.onRateLoaded(0);
                        return;
                    }

                    Double rate = getRateFromChartMap(chart, exactKey);

                    if (rate == null) {
                        rate = findNearestRate(chart, fat, snf);
                    }

                    callback.onRateLoaded(rate == null ? 0 : rate);
                })
                .addOnFailureListener(e -> {
                    callback.onRateLoaded(0);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private Double getRateFromChartMap(Map<String, Object> chart, String key) {
        Object value = chart.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private Double findNearestRate(Map<String, Object> chart, double fat, double snf) {
        double roundedFat = round1(fat);
        double roundedSnf = round1(snf);

        Double nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (String key : chart.keySet()) {
            String[] parts = key.split("_");

            if (parts.length != 2) continue;

            try {
                double keyFat = Double.parseDouble(parts[0]);
                double keySnf = Double.parseDouble(parts[1]);

                double distance = Math.abs(keyFat - roundedFat) + Math.abs(keySnf - roundedSnf);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = getRateFromChartMap(chart, key);
                }
            } catch (Exception ignored) {}
        }

        return nearest;
    }

    private String milkTypeToChartDocId(String milkType) {
        if ("Buffalo".equalsIgnoreCase(milkType)) return "buffalo";
        if ("Mix".equalsIgnoreCase(milkType)) return "mix";
        return "cow";
    }

    private String chartKey(double fat, double snf) {
        return String.format(Locale.US, "%.1f_%.1f", fat, snf);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private interface RateCallback {
        void onRateLoaded(double rate);
    }

    private Farmer findFarmerByDisplayText(String text) {
        for (Farmer farmer : farmers) {
            if (displayFarmer(farmer).equalsIgnoreCase(text)) {
                return farmer;
            }
        }

        return null;
    }

    private Farmer findFarmerFromInput() {
        String input = etFarmer.getText().toString().trim();

        for (Farmer farmer : farmers) {
            if (input.equalsIgnoreCase(displayFarmer(farmer))
                    || input.equalsIgnoreCase(safe(farmer.getCode()))
                    || input.equalsIgnoreCase(safe(farmer.getName()))) {
                return farmer;
            }
        }

        return selectedFarmer;
    }

    private String displayFarmer(Farmer farmer) {
        return safe(farmer.getCode()) + " - " + safe(farmer.getName());
    }

    private double getDouble(QueryDocumentSnapshot doc, String key) {
        Double value = doc.getDouble(key);
        return value == null ? 0 : value;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseDateMillis(String text) {
        try {
            return dateFormat.parse(text).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private boolean isSameDay(long a, long b) {
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();

        ca.setTimeInMillis(a);
        cb.setTimeInMillis(b);

        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(long a, long b) {
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();

        ca.setTimeInMillis(a);
        cb.setTimeInMillis(b);

        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.WEEK_OF_YEAR) == cb.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(long a, long b) {
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();

        ca.setTimeInMillis(a);
        cb.setTimeInMillis(b);

        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH);
    }

    private int color(String hex) {
        return android.graphics.Color.parseColor(hex);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (farmerListener != null) {
            farmerListener.remove();
            farmerListener = null;
        }

        if (milkListener != null) {
            milkListener.remove();
            milkListener = null;
        }

        if (addEntryDialog != null && addEntryDialog.isShowing()) {
            addEntryDialog.dismiss();
        }
    }
}