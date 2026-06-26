package com.prashant.milkdairy.ui.Generate_Bills;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.prashant.milkdairy.Adapter.BillsAdapter;
import com.prashant.milkdairy.Model.BillModel;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillsFragment extends Fragment {

    private RecyclerView rvBills;
    private LinearLayout layoutEmptyBills;
    private TextView tvEmptyBillsSub;
    private android.widget.ProgressBar progressBills;
    private Spinner spStatus, spFarmer;
    private EditText etSearch, etFromDate, etToDate;

    private View btnRefresh;
    private MaterialButton btnAllFarmers, btnSingleFarmer, btnCalculate;
    private MaterialButton btnClearPreview, btnGenerateBill;

    private LinearLayout layoutLoader, layoutFarmer, layoutBillLoader;
    private LinearLayout layoutBillPreviewCards, layoutMilkRecordRows;

    private View seeMilkRecordCard, milkRecordsDialog;

    private TextView tvResult, tvBillPreviewTitle, tvTotalMilk, tvNetPayable;
    private TextView tvPreviewBonusAmount, tvPreviewDeductionAmount;
    private TextView tvDialogTitle, tvMilkMorningTotal, tvMilkEveningTotal, tvMilkGrandTotal;

    private TextView tvStatTotal, tvStatPending, tvStatPaid, tvStatAmount;

    private BillsAdapter adapter;

    private final List<BillModel> billList = new ArrayList<>();
    private final List<BillModel> filteredBills = new ArrayList<>();
    private final List<Farmer> farmers = new ArrayList<>();
    private final List<BillPreview> pendingPreviewBills = new ArrayList<>();

    private ListenerRegistration billsListener;
    private ListenerRegistration farmersListener;

    private boolean isSingleFarmer = false;
    private BillPreview selectedPreview;
    private Farmer selectedFarmer;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    public BillsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bills, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecycler();
        setupStatusFilter();
        setupSearch();
        setupRefresh();
        setupDatePicker();
        setupFarmerSpinner();
        setupGenerateLogic();
        listenBills();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void initViews(View view) {
        rvBills = view.findViewById(R.id.rvBills);
        layoutEmptyBills = view.findViewById(R.id.layoutEmptyBills);

        tvEmptyBillsSub = view.findViewById(R.id.tvEmptyBillsSub);

        progressBills = view.findViewById(R.id.progressBills);
        spStatus = view.findViewById(R.id.spStatus);
        etSearch = view.findViewById(R.id.etSearch);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        layoutLoader = view.findViewById(R.id.layoutLoader);

        btnAllFarmers = view.findViewById(R.id.btnAllFarmers);
        btnSingleFarmer = view.findViewById(R.id.btnSingleFarmer);
        btnCalculate = view.findViewById(R.id.btnCalculate);

        etFromDate = view.findViewById(R.id.etFromDate);
        etToDate = view.findViewById(R.id.etToDate);

        spFarmer = view.findViewById(R.id.spFarmer);
        layoutFarmer = view.findViewById(R.id.layoutFarmer);

        layoutBillLoader = view.findViewById(R.id.layoutBillLoader);
        tvResult = view.findViewById(R.id.tvResult);
        layoutBillPreviewCards = view.findViewById(R.id.layoutBillPreviewCards);

        btnClearPreview = view.findViewById(R.id.btnClearPreview);
        btnGenerateBill = view.findViewById(R.id.btnGenerateBill);

        tvBillPreviewTitle = view.findViewById(R.id.tvBillPreviewTitle);
        tvTotalMilk = view.findViewById(R.id.tvTotalMilk);
        tvNetPayable = view.findViewById(R.id.tvNetPayable);
        tvPreviewBonusAmount = view.findViewById(R.id.tvPreviewBonusAmount);
        tvPreviewDeductionAmount = view.findViewById(R.id.tvPreviewDeductionAmount);

        seeMilkRecordCard = view.findViewById(R.id.seeMilkRecordCard);
        milkRecordsDialog = view.findViewById(R.id.milkRecordsDialog);
        layoutMilkRecordRows = view.findViewById(R.id.layoutMilkRecordRows);

        tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        tvMilkMorningTotal = view.findViewById(R.id.tvMilkMorningTotal);
        tvMilkEveningTotal = view.findViewById(R.id.tvMilkEveningTotal);
        tvMilkGrandTotal = view.findViewById(R.id.tvMilkGrandTotal);

        tvStatTotal = view.findViewById(R.id.statTotal).findViewById(R.id.tvDashnum);
        tvStatPending = view.findViewById(R.id.statCow).findViewById(R.id.tvDashnum);
        tvStatPaid = view.findViewById(R.id.statBuffalo).findViewById(R.id.tvDashnum);
        tvStatAmount = view.findViewById(R.id.statActive).findViewById(R.id.tvDashnum);

        ((TextView) view.findViewById(R.id.statTotal).findViewById(R.id.tvdashtext)).setText("TOTAL");
        ((TextView) view.findViewById(R.id.statCow).findViewById(R.id.tvdashtext)).setText("PENDING");
        ((TextView) view.findViewById(R.id.statBuffalo).findViewById(R.id.tvdashtext)).setText("PAID");
        ((TextView) view.findViewById(R.id.statActive).findViewById(R.id.tvdashtext)).setText("AMOUNT");

        View btnCloseDialog = view.findViewById(R.id.btnCloseDialog);
        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> milkRecordsDialog.setVisibility(View.GONE));
        }

        String today = sdf.format(Calendar.getInstance().getTime());
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);

        etFromDate.setText(sdf.format(monthStart.getTime()));
        etToDate.setText(today);

        clearPreview();
    }

    private void setupRecycler() {
        rvBills.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBills.setNestedScrollingEnabled(true);

        adapter = new BillsAdapter(filteredBills, new BillsAdapter.BillActionListener() {
            @Override
            public void onPaidClick(BillModel bill) {
                markBillPaid(bill);
            }

            @Override
            public void onShareClick(BillModel bill) {
                sendBillToWhatsApp(bill);
            }

            @Override
            public void onPdfClick(BillModel bill) {
                generatePdfBill(bill);
            }

            @Override
            public void onCancelClick(BillModel bill) {
                confirmCancelBill(bill);
            }

            @Override
            public void onRestoreClick(BillModel bill) {
                restoreBill(bill);
            }
        });

        rvBills.setAdapter(adapter);
    }

    private void setupStatusFilter() {
        String[] statusOptions = {"All", "Pending", "Paid", "Cancelled"};
        spStatus.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions));

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterBills();
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                filterBills();
            }

            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });
    }

    private void setupRefresh() {
        btnRefresh.setOnClickListener(v -> {
            layoutLoader.setVisibility(View.VISIBLE);
            rvBills.setVisibility(View.GONE);

            filterBills();

            layoutLoader.postDelayed(() -> {
                layoutLoader.setVisibility(View.GONE);
                rvBills.setVisibility(View.VISIBLE);
            }, 500);
        });
    }

    private void setupDatePicker() {
        etFromDate.setOnClickListener(v -> openDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> openDatePicker(etToDate));
    }

    private void openDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(sdf.parse(editText.getText().toString().trim()));
        } catch (Exception ignored) {}

        new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    editText.setText(sdf.format(calendar.getTime()));
                    clearPreview();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupFarmerSpinner() {
        farmersListener = FirebaseRefs.farmers().addSnapshotListener((snapshots, error) -> {
            if (!isAdded()) return;

            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            farmers.clear();

            List<String> names = new ArrayList<>();
            names.add("Select farmer");

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Farmer farmer = new Farmer();
                    farmer.setId(doc.getId());
                    farmer.setCode(doc.getString("code"));
                    farmer.setName(doc.getString("name"));
                    farmer.setMobile(doc.getString("mobile"));
                    farmer.setStatus(doc.getString("status"));
                    farmer.setMilkType(doc.getString("milkType"));

                    farmers.add(farmer);
                    names.add(safe(farmer.getCode()) + " - " + safe(farmer.getName()));
                }
            }

            spFarmer.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
        });
    }

    private void setupGenerateLogic() {
        isSingleFarmer = false;
        layoutFarmer.setVisibility(View.GONE);
        updateBillScopeButtons();

        btnAllFarmers.setOnClickListener(v -> {
            isSingleFarmer = false;
            layoutFarmer.setVisibility(View.GONE);
            updateBillScopeButtons();
            clearPreview();
        });

        btnSingleFarmer.setOnClickListener(v -> {
            isSingleFarmer = true;
            layoutFarmer.setVisibility(View.VISIBLE);
            updateBillScopeButtons();
            clearPreview();
        });

        btnCalculate.setOnClickListener(v -> calculateBills());
        btnClearPreview.setOnClickListener(v -> clearPreview());
        btnGenerateBill.setOnClickListener(v -> saveFinalBills());

        if (seeMilkRecordCard != null) {
            seeMilkRecordCard.setOnClickListener(v -> {
                if (!isSingleFarmer) {
                    Toast.makeText(requireContext(), "Milk records are available for single farmer bill only", Toast.LENGTH_SHORT).show();
                    return;
                }

                showMilkRecordsDialog();
            });
        }

        View btnCloseDialog = requireView().findViewById(R.id.btnCloseDialog);
        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> {
                if (milkRecordsDialog != null) {
                    milkRecordsDialog.setVisibility(View.GONE);
                }
            });
        }
    }

    private void updateBillScopeButtons() {
        setBillScopeButton(btnAllFarmers, !isSingleFarmer);
        setBillScopeButton(btnSingleFarmer, isSingleFarmer);
    }

    private void setBillScopeButton(MaterialButton button, boolean selected) {
        int bgColor = selected ? color("#ECFDF5") : color("#F4F7FA");
        int textColor = selected ? color("#10B981") : color("#667085");

        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private int color(String hex) {
        return Color.parseColor(hex);
    }

    private void listenBills() {
        billsListener = FirebaseRefs.bills().orderBy("generatedAt", Query.Direction.DESCENDING).addSnapshotListener((snapshots, error) -> {
            if (!isAdded()) return;

            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            billList.clear();

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    BillModel bill = new BillModel();
                    bill.setId(doc.getId());
                    bill.setBillNo(doc.getString("billNo"));
                    bill.setFarmerId(doc.getString("farmerId"));
                    bill.setFarmerName(doc.getString("farmerName"));
                    bill.setFarmerCode(doc.getString("farmerCode"));
                    bill.setMobile(doc.getString("mobile"));
                    bill.setPeriodFrom(doc.getString("periodFrom"));
                    bill.setPeriodTo(doc.getString("periodTo"));
                    bill.setTotalLiters(getDouble(doc, "totalLiters"));
                    bill.setMilkAmount(getDouble(doc, "milkAmount"));
                    bill.setBonusAmount(getDouble(doc, "bonusAmount"));
                    bill.setDeductionAmount(getDouble(doc, "deductionAmount"));
                    bill.setNetPayable(getDouble(doc, "netPayable"));
                    bill.setStatus(doc.getString("status") == null ? "Pending" : doc.getString("status"));
                    bill.setMilkSnapshot(getSnapshotList(doc.get("milkSnapshot")));
                    bill.setBonusSnapshot(getSnapshotList(doc.get("bonusSnapshot")));
                    bill.setDeductionSnapshot(getSnapshotList(doc.get("deductionSnapshot")));

                    billList.add(bill);
                }
            }

            updateSummaryCards();
            filterBills();
        });
    }

    private List<Map<String, Object>> getSnapshotList(Object raw) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (!(raw instanceof List)) {
            return result;
        }

        for (Object item : (List<?>) raw) {
            if (item instanceof Map) {
                Map<String, Object> copy = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                    if (entry.getKey() != null) {
                        copy.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                result.add(copy);
            }
        }

        return result;
    }

    private void updateSummaryCards() {
        int total = billList.size();
        int pending = 0;
        int paid = 0;
        double totalAmount = 0;

        for (BillModel bill : billList) {
            if ("Pending".equalsIgnoreCase(bill.getStatus())) pending++;
            if ("Paid".equalsIgnoreCase(bill.getStatus())) paid++;
            if (!"Cancelled".equalsIgnoreCase(bill.getStatus())) {
                totalAmount += bill.getNetPayableValue();
            }
        }

        tvStatTotal.setText(String.valueOf(total));
        tvStatPending.setText(String.valueOf(pending));
        tvStatPaid.setText(String.valueOf(paid));
        tvStatAmount.setText("₹" + formatCompact(totalAmount));
    }

    private void calculateBills() {
        if (!validateDates()) return;

        layoutBillLoader.setVisibility(View.VISIBLE);
        layoutBillPreviewCards.setVisibility(View.GONE);
        tvResult.setVisibility(View.GONE);
        milkRecordsDialog.setVisibility(View.GONE);

        List<Farmer> targetFarmers = getTargetFarmers();

        if (targetFarmers.isEmpty()) {
            layoutBillLoader.setVisibility(View.GONE);
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("Please select valid farmer.");
            return;
        }

        String from = etFromDate.getText().toString().trim();
        String to = etToDate.getText().toString().trim();

        FirebaseRefs.milkCollection().get().addOnSuccessListener(milkSnapshots -> {
            FirebaseRefs.bonusDistributions().get().addOnSuccessListener(bonusSnapshots -> {
                FirebaseRefs.bonusRules().get().addOnSuccessListener(ruleSnapshots -> {
                    FirebaseRefs.advanceFood().get().addOnSuccessListener(deductionSnapshots -> {
                        pendingPreviewBills.clear();

                        for (Farmer farmer : targetFarmers) {
                            if (!"Active".equalsIgnoreCase(safe(farmer.getStatus()))) continue;
                            if (isAlreadyBilled(farmer.getId(), from, to)) continue;

                            BillPreview preview = calculateFarmerPreview(
                                    farmer,
                                    milkSnapshots.getDocuments(),
                                    bonusSnapshots.getDocuments(),
                                    ruleSnapshots.getDocuments(),
                                    deductionSnapshots.getDocuments(),
                                    from,
                                    to
                            );

                            if (preview != null && (preview.totalLiters > 0
                                    || preview.milkAmount > 0
                                    || preview.deductionAmount > 0
                                    || preview.bonusAmount > 0)) {
                                pendingPreviewBills.add(preview);
                            }
                        }

                        layoutBillLoader.setVisibility(View.GONE);

                        if (pendingPreviewBills.isEmpty()) {
                            tvResult.setVisibility(View.VISIBLE);
                            tvResult.setText("No bill data found for selected period.");
                            return;
                        }

                        if (isSingleFarmer) {
                            selectedPreview = pendingPreviewBills.get(0);
                            showSinglePreview(selectedPreview);
                        } else {
                            selectedPreview = null;
                            showAllPreviewSummary();
                        }

                    }).addOnFailureListener(e -> showCalculateError(e.getMessage()));
                }).addOnFailureListener(e -> showCalculateError(e.getMessage()));
            }).addOnFailureListener(e -> showCalculateError(e.getMessage()));
        }).addOnFailureListener(e -> showCalculateError(e.getMessage()));
    }

    private List<Farmer> getTargetFarmers() {
        List<Farmer> target = new ArrayList<>();

        if (isSingleFarmer) {
            int pos = spFarmer.getSelectedItemPosition();

            if (pos <= 0 || pos - 1 >= farmers.size()) {
                return target;
            }

            selectedFarmer = farmers.get(pos - 1);
            target.add(selectedFarmer);
        } else {
            target.addAll(farmers);
        }

        return target;
    }

    private BillPreview calculateFarmerPreview(Farmer farmer,
                                               List<DocumentSnapshot> milkDocs,
                                               List<DocumentSnapshot> bonusDocs,
                                               List<DocumentSnapshot> bonusRuleDocs,
                                               List<DocumentSnapshot> deductionDocs,
                                               String from,
                                               String to) {
        BillPreview preview = new BillPreview();
        preview.farmer = farmer;
        preview.periodFrom = from;
        preview.periodTo = to;

        for (DocumentSnapshot doc : milkDocs) {
            String farmerId = doc.getString("farmerId");
            String date = doc.getString("date");

            if (!farmer.getId().equals(farmerId)) continue;
            if (!isDateInRange(date, from, to)) continue;

            double liters = getDouble(doc, "liters");
            double fat = getDouble(doc, "fat");
            double rate = getDouble(doc, "rate");
            double amount = getMilkAmount(doc);

            if (liters <= 0 || rate <= 0 || amount <= 0) continue;

            String shift = safe(doc.getString("shift"));

            preview.totalLiters += liters;
            preview.milkAmount += amount;
            preview.totalFatWeighted += fat * liters;

            if ("Morning".equalsIgnoreCase(shift)) {
                preview.morningLiters += liters;
                preview.morningAmount += amount;
                preview.morningFatWeighted += fat * liters;
                preview.morningRateWeighted += rate * liters;
            } else {
                preview.eveningLiters += liters;
                preview.eveningAmount += amount;
                preview.eveningFatWeighted += fat * liters;
                preview.eveningRateWeighted += rate * liters;
            }

            Map<String, Object> milkSnapshot = new HashMap<>();
            milkSnapshot.put("date", date);
            milkSnapshot.put("shift", shift);
            milkSnapshot.put("liters", liters);
            milkSnapshot.put("fat", fat);
            milkSnapshot.put("snf", getDouble(doc, "snf"));
            milkSnapshot.put("rate", rate);
            milkSnapshot.put("amount", amount);
            preview.milkSnapshots.add(milkSnapshot);
        }

        applyActiveBonusToPreview(preview, bonusDocs, bonusRuleDocs, from, to);
        applyDeductionsToPreview(preview, deductionDocs, from, to);

        preview.netPayable = preview.milkAmount + preview.bonusAmount - preview.deductionAmount;

        if (preview.netPayable < 0) {
            preview.netPayable = 0;
        }

        return preview;
    }

    private void applyActiveBonusToPreview(BillPreview preview,
                                           List<DocumentSnapshot> bonusDocs,
                                           List<DocumentSnapshot> bonusRuleDocs,
                                           String from,
                                           String to) {
        List<String> activeRuleIds = new ArrayList<>();

        for (DocumentSnapshot ruleDoc : bonusRuleDocs) {
            Boolean active = ruleDoc.getBoolean("active");

            if (active == null || !active) continue;

            String ruleId = safe(ruleDoc.getString("ruleId"));

            if (!ruleId.isEmpty()) {
                activeRuleIds.add(ruleId);
            }

            activeRuleIds.add(ruleDoc.getId());
        }

        for (DocumentSnapshot doc : bonusDocs) {
            String farmerId = doc.getString("farmerId");
            String status = safe(doc.getString("status"));

            if (!preview.farmer.getId().equals(farmerId)) continue;
            if ("CANCELLED".equalsIgnoreCase(status)) continue;

            String bonusRuleId = safe(doc.getString("bonusRuleId"));

            if (bonusRuleId.isEmpty() || !activeRuleIds.contains(bonusRuleId)) {
                continue;
            }

            String periodFrom = doc.getString("periodFrom");
            String periodTo = doc.getString("periodTo");

            if (!isBonusInsideBillPeriod(periodFrom, periodTo, from, to)) continue;

            double bonus = getDouble(doc, "bonusAmount");
            if (bonus <= 0) continue;

            preview.bonusAmount += bonus;

            Map<String, Object> bonusSnapshot = new HashMap<>();
            bonusSnapshot.put("distributionId", doc.getString("distributionId"));
            bonusSnapshot.put("bonusRuleId", bonusRuleId);
            bonusSnapshot.put("bonusRuleName", doc.getString("bonusRuleName"));
            bonusSnapshot.put("bonusAmount", bonus);
            bonusSnapshot.put("periodFrom", periodFrom);
            bonusSnapshot.put("periodTo", periodTo);
            preview.bonusSnapshots.add(bonusSnapshot);
        }
    }

    private void applyDeductionsToPreview(BillPreview preview,
                                          List<DocumentSnapshot> deductionDocs,
                                          String from,
                                          String to) {
        preview.deductionAmount = 0;
        preview.deductionSnapshots.clear();

        for (DocumentSnapshot doc : deductionDocs) {
            String farmerId = doc.getString("farmerId");
            String status = safe(doc.getString("status"));

            if (!preview.farerIdMatches(farmerId)) continue;

            if ("DELETED".equalsIgnoreCase(status)
                    || "CANCELLED".equalsIgnoreCase(status)
                    || "INACTIVE".equalsIgnoreCase(status)) {
                continue;
            }

            String date = safe(doc.getString("date"));

            if (date.isEmpty()) continue;
            if (!isDateInRange(date, from, to)) continue;

            double amount = getDouble(doc, "amount");
            if (amount <= 0) continue;

            String title = firstNonEmpty(
                    doc.getString("category"),
                    doc.getString("title"),
                    doc.getString("type"),
                    "Deduction"
            );

            preview.deductionAmount += amount;

            Map<String, Object> deductionSnapshot = new HashMap<>();
            deductionSnapshot.put("deductionId", doc.getId());
            deductionSnapshot.put("date", date);
            deductionSnapshot.put("title", title);
            deductionSnapshot.put("amount", amount);
            deductionSnapshot.put("status", status);
            preview.deductionSnapshots.add(deductionSnapshot);
        }
    }

    private void showSinglePreview(BillPreview preview) {
        selectedPreview = preview;

        tvResult.setVisibility(View.GONE);
        layoutBillPreviewCards.setVisibility(View.VISIBLE);
        milkRecordsDialog.setVisibility(View.GONE);

        tvBillPreviewTitle.setText("Bill Preview - " + safe(preview.farmer.getName()));
        tvTotalMilk.setText("Total Milk: " + format(preview.totalLiters) + " L");
        tvNetPayable.setText("Net Payable: ₹" + format(preview.netPayable));
        tvPreviewBonusAmount.setText("₹" + format(preview.bonusAmount));
        tvPreviewDeductionAmount.setText("₹" + format(preview.deductionAmount));

        seeMilkRecordCard.setVisibility(View.VISIBLE);
    }

    private void showAllPreviewSummary() {
        double totalMilk = 0;
        double totalBonus = 0;
        double totalDeduction = 0;
        double totalNet = 0;

        for (BillPreview preview : pendingPreviewBills) {
            totalMilk += preview.totalLiters;
            totalBonus += preview.bonusAmount;
            totalDeduction += preview.deductionAmount;
            totalNet += preview.netPayable;
        }

        selectedPreview = null;

        tvResult.setVisibility(View.GONE);
        layoutBillPreviewCards.setVisibility(View.VISIBLE);

        tvBillPreviewTitle.setText("Ready to Generate " + pendingPreviewBills.size() + " Bills");
        tvTotalMilk.setText("Total Milk: " + format(totalMilk) + " L");
        tvNetPayable.setText("Net Payable: ₹" + format(totalNet));
        tvPreviewBonusAmount.setText("₹" + format(totalBonus));
        tvPreviewDeductionAmount.setText("₹" + format(totalDeduction));

        if (seeMilkRecordCard != null) {
            seeMilkRecordCard.setVisibility(View.GONE);
        }
    }

    private void showMilkRecordsDialog() {
        if (selectedPreview == null) {
            Toast.makeText(requireContext(), "Calculate single farmer bill first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPreview.milkSnapshots == null || selectedPreview.milkSnapshots.isEmpty()) {
            Toast.makeText(requireContext(), "No milk records found for selected period", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, MilkDayRow> dayMap = new LinkedHashMap<>();

        for (Map<String, Object> snapshot : selectedPreview.milkSnapshots) {
            String date = safe(String.valueOf(snapshot.get("date")));
            String shift = safe(String.valueOf(snapshot.get("shift")));
            double liters = getNumber(snapshot.get("liters"));

            if (date.isEmpty() || "null".equalsIgnoreCase(date) || liters <= 0) {
                continue;
            }

            MilkDayRow row = dayMap.get(date);

            if (row == null) {
                row = new MilkDayRow();
                row.date = date;
                dayMap.put(date, row);
            }

            if ("Morning".equalsIgnoreCase(shift)) {
                row.morning += liters;
            } else if ("Evening".equalsIgnoreCase(shift)) {
                row.evening += liters;
            }
        }

        List<MilkDayRow> rows = new ArrayList<>(dayMap.values());

        Collections.sort(rows, (a, b) -> {
            try {
                return Long.compare(parseDate(a.date), parseDate(b.date));
            } catch (Exception e) {
                return a.date.compareTo(b.date);
            }
        });

        if (rows.isEmpty()) {
            Toast.makeText(requireContext(), "No milk records found for selected period", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView title = new TextView(requireContext());
        title.setText("Milk Records - " + safe(selectedPreview.farmer.getName()));
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#1F2937"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        LinearLayout header = createMilkRecordLine("Date", "Morning", "Evening", "Total", true);
        header.setBackgroundColor(Color.parseColor("#F3F4F6"));
        root.addView(header);

        double totalMorning = 0;
        double totalEvening = 0;

        for (MilkDayRow row : rows) {
            totalMorning += row.morning;
            totalEvening += row.evening;

            root.addView(createMilkRecordLine(
                    shortDate(row.date),
                    row.morning > 0 ? format(row.morning) : "-",
                    row.evening > 0 ? format(row.evening) : "-",
                    format(row.morning + row.evening),
                    false
            ));

            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
            ));
            divider.setBackgroundColor(Color.parseColor("#E5E7EB"));
            root.addView(divider);
        }

        LinearLayout totalRow = createMilkRecordLine(
                "Total",
                format(totalMorning) + "L",
                format(totalEvening) + "L",
                format(totalMorning + totalEvening) + "L",
                true
        );
        totalRow.setPadding(dp(5), dp(8), dp(5), dp(5));
        root.addView(totalRow);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(root)
                .setPositiveButton("Close", null)
                .show();
    }
    private LinearLayout createMilkRecordLine(String date,
                                              String morning,
                                              String evening,
                                              String total,
                                              boolean bold) {
        LinearLayout line = new LinearLayout(requireContext());
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(dp(5), dp(6), dp(5), dp(6));

        line.addView(createMilkRecordCell(date, 1.2f, false, bold));
        line.addView(createMilkRecordCell(morning, 1f, true, bold));
        line.addView(createMilkRecordCell(evening, 1f, true, bold));
        line.addView(createMilkRecordCell(total, 1f, true, bold));

        return line;
    }

    private TextView createMilkRecordCell(String text, float weight, boolean center, boolean bold) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTextColor(Color.parseColor(bold ? "#1F2937" : "#4B5563"));
        tv.setGravity(center ? Gravity.CENTER : Gravity.START);

        if (bold) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        ));

        return tv;
    }

    private void addMilkRecordRow(MilkDayRow row) {
        LinearLayout line = new LinearLayout(requireContext());
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(dp(5), dp(5), dp(5), dp(5));

        line.addView(createMilkCell(shortDate(row.date), 1.2f, false));
        line.addView(createMilkCell(row.morning > 0 ? format(row.morning) : "-", 1f, true));
        line.addView(createMilkCell(row.evening > 0 ? format(row.evening) : "-", 1f, true));
        line.addView(createMilkCell(format(row.morning + row.evening), 1f, true));

        layoutMilkRecordRows.addView(line);

        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        ));
        divider.setBackgroundColor(Color.parseColor("#E5E7EB"));
        layoutMilkRecordRows.addView(divider);
    }

    private TextView createMilkCell(String text, float weight, boolean center) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTextColor(Color.parseColor("#4B5563"));
        tv.setGravity(center ? Gravity.CENTER : Gravity.START);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        ));
        return tv;
    }

    private void saveFinalBills() {
        if (pendingPreviewBills.isEmpty()) {
            Toast.makeText(requireContext(), "Calculate bill first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateBill.setEnabled(false);
        btnGenerateBill.setText("Saving...");

        WriteBatch batch = FirebaseFirestoreInstance.batch();
        int index = 1;

        for (BillPreview preview : pendingPreviewBills) {
            String billId = FirebaseRefs.bills().document().getId();
            String billNo = "BILL-" + System.currentTimeMillis() + "-" + index;

            Map<String, Object> map = new HashMap<>();
            map.put("id", billId);
            map.put("billId", billId);
            map.put("billNo", billNo);
            map.put("farmerId", preview.farmer.getId());
            map.put("farmerCode", preview.farmer.getCode());
            map.put("farmerName", preview.farmer.getName());
            map.put("mobile", preview.farmer.getMobile());
            map.put("periodFrom", preview.periodFrom);
            map.put("periodTo", preview.periodTo);
            map.put("totalLiters", preview.totalLiters);
            map.put("milkAmount", preview.milkAmount);
            map.put("bonusAmount", preview.bonusAmount);
            map.put("deductionAmount", preview.deductionAmount);
            map.put("netPayable", preview.netPayable);
            map.put("status", "Pending");
            map.put("milkSnapshot", preview.milkSnapshots);
            map.put("bonusSnapshot", preview.bonusSnapshots);
            map.put("deductionSnapshot", preview.deductionSnapshots);
            map.put("generatedAt", FieldValue.serverTimestamp());
            map.put("paidAt", null);

            batch.set(FirebaseRefs.bills().document(billId), map);
            index++;
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    btnGenerateBill.setEnabled(true);
                    btnGenerateBill.setText("Generate Bill");
                    Toast.makeText(requireContext(), "Bills generated successfully", Toast.LENGTH_SHORT).show();
                    clearPreview();
                })
                .addOnFailureListener(e -> {
                    btnGenerateBill.setEnabled(true);
                    btnGenerateBill.setText("Generate Bill");
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markBillPaid(BillModel bill) {
        FirebaseRefs.bills().document(bill.getId())
                .update(
                        "status", "Paid",
                        "paidAt", FieldValue.serverTimestamp(),
                        "paymentMode", "Cash"
                )
                .addOnSuccessListener(unused -> Toast.makeText(requireContext(), "Bill marked as Paid", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void confirmCancelBill(BillModel bill) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Bill")
                .setMessage("Are you sure you want to cancel this bill?")
                .setPositiveButton("Cancel Bill",
                        (d, w) -> cancelBill(bill))
                .setNegativeButton("Close", null)
                .show();
    }



    private void sendBillToWhatsApp(BillModel bill) {
        String mobile = normalizeMobile(bill.getMobile());

        if (mobile.isEmpty()) {
            FirebaseRefs.farmers().document(bill.getFarmerId()).get()
                    .addOnSuccessListener(doc -> {
                        String fetchedMobile = normalizeMobile(doc.getString("mobile"));

                        if (fetchedMobile.isEmpty()) {
                            Toast.makeText(requireContext(), "Farmer mobile not available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        openWhatsAppBill(bill, fetchedMobile);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            return;
        }

        openWhatsAppBill(bill, mobile);
    }

    private void openWhatsAppBill(BillModel bill, String mobile) {
        String message =
                "Milk Bill\n\n" +
                        "Farmer: " + bill.getFarmerName() + "\n" +
                        "Code: " + bill.getFarmerCode() + "\n" +
                        "Bill No: " + bill.getBillNo() + "\n" +
                        "Period: " + bill.getPeriod() + "\n\n" +
                        "Milk Amount: ₹" + bill.getMilkAmount() + "\n" +
                        "Bonus: ₹" + bill.getBonus() + "\n" +
                        "Deduction: ₹" + bill.getDeduction() + "\n" +
                        "Net Payable: ₹" + bill.getNetPayable();

        try {
            String url = "https://wa.me/91" + mobile + "?text=" +
                    URLEncoder.encode(message, StandardCharsets.UTF_8.toString());

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "WhatsApp not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePdfBill(BillModel bill) {
        BillPreview preview = previewFromBill(bill);

        if (preview.milkSnapshots.isEmpty() && preview.deductionSnapshots.isEmpty()) {
            Toast.makeText(requireContext(), "No bill data found.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseRefs.currentUserDoc().get()
                .addOnSuccessListener(userDoc -> {
                    String dairyName = getPdfDairyName(userDoc);
                    createRealBillPdf(dairyName, preview, bill.getBillNo());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private BillPreview previewFromBill(BillModel bill) {
        BillPreview preview = new BillPreview();
        Farmer farmer = new Farmer();
        farmer.setId(bill.getFarmerId());
        farmer.setCode(bill.getFarmerCode());
        farmer.setName(bill.getFarmerName());
        farmer.setMobile(bill.getMobile());

        preview.farmer = farmer;
        preview.periodFrom = bill.getPeriodFrom();
        preview.periodTo = bill.getPeriodTo();
        preview.totalLiters = bill.getTotalLitersValue();
        preview.milkAmount = bill.getMilkAmountValue();
        preview.bonusAmount = bill.getBonusAmountValue();
        preview.deductionAmount = bill.getDeductionAmountValue();
        preview.netPayable = bill.getNetPayableValue();
        preview.milkSnapshots.addAll(bill.getMilkSnapshot());
        preview.bonusSnapshots.addAll(bill.getBonusSnapshot());
        preview.deductionSnapshots.addAll(bill.getDeductionSnapshot());

        for (Map<String, Object> row : preview.milkSnapshots) {
            double liters = getNumber(row.get("liters"));
            double fat = getNumber(row.get("fat"));
            preview.totalFatWeighted += fat * liters;
        }

        return preview;
    }

    private void createRealBillPdf(String dairyName, BillPreview preview, String billNo) {
        String farmer = safe(preview.farmer.getName());
        String code = safe(preview.farmer.getCode());

        List<DayMilkRow> dayRows = buildPdfDayRows(preview.milkSnapshots, preview.deductionSnapshots, preview.periodFrom, preview.periodTo);

        if (dayRows.isEmpty()) {
            Toast.makeText(requireContext(), "No bill data found.", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalLiters = preview.totalLiters;
        double milkAmount = preview.milkAmount;
        double bonusAmount = preview.bonusAmount;
        double deductionAmount = getPdfTotalDeduction(dayRows);
        double netPayable = Math.max(0, milkAmount + bonusAmount - deductionAmount);

        PdfDocument pdfDocument = new PdfDocument();

        Paint textPaint = new Paint();
        textPaint.setTextSize(7);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(7);
        boldPaint.setFakeBoldText(true);

        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);

        int pageWidth = 595;
        int pageHeight = 842;

        int left = 30;
        int right = 565;
        int rowHeight = 25;

        int c1 = 115;
        int c2 = 150;
        int c3 = 180;
        int c4 = 210;
        int c5 = 260;
        int c6 = 295;
        int c7 = 325;
        int c8 = 355;
        int c9 = 405;
        int c10 = 485;

        int pageNo = 1;

        PdfDocument.Page page = pdfDocument.startPage(
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create()
        );

        Canvas canvas = page.getCanvas();

        int y = drawPdfHeader(canvas, dairyName, billNo, farmer, code, preview.periodFrom, preview.periodTo, textPaint, boldPaint, linePaint);
        y = drawBillTableHeader(canvas, left, right, y, rowHeight, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, boldPaint, linePaint);

        for (DayMilkRow row : dayRows) {
            if (y + rowHeight > 720) {
                pdfDocument.finishPage(page);

                pageNo++;
                page = pdfDocument.startPage(
                        new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create()
                );

                canvas = page.getCanvas();

                y = 40;
                y = drawBillTableHeader(canvas, left, right, y, rowHeight, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, boldPaint, linePaint);
            }

            drawBillDataRow(canvas, row, y, rowHeight, left, right, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, textPaint, linePaint);
            y += rowHeight;
        }

        if (y + 120 > 780) {
            pdfDocument.finishPage(page);

            pageNo++;
            page = pdfDocument.startPage(
                    new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create()
            );

            canvas = page.getCanvas();
            y = 50;
        }

        int sumTop = y + 20;
        int sumBottom = sumTop + 70;

        canvas.drawRect(left, sumTop, right, sumBottom, linePaint);
        canvas.drawLine(200, sumTop, 200, sumBottom, linePaint);
        canvas.drawLine(380, sumTop, 380, sumBottom, linePaint);
        canvas.drawLine(left, sumTop + 35, right, sumTop + 35, linePaint);

        canvas.drawText("एकूण लिटर: " + format(totalLiters), 40, sumTop + 22, textPaint);
        canvas.drawText("दूध रक्कम: ₹ " + format(milkAmount), 215, sumTop + 22, textPaint);
        canvas.drawText("कपात: ₹ " + format(deductionAmount), 395, sumTop + 22, textPaint);

        double avgFat = totalLiters > 0 ? preview.totalFatWeighted / totalLiters : 0;

        canvas.drawText("सरासरी फॅट: " + format(avgFat), 40, sumTop + 55, textPaint);
        canvas.drawText("बोनस: ₹ " + format(bonusAmount), 215, sumTop + 55, textPaint);

        boldPaint.setTextSize(9);
        canvas.drawText("निव्वळ देय: ₹ " + format(netPayable), 395, sumTop + 55, boldPaint);

        textPaint.setTextSize(8);
        canvas.drawText("चुकभुल देणेघेणे - धन्यवाद", 220, sumBottom + 90, textPaint);

        pdfDocument.finishPage(page);

        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(folder, farmer + "_" + billNo + "_Milk_Bill.pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();
            showPdfDownloadNotification(file);
        } catch (IOException e) {
            pdfDocument.close();
            Toast.makeText(getContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
        }
    }
    private void cancelBill(BillModel bill) {

        FirebaseRefs.bills()
                .document(bill.getId())
                .update(
                        "status", "Cancelled",
                        "cancelledAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                requireContext(),
                                "Bill cancelled",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                requireContext(),
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
    private void restoreBill(BillModel bill) {

        FirebaseRefs.bills()
                .document(bill.getId())
                .update(
                        "status", "Pending",
                        "cancelledAt", null
                )
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                requireContext(),
                                "Bill restored",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                requireContext(),
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showPdfDownloadNotification(File file) {
        MediaScannerConnection.scanFile(
                getContext(),
                new String[]{file.getAbsolutePath()},
                null,
                null
        );

        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "pdf_download_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "PDF Downloads",
                    NotificationManager.IMPORTANCE_HIGH
            );

            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "PDF saved in Downloads", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(requireContext(), channelId)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Download complete")
                        .setContentText("Tap to open PDF")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        notificationManager.notify(101, builder.build());
        Toast.makeText(requireContext(), "PDF saved in Downloads", Toast.LENGTH_SHORT).show();
    }

    private int drawPdfHeader(Canvas canvas,
                              String dairyName,
                              String billNo,
                              String farmer,
                              String code,
                              String from,
                              String to,
                              Paint textPaint,
                              Paint boldPaint,
                              Paint linePaint) {
        boldPaint.setTextSize(15);
        float titleWidth = boldPaint.measureText(dairyName);
        canvas.drawText(dairyName, (595 - titleWidth) / 2, 50, boldPaint);

        textPaint.setTextSize(8);

        canvas.drawText("बिल नं: " + billNo, 40, 80, textPaint);
        canvas.drawText("कालावधी: " + from + " - " + to, 220, 80, textPaint);
        canvas.drawText("बिल दिनांक: " + android.text.format.DateFormat.format("dd-MM-yyyy", new java.util.Date()), 430, 80, textPaint);

        canvas.drawLine(30, 95, 565, 95, linePaint);

        canvas.drawText("उत्पादकाचे नाव: " + code + " - " + farmer, 40, 115, textPaint);
        canvas.drawText("शाखा: मुख्य शाखा", 430, 115, textPaint);

        canvas.drawLine(30, 130, 565, 130, linePaint);

        return 150;
    }

    private int drawBillTableHeader(Canvas canvas,
                                    int left,
                                    int right,
                                    int top,
                                    int rowHeight,
                                    int c1,
                                    int c2,
                                    int c3,
                                    int c4,
                                    int c5,
                                    int c6,
                                    int c7,
                                    int c8,
                                    int c9,
                                    int c10,
                                    Paint boldPaint,
                                    Paint linePaint) {
        int headerBottom = top + (rowHeight * 2);

        canvas.drawRect(left, top, right, headerBottom, linePaint);

        canvas.drawLine(c1, top, c1, headerBottom, linePaint);
        canvas.drawLine(c5, top, c5, headerBottom, linePaint);
        canvas.drawLine(c9, top, c9, headerBottom, linePaint);

        int subTop = top + rowHeight;

        canvas.drawLine(c2, subTop, c2, headerBottom, linePaint);
        canvas.drawLine(c3, subTop, c3, headerBottom, linePaint);
        canvas.drawLine(c4, subTop, c4, headerBottom, linePaint);

        canvas.drawLine(c6, subTop, c6, headerBottom, linePaint);
        canvas.drawLine(c7, subTop, c7, headerBottom, linePaint);
        canvas.drawLine(c8, subTop, c8, headerBottom, linePaint);

        canvas.drawLine(c10, subTop, c10, headerBottom, linePaint);
        canvas.drawLine(left, subTop, right, subTop, linePaint);

        boldPaint.setTextSize(7);

        drawCenteredText(canvas, "दिनांक", left, c1, top + 17, boldPaint);
        drawCenteredText(canvas, "सकाळ", c1, c5, top + 17, boldPaint);
        drawCenteredText(canvas, "सायंकाळ", c5, c9, top + 17, boldPaint);
        drawCenteredText(canvas, "कपात", c9, right, top + 17, boldPaint);

        int y = top + 42;

        drawCenteredText(canvas, "लिटर", c1, c2, y, boldPaint);
        drawCenteredText(canvas, "फॅट", c2, c3, y, boldPaint);
        drawCenteredText(canvas, "दर", c3, c4, y, boldPaint);
        drawCenteredText(canvas, "रक्कम", c4, c5, y, boldPaint);

        drawCenteredText(canvas, "लिटर", c5, c6, y, boldPaint);
        drawCenteredText(canvas, "फॅट", c6, c7, y, boldPaint);
        drawCenteredText(canvas, "दर", c7, c8, y, boldPaint);
        drawCenteredText(canvas, "रक्कम", c8, c9, y, boldPaint);

        drawCenteredText(canvas, "तपशील", c9, c10, y, boldPaint);
        drawCenteredText(canvas, "रक्कम", c10, right, y, boldPaint);

        return headerBottom;
    }

    private void drawBillDataRow(Canvas canvas,
                                 DayMilkRow row,
                                 int top,
                                 int rowHeight,
                                 int left,
                                 int right,
                                 int c1,
                                 int c2,
                                 int c3,
                                 int c4,
                                 int c5,
                                 int c6,
                                 int c7,
                                 int c8,
                                 int c9,
                                 int c10,
                                 Paint textPaint,
                                 Paint linePaint) {
        int bottom = top + rowHeight;
        int textY = top + 17;

        canvas.drawRect(left, top, right, bottom, linePaint);

        canvas.drawLine(c1, top, c1, bottom, linePaint);
        canvas.drawLine(c2, top, c2, bottom, linePaint);
        canvas.drawLine(c3, top, c3, bottom, linePaint);
        canvas.drawLine(c4, top, c4, bottom, linePaint);
        canvas.drawLine(c5, top, c5, bottom, linePaint);
        canvas.drawLine(c6, top, c6, bottom, linePaint);
        canvas.drawLine(c7, top, c7, bottom, linePaint);
        canvas.drawLine(c8, top, c8, bottom, linePaint);
        canvas.drawLine(c9, top, c9, bottom, linePaint);
        canvas.drawLine(c10, top, c10, bottom, linePaint);

        drawCenteredText(canvas, row.dayLabel, left, c1, textY, textPaint);

        drawCenteredText(canvas, row.morning.liters > 0 ? format(row.morning.liters) : "-", c1, c2, textY, textPaint);
        drawCenteredText(canvas, row.morning.liters > 0 ? format(row.morning.avgFat()) : "-", c2, c3, textY, textPaint);
        drawCenteredText(canvas, row.morning.liters > 0 ? format(row.morning.avgRate()) : "-", c3, c4, textY, textPaint);
        drawCenteredText(canvas, row.morning.liters > 0 ? format(row.morning.amount) : "-", c4, c5, textY, textPaint);

        drawCenteredText(canvas, row.evening.liters > 0 ? format(row.evening.liters) : "-", c5, c6, textY, textPaint);
        drawCenteredText(canvas, row.evening.liters > 0 ? format(row.evening.avgFat()) : "-", c6, c7, textY, textPaint);
        drawCenteredText(canvas, row.evening.liters > 0 ? format(row.evening.avgRate()) : "-", c7, c8, textY, textPaint);
        drawCenteredText(canvas, row.evening.liters > 0 ? format(row.evening.amount) : "-", c8, c9, textY, textPaint);

        drawCenteredText(canvas, row.deductionTitle.isEmpty() ? "-" : row.deductionTitle, c9, c10, textY, textPaint);
        drawCenteredText(canvas, row.deductionAmount > 0 ? format(row.deductionAmount) : "-", c10, right, textY, textPaint);
    }

    private void drawCenteredText(Canvas canvas, String text, float left, float right, float y, Paint paint) {
        if (text == null) text = "";
        float textWidth = paint.measureText(text);
        float x = left + ((right - left) / 2) - (textWidth / 2);
        canvas.drawText(text, x, y, paint);
    }

    private List<DayMilkRow> buildPdfDayRows(List<Map<String, Object>> milkSnapshots,
                                             List<Map<String, Object>> deductionSnapshots,
                                             String from,
                                             String to) {
        Map<String, DayMilkRow> rowMap = new LinkedHashMap<>();

        for (Map<String, Object> snapshot : milkSnapshots) {
            String dateKey = normalizePdfDateKey(safe((String) snapshot.get("date")));

            if (dateKey.isEmpty()) continue;
            if (!isDateInRange(dateKey, from, to)) continue;

            DayMilkRow row = rowMap.get(dateKey);

            if (row == null) {
                row = new DayMilkRow();
                row.date = dateKey;
                row.dayLabel = getDayLabel(dateKey);
                rowMap.put(dateKey, row);
            }

            String shift = safe((String) snapshot.get("shift"));
            double liters = getNumber(snapshot.get("liters"));
            double fat = getNumber(snapshot.get("fat"));
            double rate = getNumber(snapshot.get("rate"));
            double amount = getNumber(snapshot.get("amount"));

            if ("Morning".equalsIgnoreCase(shift)) {
                row.morning.add(liters, fat, rate, amount);
            } else {
                row.evening.add(liters, fat, rate, amount);
            }
        }

        for (Map<String, Object> snapshot : deductionSnapshots) {
            String dateKey = normalizePdfDateKey(safe((String) snapshot.get("date")));

            if (dateKey.isEmpty()) continue;
            if (!isDateInRange(dateKey, from, to)) continue;

            double amount = getNumber(snapshot.get("amount"));
            if (amount <= 0) continue;

            DayMilkRow row = rowMap.get(dateKey);

            if (row == null) {
                row = new DayMilkRow();
                row.date = dateKey;
                row.dayLabel = getDayLabel(dateKey);
                rowMap.put(dateKey, row);
            }

            String title = safe((String) snapshot.get("title"));
            if (title.isEmpty()) title = "Deduction";

            if (row.deductionTitle.isEmpty()) {
                row.deductionTitle = title;
            } else if (!row.deductionTitle.contains(title)) {
                row.deductionTitle = row.deductionTitle + "," + title;
            }

            row.deductionAmount += amount;
        }

        List<DayMilkRow> rows = new ArrayList<>(rowMap.values());

        Collections.sort(rows, (a, b) -> {
            try {
                return Long.compare(parseDate(a.date), parseDate(b.date));
            } catch (Exception e) {
                return a.date.compareTo(b.date);
            }
        });

        return rows;
    }

    private double getPdfTotalDeduction(List<DayMilkRow> rows) {
        double total = 0;

        for (DayMilkRow row : rows) {
            total += row.deductionAmount;
        }

        return total;
    }

    private String normalizePdfDateKey(String date) {
        date = safe(date).trim();

        if (date.isEmpty()) return "";

        try {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    .format(new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date));
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    .format(new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date));
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date));
        } catch (Exception ignored) {}

        return date.replace("/", "-");
    }

    private String getPdfDairyName(DocumentSnapshot userDoc) {
        String dairyName = "";

        if (userDoc != null && userDoc.exists()) {
            dairyName = safe(userDoc.getString("dairyNameMarathi"));

            if (dairyName.isEmpty()) {
                dairyName = safe(userDoc.getString("dairyName"));
            }

            if (dairyName.isEmpty()) {
                dairyName = safe(userDoc.getString("businessName"));
            }
        }

        if (dairyName.isEmpty()) {
            return "शिवशंभू मिल्क डेअरी";
        }

        String lower = dairyName.toLowerCase(Locale.US);

        if (lower.contains("shivshambhu")) {
            return "शिवशंभू मिल्क डेअरी";
        }

        return dairyName
                .replace("Milk", "मिल्क")
                .replace("milk", "मिल्क")
                .replace("Dairy", "डेअरी")
                .replace("dairy", "डेअरी");
    }

    private String getDayLabel(String date) {
        return normalizePdfDateKey(date);
    }

    private void filterBills() {
        filteredBills.clear();

        String search = etSearch.getText().toString().toLowerCase(Locale.US).trim();
        String status = spStatus.getSelectedItem() == null ? "All" : spStatus.getSelectedItem().toString();

        for (BillModel bill : billList) {
            boolean matchSearch =
                    safe(bill.getBillNo()).toLowerCase(Locale.US).contains(search)
                            || safe(bill.getFarmerName()).toLowerCase(Locale.US).contains(search)
                            || safe(bill.getFarmerCode()).toLowerCase(Locale.US).contains(search);

            boolean matchStatus = status.equals("All") || safe(bill.getStatus()).equalsIgnoreCase(status);

            if (matchSearch && matchStatus) {
                filteredBills.add(bill);
            }
        }

        adapter.updateList(filteredBills);

        if (filteredBills.isEmpty()) {

            rvBills.setVisibility(View.GONE);

            layoutEmptyBills.setVisibility(View.VISIBLE);

        } else {

            rvBills.setVisibility(View.VISIBLE);

            layoutEmptyBills.setVisibility(View.GONE);
        }
    }

    private void clearPreview() {
        pendingPreviewBills.clear();
        selectedPreview = null;

        if (layoutBillPreviewCards != null) {
            layoutBillPreviewCards.setVisibility(View.GONE);
        }

        if (layoutBillLoader != null) {
            layoutBillLoader.setVisibility(View.GONE);
        }

        if (milkRecordsDialog != null) {
            milkRecordsDialog.setVisibility(View.GONE);
        }

        if (layoutMilkRecordRows != null) {
            layoutMilkRecordRows.removeAllViews();
        }

        if (tvResult != null) {
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("No bill preview available");
        }

        if (tvBillPreviewTitle != null) tvBillPreviewTitle.setText("Bill Preview");
        if (tvTotalMilk != null) tvTotalMilk.setText("Total Milk: 0L");
        if (tvNetPayable != null) tvNetPayable.setText("Net Payable: ₹0.00");
        if (tvPreviewBonusAmount != null) tvPreviewBonusAmount.setText("₹0.00");
        if (tvPreviewDeductionAmount != null) tvPreviewDeductionAmount.setText("₹0.00");
    }

    private boolean validateDates() {
        String from = etFromDate.getText().toString().trim();
        String to = etToDate.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(requireContext(), "Select bill period", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            long fromTime = parseDate(from);
            long toTime = parseDate(to);
            long today = Calendar.getInstance().getTimeInMillis();

            if (fromTime > toTime) {
                Toast.makeText(requireContext(), "From date cannot exceed To date", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (fromTime > today || toTime > today) {
                Toast.makeText(requireContext(), "Future dates are not allowed", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean isAlreadyBilled(String farmerId, String from, String to) {
        for (BillModel bill : billList) {
            if (farmerId.equals(bill.getFarmerId())
                    && from.equals(bill.getPeriodFrom())
                    && to.equals(bill.getPeriodTo())
                    && !"Cancelled".equalsIgnoreCase(bill.getStatus())) {
                return true;
            }
        }

        return false;
    }

    private boolean isDateInRange(String date, String from, String to) {
        try {
            long d = parseDate(date);
            return d >= parseDate(from) && d <= parseDate(to);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBonusInsideBillPeriod(String bonusFrom, String bonusTo, String billFrom, String billTo) {
        try {
            long bf = parseDate(bonusFrom);
            long bt = parseDate(bonusTo);
            long f = parseDate(billFrom);
            long t = parseDate(billTo);
            return bf >= f && bt <= t;
        } catch (Exception e) {
            return false;
        }
    }

    private long parseDate(String date) throws ParseException {
        if (date == null) throw new ParseException("Null date", 0);

        try {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date).getTime();
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date).getTime();
        } catch (Exception ignored) {}

        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).getTime();
    }

    private double getMilkAmount(DocumentSnapshot doc) {
        double total = getDouble(doc, "total");
        if (total > 0) return total;

        double gross = getDouble(doc, "grossAmount");
        if (gross > 0) return gross;

        double liters = getDouble(doc, "liters");
        double rate = getDouble(doc, "rate");

        return liters * rate;
    }

    private double getDouble(DocumentSnapshot doc, String key) {
        Double value = doc.getDouble(key);
        return value == null ? 0 : value;
    }

    private double getNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (Exception ignored) {}
        }

        return 0;
    }

    private String normalizeMobile(String mobile) {
        String digits = safe(mobile).replaceAll("[^0-9]", "");

        if (digits.startsWith("91") && digits.length() > 10) {
            digits = digits.substring(2);
        }

        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }

        return digits.length() == 10 ? digits : "";
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String shortDate(String date) {
        try {
            return new SimpleDateFormat("dd-MM", Locale.US)
                    .format(new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date));
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd-MM", Locale.US)
                    .format(new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date));
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd-MM", Locale.US)
                    .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date));
        } catch (Exception ignored) {}

        return date;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatCompact(double value) {
        if (value >= 100000) {
            return String.format(Locale.US, "%.1fL", value / 100000.0);
        }
        if (value >= 1000) {
            return String.format(Locale.US, "%.1fK", value / 1000.0);
        }
        return String.format(Locale.US, "%.0f", value);
    }

    private void showCalculateError(String message) {
        layoutBillLoader.setVisibility(View.GONE);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(message == null ? "Unable to calculate bill" : message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (billsListener != null) {
            billsListener.remove();
            billsListener = null;
        }

        if (farmersListener != null) {
            farmersListener.remove();
            farmersListener = null;
        }
    }

    private static class BillPreview {
        Farmer farmer;
        String periodFrom;
        String periodTo;

        double totalLiters;
        double milkAmount;
        double bonusAmount;
        double deductionAmount;
        double netPayable;

        double totalFatWeighted;

        double morningLiters;
        double morningAmount;
        double morningFatWeighted;
        double morningRateWeighted;

        double eveningLiters;
        double eveningAmount;
        double eveningFatWeighted;
        double eveningRateWeighted;

        List<Map<String, Object>> milkSnapshots = new ArrayList<>();
        List<Map<String, Object>> bonusSnapshots = new ArrayList<>();
        List<Map<String, Object>> deductionSnapshots = new ArrayList<>();

        boolean farerIdMatches(String farmerId) {
            return farmer != null && farmer.getId() != null && farmer.getId().equals(farmerId);
        }
    }

    private static class MilkDayRow {
        String date = "";
        double morning = 0;
        double evening = 0;
    }

    private static class DayMilkRow {
        String date = "";
        String dayLabel = "";
        DayShiftValue morning = new DayShiftValue();
        DayShiftValue evening = new DayShiftValue();
        String deductionTitle = "";
        double deductionAmount = 0;
    }

    private static class DayShiftValue {
        double liters;
        double amount;
        double fatWeighted;
        double rateWeighted;

        void add(double liters, double fat, double rate, double amount) {
            this.liters += liters;
            this.amount += amount;
            this.fatWeighted += fat * liters;
            this.rateWeighted += rate * liters;
        }

        double avgFat() {
            return liters > 0 ? fatWeighted / liters : 0;
        }

        double avgRate() {
            return liters > 0 ? rateWeighted / liters : 0;
        }
    }

    private static class FirebaseFirestoreInstance {
        static WriteBatch batch() {
            return com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
        }
    }
}