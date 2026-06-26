package com.prashant.milkdairy.ui.Reports;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.prashant.milkdairy.Model.BillingReportModel;
import com.prashant.milkdairy.Model.InventoryReportModel;
import com.prashant.milkdairy.Model.MilkEntry;
import com.prashant.milkdairy.Model.YieldReportModel;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReportsFragment extends Fragment {

    // ─── Top report type tabs ────────────────────────────────────────────────
    private LinearLayout layoutmilkcol, layoutcbyeild, layoutBilling, layoutInventory;

    // ─── Period chips ─────────────────────────────────────────────────────────
    private TextView btnPeriodDaily, btnPeriod10Days, btnPeriodMonthly, btnPeriodCustom;

    // ─── Date pickers ─────────────────────────────────────────────────────────
    private LinearLayout layoutSingleDate, layoutBillingDate;
    private LinearLayout cardDatePicker, cardFromDate, cardToDate;
    private TextView tvSelectedDate, tvFromDate, tvToDate;

    // ─── Overview stat cards ──────────────────────────────────────────────────
    private TextView tvTotalLiters, tvTotalLitersamt;
    private TextView tvTotalAmount, tvPendingamt;
    private TextView tvCowBuffaloTitle, tvTotalCowBuffalo;
    private TextView tvMorningEveningTitle, tvTotalBilled;
    private TextView tvCard1Sub, tvCard2Sub, tvCard3Sub, tvCard4Sub;
    private LinearLayout layoutFourthCard;

    // ─── Sub-report card layouts ──────────────────────────────────────────────
    private LinearLayout layoutMilkCards, layoutYieldCards, layoutBillingCards, layoutInventoryCards;

    // ─── Milk buttons ─────────────────────────────────────────────────────────
    private LinearLayout btnPreviewCow, btnExcelCow, btnPdfCow;
    private LinearLayout btnPreviewBuffalo, btnExcelBuffalo, btnPdfBuffalo;
    private LinearLayout btnPreviewMix, btnExcelMix, btnPdfMix;

    // ─── Yield buttons ────────────────────────────────────────────────────────
    private LinearLayout btnPreviewCowYield, btnExcelCowYield, btnPdfCowYield;
    private LinearLayout btnPreviewBuffaloYield, btnExcelBuffaloYield, btnPdfBuffaloYield;
    private LinearLayout btnPreviewMixYield, btnExcelMixYield, btnPdfMixYield;

    // ─── Billing buttons ──────────────────────────────────────────────────────
    private LinearLayout btnPreviewPaid, btnExcelPaid, btnPdfPaid;
    private LinearLayout btnPreviewPending, btnExcelPending, btnPdfPending;
    private LinearLayout btnPreviewCancelled, btnExcelCancelled, btnPdfCancelled;

    // ─── Inventory buttons ────────────────────────────────────────────────────
    private LinearLayout btnPreviewStockIn, btnExcelStockIn, btnPdfStockIn;
    private LinearLayout btnPreviewStockOut, btnExcelStockOut, btnPdfStockOut;
    private LinearLayout btnPreviewLowStock, btnExcelLowStock, btnPdfLowStock;

    // ─── State ────────────────────────────────────────────────────────────────
    private String currentModule = "milk";
    private String currentPeriod = "daily";

    private Calendar calendar, fromCalendar, toCalendar;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    // ─── Data caches ──────────────────────────────────────────────────────────
    private final List<MilkEntry>          cowMilkList        = new ArrayList<>();
    private final List<MilkEntry>          buffaloMilkList    = new ArrayList<>();
    private final List<MilkEntry>          mixMilkList        = new ArrayList<>();

    private final List<YieldReportModel>   cowYieldList       = new ArrayList<>();
    private final List<YieldReportModel>   buffaloYieldList   = new ArrayList<>();
    private final List<YieldReportModel>   mixYieldList       = new ArrayList<>();

    private final List<BillingReportModel> paidBillingList    = new ArrayList<>();
    private final List<BillingReportModel> pendingBillingList = new ArrayList<>();
    private final List<BillingReportModel> cancelledBillingList = new ArrayList<>();

    private final List<InventoryReportModel> stockInList    = new ArrayList<>();
    private final List<InventoryReportModel> stockOutList   = new ArrayList<>();
    private final List<InventoryReportModel> lowStockList   = new ArrayList<>();

    // ─── Headers ──────────────────────────────────────────────────────────────
    private static final String[] MILK_HEADERS      = {"Shift", "Farmer", "Mobile", "Liters", "FAT", "SNF", "Rate", "Amount"};
    private static final String[] YIELD_HEADERS     = {"Farmer", "Cow L", "Buffalo L", "Mix L", "Cow ₹", "Buffalo ₹", "Total ₹"};
    private static final String[] BILLING_HEADERS   = {"Code", "Farmer", "Milk ₹", "Bonus", "Deduction", "Net", "Status"};
    private static final String[] INVENTORY_HEADERS = {"Item", "Category", "Stock In", "Stock Out", "Current", "Unit", "Value"};

    public ReportsFragment() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE VIEW
    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        initViews(view);
        initCalendars();
        setupTopButtons();
        setupPeriodButtons();
        setupDatePickers();
        selectModule("milk");
        return view;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INIT VIEWS
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews(View v) {
        layoutmilkcol   = v.findViewById(R.id.layoutmilkcol);
        layoutcbyeild   = v.findViewById(R.id.layoutcbyeild);
        layoutBilling   = v.findViewById(R.id.layoutBilling);
        layoutInventory = v.findViewById(R.id.layoutInventory);

        btnPeriodDaily   = v.findViewById(R.id.btnPeriodDaily);
        btnPeriod10Days  = v.findViewById(R.id.btnPeriod10Days);
        btnPeriodMonthly = v.findViewById(R.id.btnPeriodMonthly);
        btnPeriodCustom  = v.findViewById(R.id.btnPeriodCustom);

        layoutSingleDate  = v.findViewById(R.id.layoutSingleDate);
        layoutBillingDate = v.findViewById(R.id.layoutBillingDate);
        cardDatePicker    = v.findViewById(R.id.cardDatePicker);
        cardFromDate      = v.findViewById(R.id.cardFromDate);
        cardToDate        = v.findViewById(R.id.cardToDate);
        tvSelectedDate    = v.findViewById(R.id.tvSelectedDate);
        tvFromDate        = v.findViewById(R.id.tvFromDate);
        tvToDate          = v.findViewById(R.id.tvToDate);

        tvTotalLiters         = v.findViewById(R.id.tvTotalLiters);
        tvTotalLitersamt      = v.findViewById(R.id.tvTotalLitersamt);
        tvTotalAmount         = v.findViewById(R.id.tvTotalAmount);
        tvPendingamt          = v.findViewById(R.id.tvPendingamt);
        tvCowBuffaloTitle     = v.findViewById(R.id.tvCowBuffaloTitle);
        tvTotalCowBuffalo     = v.findViewById(R.id.tvTotalCowBuffalo);
        tvMorningEveningTitle = v.findViewById(R.id.tvMorningEveningTitle);
        tvTotalBilled         = v.findViewById(R.id.tvTotalBilled);
        tvCard1Sub            = v.findViewById(R.id.tvCard1Sub);
        tvCard2Sub            = v.findViewById(R.id.tvCard2Sub);
        tvCard3Sub            = v.findViewById(R.id.tvCard3Sub);
        tvCard4Sub            = v.findViewById(R.id.tvCard4Sub);
        layoutFourthCard      = v.findViewById(R.id.layoutFourthCard);

        layoutMilkCards      = v.findViewById(R.id.layoutMilkCards);
        layoutYieldCards     = v.findViewById(R.id.layoutYieldCards);
        layoutBillingCards   = v.findViewById(R.id.layoutBillingCards);
        layoutInventoryCards = v.findViewById(R.id.layoutInventoryCards);

        // Milk
        btnPreviewCow     = v.findViewById(R.id.btnPreviewCow);
        btnExcelCow       = v.findViewById(R.id.btnExcelCow);
        btnPdfCow         = v.findViewById(R.id.btnPdfCow);
        btnPreviewBuffalo = v.findViewById(R.id.btnPreviewBuffalo);
        btnExcelBuffalo   = v.findViewById(R.id.btnExcelBuffalo);
        btnPdfBuffalo     = v.findViewById(R.id.btnPdfBuffalo);
        btnPreviewMix     = v.findViewById(R.id.btnPreviewMix);
        btnExcelMix       = v.findViewById(R.id.btnExcelMix);
        btnPdfMix         = v.findViewById(R.id.btnPdfMix);

        // Yield
        btnPreviewCowYield     = v.findViewById(R.id.btnPreviewCowYield);
        btnExcelCowYield       = v.findViewById(R.id.btnExcelCowYield);
        btnPdfCowYield         = v.findViewById(R.id.btnPdfCowYield);
        btnPreviewBuffaloYield = v.findViewById(R.id.btnPreviewBuffaloYield);
        btnExcelBuffaloYield   = v.findViewById(R.id.btnExcelBuffaloYield);
        btnPdfBuffaloYield     = v.findViewById(R.id.btnPdfBuffaloYield);
        btnPreviewMixYield     = v.findViewById(R.id.btnPreviewMixYield);
        btnExcelMixYield       = v.findViewById(R.id.btnExcelMixYield);
        btnPdfMixYield         = v.findViewById(R.id.btnPdfMixYield);

        // Billing
        btnPreviewPaid      = v.findViewById(R.id.btnPreviewPaid);
        btnExcelPaid        = v.findViewById(R.id.btnExcelPaid);
        btnPdfPaid          = v.findViewById(R.id.btnPdfPaid);
        btnPreviewPending   = v.findViewById(R.id.btnPreviewPending);
        btnExcelPending     = v.findViewById(R.id.btnExcelPending);
        btnPdfPending       = v.findViewById(R.id.btnPdfPending);
        btnPreviewCancelled = v.findViewById(R.id.btnPreviewCancelled);
        btnExcelCancelled   = v.findViewById(R.id.btnExcelCancelled);
        btnPdfCancelled     = v.findViewById(R.id.btnPdfCancelled);

        // Inventory
        btnPreviewStockIn  = v.findViewById(R.id.btnPreviewStockIn);
        btnExcelStockIn    = v.findViewById(R.id.btnExcelStockIn);
        btnPdfStockIn      = v.findViewById(R.id.btnPdfStockIn);
        btnPreviewStockOut = v.findViewById(R.id.btnPreviewStockOut);
        btnExcelStockOut   = v.findViewById(R.id.btnExcelStockOut);
        btnPdfStockOut     = v.findViewById(R.id.btnPdfStockOut);
        btnPreviewLowStock = v.findViewById(R.id.btnPreviewLowStock);
        btnExcelLowStock   = v.findViewById(R.id.btnExcelLowStock);
        btnPdfLowStock     = v.findViewById(R.id.btnPdfLowStock);
    }

    private void initCalendars() {
        calendar     = Calendar.getInstance();
        fromCalendar = Calendar.getInstance();
        toCalendar   = Calendar.getInstance();
        updateDateText();
        setTenDaysDate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTENERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupTopButtons() {
        layoutmilkcol.setOnClickListener(v -> selectModule("milk"));
        layoutcbyeild.setOnClickListener(v -> selectModule("yield"));
        layoutBilling.setOnClickListener(v -> selectModule("billing"));
        layoutInventory.setOnClickListener(v -> selectModule("inventory"));
    }

    private void setupPeriodButtons() {
        btnPeriodDaily.setOnClickListener(v -> {
            currentPeriod = "daily";
            selectPeriodChip(btnPeriodDaily);
            applyDateLayout();
            refreshData();
        });
        btnPeriod10Days.setOnClickListener(v -> {
            currentPeriod = "10days";
            selectPeriodChip(btnPeriod10Days);
            applyDateLayout();
            refreshData();
        });
        btnPeriodMonthly.setOnClickListener(v -> {
            currentPeriod = "monthly";
            selectPeriodChip(btnPeriodMonthly);
            applyDateLayout();
            refreshData();
        });
        btnPeriodCustom.setOnClickListener(v -> {
            currentPeriod = "custom";
            selectPeriodChip(btnPeriodCustom);
            applyDateLayout();
            // User picks dates manually
        });
    }

    private void setupDatePickers() {
        cardDatePicker.setOnClickListener(v -> openDatePicker());
        cardFromDate.setOnClickListener(v -> openFromDatePicker());
        cardToDate.setOnClickListener(v -> openToDatePicker());
    }

    // ─── Per-module button wiring (called each time module changes) ───────────

    private void setupMilkButtons() {
        btnPreviewCow.setOnClickListener(v -> openPreview("milk_cow"));
        btnExcelCow.setOnClickListener(v   -> exportCsvMilk("CowMilk", cowMilkList));
        btnPdfCow.setOnClickListener(v     -> exportPdf("Cow Milk Collection", cowMilkList, MILK_HEADERS, "CowMilk"));

        btnPreviewBuffalo.setOnClickListener(v -> openPreview("milk_buffalo"));
        btnExcelBuffalo.setOnClickListener(v   -> exportCsvMilk("BuffaloMilk", buffaloMilkList));
        btnPdfBuffalo.setOnClickListener(v     -> exportPdf("Buffalo Milk Collection", buffaloMilkList, MILK_HEADERS, "BuffaloMilk"));

        btnPreviewMix.setOnClickListener(v -> openPreview("milk_mix"));
        btnExcelMix.setOnClickListener(v   -> exportCsvMilk("MixMilk", mixMilkList));
        btnPdfMix.setOnClickListener(v     -> exportPdf("Mix Milk Collection", mixMilkList, MILK_HEADERS, "MixMilk"));
    }

    private void setupYieldButtons() {
        btnPreviewCowYield.setOnClickListener(v -> openPreview("yield_cow"));
        btnExcelCowYield.setOnClickListener(v   -> exportCsvYield("CowYield", cowYieldList));
        btnPdfCowYield.setOnClickListener(v     -> exportPdf("Cow Yield Report", cowYieldList, YIELD_HEADERS, "CowYield"));

        btnPreviewBuffaloYield.setOnClickListener(v -> openPreview("yield_buffalo"));
        btnExcelBuffaloYield.setOnClickListener(v   -> exportCsvYield("BuffaloYield", buffaloYieldList));
        btnPdfBuffaloYield.setOnClickListener(v     -> exportPdf("Buffalo Yield Report", buffaloYieldList, YIELD_HEADERS, "BuffaloYield"));

        btnPreviewMixYield.setOnClickListener(v -> openPreview("yield_mix"));
        btnExcelMixYield.setOnClickListener(v   -> exportCsvYield("MixYield", mixYieldList));
        btnPdfMixYield.setOnClickListener(v     -> exportPdf("Mix Yield Report", mixYieldList, YIELD_HEADERS, "MixYield"));
    }

    private void setupBillingButtons() {
        btnPreviewPaid.setOnClickListener(v -> openPreview("billing_paid"));
        btnExcelPaid.setOnClickListener(v   -> exportCsvBilling("PaidBills", paidBillingList));
        btnPdfPaid.setOnClickListener(v     -> exportPdf("Paid Bills", paidBillingList, BILLING_HEADERS, "PaidBills"));

        btnPreviewPending.setOnClickListener(v -> openPreview("billing_pending"));
        btnExcelPending.setOnClickListener(v   -> exportCsvBilling("PendingBills", pendingBillingList));
        btnPdfPending.setOnClickListener(v     -> exportPdf("Pending Bills", pendingBillingList, BILLING_HEADERS, "PendingBills"));

        btnPreviewCancelled.setOnClickListener(v -> openPreview("billing_cancelled"));
        btnExcelCancelled.setOnClickListener(v   -> exportCsvBilling("CancelledBills", cancelledBillingList));
        btnPdfCancelled.setOnClickListener(v     -> exportPdf("Cancelled Bills", cancelledBillingList, BILLING_HEADERS, "CancelledBills"));
    }

    private void setupInventoryButtons() {
        btnPreviewStockIn.setOnClickListener(v -> openPreview("inv_stock_in"));
        btnExcelStockIn.setOnClickListener(v   -> exportCsvInventory("StockIn", stockInList));
        btnPdfStockIn.setOnClickListener(v     -> exportPdf("Stock In Report", stockInList, INVENTORY_HEADERS, "StockIn"));

        btnPreviewStockOut.setOnClickListener(v -> openPreview("inv_stock_out"));
        btnExcelStockOut.setOnClickListener(v   -> exportCsvInventory("StockOut", stockOutList));
        btnPdfStockOut.setOnClickListener(v     -> exportPdf("Stock Out Report", stockOutList, INVENTORY_HEADERS, "StockOut"));

        btnPreviewLowStock.setOnClickListener(v -> openPreview("inv_low_stock"));
        btnExcelLowStock.setOnClickListener(v   -> exportCsvInventory("LowStock", lowStockList));
        btnPdfLowStock.setOnClickListener(v     -> exportPdf("Low Stock Report", lowStockList, INVENTORY_HEADERS, "LowStock"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODULE SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void selectModule(String module) {
        currentModule = module;
        currentPeriod = "daily";
        selectPeriodChip(btnPeriodDaily);
        selectTopTab(module);
        showSubCards(module);
        applyDateLayout();
        setupModuleButtons(module);
        refreshData();
    }

    private void selectTopTab(String module) {
        setTabSelected(layoutmilkcol,   "milk".equals(module));
        setTabSelected(layoutcbyeild,   "yield".equals(module));
        setTabSelected(layoutBilling,   "billing".equals(module));
        setTabSelected(layoutInventory, "inventory".equals(module));
    }

    private void setTabSelected(LinearLayout layout, boolean selected) {

        layout.setBackgroundResource(
                selected
                        ? R.drawable.bg_milk_selected_green
                        : R.drawable.bg_unselected
        );

        updateTextColors(layout, selected);
    }

    private void updateTextColors(View view, boolean selected) {

        if (view instanceof TextView) {

            TextView textView = (TextView) view;

            if (selected) {

                textView.setTextColor(Color.WHITE);

            } else {

                TypedValue typedValue = new TypedValue();

                requireContext().getTheme().resolveAttribute(
                        android.R.attr.textColorPrimary,
                        typedValue,
                        true
                );

                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                typedValue.resourceId
                        )
                );
            }
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                updateTextColors(group.getChildAt(i), selected);
            }
        }
    }

    private void showSubCards(String module) {
        layoutMilkCards.setVisibility("milk".equals(module)          ? View.VISIBLE : View.GONE);
        layoutYieldCards.setVisibility("yield".equals(module)        ? View.VISIBLE : View.GONE);
        layoutBillingCards.setVisibility("billing".equals(module)    ? View.VISIBLE : View.GONE);
        layoutInventoryCards.setVisibility("inventory".equals(module) ? View.VISIBLE : View.GONE);
    }

    private void setupModuleButtons(String module) {
        switch (module) {
            case "milk":      setupMilkButtons();      break;
            case "yield":     setupYieldButtons();     break;
            case "billing":   setupBillingButtons();   break;
            case "inventory": setupInventoryButtons(); break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PERIOD & DATE LAYOUT
    // ─────────────────────────────────────────────────────────────────────────

    private void applyDateLayout() {
        boolean showSingle = "daily".equals(currentPeriod);
        layoutSingleDate.setVisibility(showSingle  ? View.VISIBLE : View.GONE);
        layoutBillingDate.setVisibility(!showSingle ? View.VISIBLE : View.GONE);

        if ("10days".equals(currentPeriod))  setTenDaysDate();
        if ("monthly".equals(currentPeriod)) setMonthlyRange();
        if ("daily".equals(currentPeriod))   updateDateText();
    }

    private void setTenDaysDate() {
        Calendar today       = Calendar.getInstance();
        Calendar tenBack     = Calendar.getInstance();
        tenBack.add(Calendar.DAY_OF_MONTH, -10);
        fromCalendar.setTime(tenBack.getTime());
        toCalendar.setTime(today.getTime());
        tvFromDate.setText(sdf.format(fromCalendar.getTime()));
        tvToDate.setText(sdf.format(toCalendar.getTime()));
    }

    private void setMonthlyRange() {
        // True month: first day of current month → today
        Calendar first = Calendar.getInstance();
        first.set(Calendar.DAY_OF_MONTH, 1);
        fromCalendar.setTime(first.getTime());
        toCalendar.setTime(Calendar.getInstance().getTime());
        tvFromDate.setText(sdf.format(fromCalendar.getTime()));
        tvToDate.setText(sdf.format(toCalendar.getTime()));
    }

    private void updateDateText() {
        tvSelectedDate.setText(sdf.format(calendar.getTime()));
    }

    private String getFromDate() {
        if ("daily".equals(currentPeriod)) return tvSelectedDate.getText().toString();
        return tvFromDate.getText().toString();
    }

    private String getToDate() {
        if ("daily".equals(currentPeriod)) return tvSelectedDate.getText().toString();
        return tvToDate.getText().toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PERIOD CHIP SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void selectPeriodChip(TextView selected) {
        TextView[] chips = {btnPeriodDaily, btnPeriod10Days, btnPeriodMonthly, btnPeriodCustom};
        for (TextView chip : chips) {
            boolean on = (chip == selected);
            chip.setBackgroundResource(on ? R.drawable.bg_milk_selected_green : R.drawable.bg_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    on ? android.R.color.white : R.color.text_secondary));
            chip.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA REFRESH ROUTER
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshData() {
        switch (currentModule) {
            case "milk":      fetchMilkData();      break;
            case "yield":     fetchYieldData();     break;
            case "billing":   fetchBillingData();   break;
            case "inventory": fetchInventoryData(); break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FLOW 1 – MILK COLLECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchMilkData() {
        cowMilkList.clear();
        buffaloMilkList.clear();
        mixMilkList.clear();

        String from = getFromDate();
        String to   = getToDate();

        if (!validateDateRange(from, to)) return;

        FirebaseRefs.milkCollection().get()
                .addOnSuccessListener(snapshots -> {
                    double totalLiters = 0, totalAmount = 0;
                    Set<String> farmerIds = new HashSet<>();
                    int totalEntries = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String date = firstString(doc, "collectionDate", "date", "createdDate");
                        if (!isDateInRange(date, from, to)) continue;

                        String milkType   = firstString(doc, "milkType", "type");
                        String shift      = firstString(doc, "shift");
                        String farmerId   = firstString(doc, "farmerId", "farmerUid");
                        String farmerName = firstString(doc, "farmerName", "name");
                        String mobile     = firstString(doc, "mobile", "mobileNumber", "farmerMobile");
                        double liters     = firstDouble(doc, "liters", "quantity", "milkLiter");
                        double fat        = firstDouble(doc, "fat", "fatPercent");
                        double snf        = firstDouble(doc, "snf", "snfPercent");
                        double rate       = firstDouble(doc, "rate", "milkRate");
                        double amount     = getMilkAmount(doc);

                        totalLiters  += liters;
                        totalAmount  += amount;
                        totalEntries++;
                        if (!farmerId.isEmpty()) farmerIds.add(farmerId);

                        MilkEntry entry = new MilkEntry(shift, farmerName, mobile, liters, fat, snf, rate, amount);

                        if (isCow(milkType))        cowMilkList.add(entry);
                        else if (isBuffalo(milkType)) buffaloMilkList.add(entry);
                        else                          mixMilkList.add(entry);
                    }

                    // Overview cards – Milk module
                    tvTotalLiters.setText("Total Milk\nCollection");
                    tvTotalLitersamt.setText(fmt(totalLiters) + " L");
                    tvTotalAmount.setText("Total\nAmount");
                    tvPendingamt.setText("₹" + fmt(totalAmount));
                    tvCowBuffaloTitle.setText("Total\nFarmers");
                    tvTotalCowBuffalo.setText(String.valueOf(farmerIds.size()));
                    tvMorningEveningTitle.setText("Total\nEntries");
                    tvTotalBilled.setText(String.valueOf(totalEntries));

                    // Sub-stats
                    tvCard1Sub.setText("Cow: " + cowMilkList.size());
                    tvCard2Sub.setText("Buffalo: " + buffaloMilkList.size());
                    tvCard3Sub.setText("Mix: " + mixMilkList.size());
                    tvCard4Sub.setText(from.equals(to) ? from : from + "→" + to);
                    layoutFourthCard.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> showNetworkError());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FLOW 2 – COW/BUFFALO YIELD
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchYieldData() {
        cowYieldList.clear();
        buffaloYieldList.clear();
        mixYieldList.clear();

        String from = getFromDate();
        String to   = getToDate();

        if (!validateDateRange(from, to)) return;

        FirebaseRefs.milkCollection().get()
                .addOnSuccessListener(snapshots -> {
                    Map<String, YieldAgg> cowMap     = new HashMap<>();
                    Map<String, YieldAgg> buffaloMap = new HashMap<>();
                    Map<String, YieldAgg> mixMap     = new HashMap<>();

                    double cowL = 0, bufL = 0, mixL = 0;
                    Set<String> farmerIds = new HashSet<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String date = firstString(doc, "collectionDate", "date", "createdDate");
                        if (!isDateInRange(date, from, to)) continue;

                        String milkType   = firstString(doc, "milkType", "type");
                        String farmerId   = firstString(doc, "farmerId", "farmerUid");
                        String farmerName = firstString(doc, "farmerName", "name");
                        String key        = farmerId.isEmpty() ? farmerName : farmerId;
                        double liters     = firstDouble(doc, "liters", "quantity", "milkLiter");
                        double amount     = getMilkAmount(doc);

                        if (!farmerId.isEmpty()) farmerIds.add(farmerId);

                        Map<String, YieldAgg> targetMap;
                        if      (isCow(milkType))     { targetMap = cowMap;     cowL += liters; }
                        else if (isBuffalo(milkType)) { targetMap = buffaloMap; bufL += liters; }
                        else                          { targetMap = mixMap;     mixL += liters; }

                        YieldAgg agg = targetMap.get(key);
                        if (agg == null) {
                            agg = new YieldAgg();
                            agg.farmerName = farmerName.isEmpty() ? key : farmerName;
                            targetMap.put(key, agg);
                        }
                        if      (isCow(milkType))     { agg.cowLiters     += liters; agg.cowAmount     += amount; }
                        else if (isBuffalo(milkType)) { agg.buffaloLiters += liters; agg.buffaloAmount += amount; }
                        else                          { agg.mixLiters     += liters; agg.mixAmount     += amount; }
                    }

                    for (YieldAgg a : cowMap.values())
                        cowYieldList.add(new YieldReportModel(a.farmerName, a.cowLiters, 0, 0, a.cowAmount, 0, a.cowAmount));
                    for (YieldAgg a : buffaloMap.values())
                        buffaloYieldList.add(new YieldReportModel(a.farmerName, 0, a.buffaloLiters, 0, 0, a.buffaloAmount, a.buffaloAmount));
                    for (YieldAgg a : mixMap.values())
                        mixYieldList.add(new YieldReportModel(a.farmerName, 0, 0, a.mixLiters, 0, 0, a.mixAmount));

                    // Overview cards – Yield module
                    tvTotalLiters.setText("Cow Milk");
                    tvTotalLitersamt.setText(fmt(cowL) + " L");
                    tvTotalAmount.setText("Buffalo Milk");
                    tvPendingamt.setText(fmt(bufL) + " L");
                    tvCowBuffaloTitle.setText("Mix Milk");
                    tvTotalCowBuffalo.setText(fmt(mixL) + " L");
                    tvMorningEveningTitle.setText("Total\nFarmers");
                    tvTotalBilled.setText(String.valueOf(farmerIds.size()));
                    tvCard1Sub.setText("Cow: " + cowYieldList.size());
                    tvCard2Sub.setText("Buf: " + buffaloYieldList.size());
                    tvCard3Sub.setText("Mix: " + mixYieldList.size());
                    tvCard4Sub.setText(from.equals(to) ? from : from + "→" + to);
                    layoutFourthCard.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> showNetworkError());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FLOW 3 – BILLING
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchBillingData() {
        paidBillingList.clear();
        pendingBillingList.clear();
        cancelledBillingList.clear();

        String from = getFromDate();
        String to   = getToDate();

        if (!validateDateRange(from, to)) return;

        FirebaseRefs.bills().get()
                .addOnSuccessListener(snapshots -> {
                    double milkAmount = 0, deduction = 0, net = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String periodFrom = firstString(doc, "periodFrom", "fromDate", "startDate", "date");
                        String periodTo   = firstString(doc, "periodTo", "toDate", "endDate", "date");

                        if (!dateRangesOverlap(periodFrom, periodTo, from, to)) continue;

                        String status     = firstString(doc, "status", "billStatus");
                        if (status.isEmpty()) status = "Pending";

                        double milk    = firstDouble(doc, "totalMilkAmount", "milkAmount", "grossMilkAmount");
                        double ded     = firstDouble(doc, "totalDeduction", "deductionAmount", "deductions");
                        double bon     = firstDouble(doc, "totalBonus", "bonusAmount", "bonus");
                        double payable = firstDouble(doc, "netPayable", "payableAmount", "totalPayable");
                        if (payable == 0) payable = milk + bon - ded;

                        String farmerCode = firstString(doc, "farmerCode", "code");
                        String farmerName = firstString(doc, "farmerName", "name");

                        BillingReportModel model = new BillingReportModel(
                                farmerCode, farmerName, milk, bon, ded, payable, status);

                        if ("Paid".equalsIgnoreCase(status))          paidBillingList.add(model);
                        else if ("Cancelled".equalsIgnoreCase(status)) cancelledBillingList.add(model);
                        else                                           pendingBillingList.add(model);

                        if (!"Cancelled".equalsIgnoreCase(status)) {
                            milkAmount += milk;
                            deduction  += ded;
                            net        += payable;
                        }
                    }

                    int total = paidBillingList.size() + pendingBillingList.size() + cancelledBillingList.size();

                    tvTotalLiters.setText("Total\nBills");
                    tvTotalLitersamt.setText(String.valueOf(total));
                    tvTotalAmount.setText("Milk\nAmount");
                    tvPendingamt.setText("₹" + fmt(milkAmount));
                    tvCowBuffaloTitle.setText("Deduction");
                    tvTotalCowBuffalo.setText("₹" + fmt(deduction));
                    tvMorningEveningTitle.setText("Net\nPayable");
                    tvTotalBilled.setText("₹" + fmt(net));
                    tvCard1Sub.setText("Paid: " + paidBillingList.size());
                    tvCard2Sub.setText("Pend: " + pendingBillingList.size());
                    tvCard3Sub.setText("Can: " + cancelledBillingList.size());
                    tvCard4Sub.setText(from.equals(to) ? from : from + "→" + to);
                    layoutFourthCard.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> showNetworkError());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FLOW 4 – INVENTORY
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchInventoryData() {
        stockInList.clear();
        stockOutList.clear();
        lowStockList.clear();

        String from = getFromDate();
        String to   = getToDate();

        if (!validateDateRange(from, to)) return;

        FirebaseRefs.inventoryTransactions().get()
                .addOnSuccessListener(txnSnaps ->
                        FirebaseRefs.inventoryItems().get()
                                .addOnSuccessListener(itemSnaps ->
                                        bindInventoryData(txnSnaps.getDocuments(), itemSnaps.getDocuments(), from, to))
                                .addOnFailureListener(e -> showNetworkError()))
                .addOnFailureListener(e -> showNetworkError());
    }

    private void bindInventoryData(List<DocumentSnapshot> txns,
                                   List<DocumentSnapshot> items,
                                   String from, String to) {

        Map<String, InventoryAgg> stockInMap  = new HashMap<>();
        Map<String, InventoryAgg> stockOutMap = new HashMap<>();
        double totalIn = 0, totalOut = 0, totalSales = 0;
        int lowCount = 0;

        // Low stock from items collection (not date-filtered)
        for (DocumentSnapshot item : items) {
            Boolean active = item.getBoolean("isActive");
            if (Boolean.FALSE.equals(active)) continue;
            double curr = firstDouble(item, "currentStock", "stock");
            double min  = firstDouble(item, "minimumStock", "minStock");
            if (curr <= min) {
                lowCount++;
                lowStockList.add(new InventoryReportModel(
                        firstString(item, "itemName", "name"),
                        firstString(item, "category"),
                        0, 0, curr,
                        firstString(item, "unit"),
                        curr * firstDouble(item, "purchaseRate", "rate")));
            }
        }

        for (DocumentSnapshot doc : txns) {
            String txnDate = getTxnDate(doc);
            if (!isDateInRange(txnDate, from, to)) continue;

            String type   = firstString(doc, "type", "transactionType");
            boolean isIn  = isStockIn(type);
            boolean isOut = isFarmerStockOut(doc);
            if (!isIn && !isOut) continue;

            String itemId   = firstString(doc, "itemId", "inventoryItemId");
            String itemName = firstString(doc, "itemName", "name");
            String key      = itemId.isEmpty() ? itemName : itemId;
            double qty      = firstDouble(doc, "quantity", "qty");
            double amount   = firstDouble(doc, "totalAmount", "amount");
            double curr     = firstDouble(doc, "newStock", "currentStock", "balanceStock");

            if (isIn) {
                totalIn += qty;
                InventoryAgg agg = stockInMap.get(key);
                if (agg == null) {
                    agg = new InventoryAgg();
                    agg.itemName = itemName; agg.category = firstString(doc, "category"); agg.unit = firstString(doc, "unit");
                    stockInMap.put(key, agg);
                }
                agg.stockIn += qty; agg.currentStock = curr;
            }
            if (isOut) {
                totalOut   += qty;
                totalSales += amount;
                InventoryAgg agg = stockOutMap.get(key);
                if (agg == null) {
                    agg = new InventoryAgg();
                    agg.itemName = itemName; agg.category = firstString(doc, "category"); agg.unit = firstString(doc, "unit");
                    stockOutMap.put(key, agg);
                }
                agg.stockOut += qty; agg.currentStock = curr; agg.stockValue += amount;
            }
        }

        for (InventoryAgg a : stockInMap.values())
            stockInList.add(new InventoryReportModel(a.itemName, a.category, a.stockIn, 0, a.currentStock, a.unit, 0));
        for (InventoryAgg a : stockOutMap.values())
            stockOutList.add(new InventoryReportModel(a.itemName, a.category, 0, a.stockOut, a.currentStock, a.unit, a.stockValue));

        tvTotalLiters.setText("Stock In");
        tvTotalLitersamt.setText(fmt(totalIn));
        tvTotalAmount.setText("Stock Out");
        tvPendingamt.setText(fmt(totalOut));
        tvCowBuffaloTitle.setText("Sales");
        tvTotalCowBuffalo.setText("₹" + fmt(totalSales));
        tvMorningEveningTitle.setText("Low Stock");
        tvTotalBilled.setText(String.valueOf(lowCount));
        tvCard1Sub.setText("In: " + stockInList.size());
        tvCard2Sub.setText("Out: " + stockOutList.size());
        tvCard3Sub.setText("Low: " + lowCount);
        tvCard4Sub.setText(from.equals(to) ? from : from + "→" + to);
        layoutFourthCard.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PREVIEW DIALOG
    // ─────────────────────────────────────────────────────────────────────────

    private void openPreview(String type) {
        List<?> data;
        String  title;
        String[] headers;

        switch (type) {
            case "milk_cow":          title = "Cow Milk Collection";   data = cowMilkList;          headers = MILK_HEADERS;      break;
            case "milk_buffalo":      title = "Buffalo Milk";          data = buffaloMilkList;      headers = MILK_HEADERS;      break;
            case "milk_mix":          title = "Mix Milk Collection";   data = mixMilkList;          headers = MILK_HEADERS;      break;
            case "yield_cow":         title = "Cow Yield";             data = cowYieldList;         headers = YIELD_HEADERS;     break;
            case "yield_buffalo":     title = "Buffalo Yield";         data = buffaloYieldList;     headers = YIELD_HEADERS;     break;
            case "yield_mix":         title = "Mix Yield";             data = mixYieldList;         headers = YIELD_HEADERS;     break;
            case "billing_paid":      title = "Paid Bills";            data = paidBillingList;      headers = BILLING_HEADERS;   break;
            case "billing_pending":   title = "Pending Bills";         data = pendingBillingList;   headers = BILLING_HEADERS;   break;
            case "billing_cancelled": title = "Cancelled Bills";       data = cancelledBillingList; headers = BILLING_HEADERS;   break;
            case "inv_stock_in":      title = "Stock In Report";       data = stockInList;          headers = INVENTORY_HEADERS; break;
            case "inv_stock_out":     title = "Stock Out Report";      data = stockOutList;         headers = INVENTORY_HEADERS; break;
            case "inv_low_stock":     title = "Low Stock Alert";       data = lowStockList;         headers = INVENTORY_HEADERS; break;
            default: return;
        }

        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "No Records Found for Selected Period", Toast.LENGTH_SHORT).show();
            return;
        }

        String period = getFromDate().equals(getToDate())
                ? getFromDate()
                : getFromDate() + " → " + getToDate();

        ReportPreviewDialogFragment dialog =
                ReportPreviewDialogFragment.newInstance(title, period, type, data, headers);
        dialog.show(getParentFragmentManager(), "report_preview");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CSV EXPORT
    // ─────────────────────────────────────────────────────────────────────────

    private void exportCsvMilk(String prefix, List<MilkEntry> list) {
        if (list.isEmpty()) { showNoData(); return; }
        try {
            File file = csvFile(prefix);
            FileWriter w = new FileWriter(file);
            writeCsvRow(w, MILK_HEADERS);
            for (MilkEntry e : list)
                writeCsvRow(w, new String[]{e.getShift(), e.getFarmerName(), e.getMobile(),
                        fmt(e.getLiters()), fmt(e.getFat()), fmt(e.getSnf()), fmt(e.getRate()), fmt(e.getAmount())});
            w.flush(); w.close();
            Toast.makeText(requireContext(), prefix + " Exported", Toast.LENGTH_SHORT).show();
            openFile(file, "text/csv");
        } catch (Exception e) { showExportError(e); }
    }

    private void exportCsvYield(String prefix, List<YieldReportModel> list) {
        if (list.isEmpty()) { showNoData(); return; }
        try {
            File file = csvFile(prefix);
            FileWriter w = new FileWriter(file);
            writeCsvRow(w, YIELD_HEADERS);
            for (YieldReportModel y : list)
                writeCsvRow(w, new String[]{y.farmerName,
                        fmt(y.cowLiters), fmt(y.buffaloLiters), fmt(y.mixLiters),
                        fmt(y.cowAmount), fmt(y.buffaloAmount), fmt(y.totalAmount)});
            w.flush(); w.close();
            Toast.makeText(requireContext(), prefix + " Exported", Toast.LENGTH_SHORT).show();
            openFile(file, "text/csv");
        } catch (Exception e) { showExportError(e); }
    }

    private void exportCsvBilling(String prefix, List<BillingReportModel> list) {
        if (list.isEmpty()) { showNoData(); return; }
        try {
            File file = csvFile(prefix);
            FileWriter w = new FileWriter(file);
            writeCsvRow(w, BILLING_HEADERS);
            for (BillingReportModel b : list)
                writeCsvRow(w, new String[]{b.farmerCode, b.farmerName,
                        fmt(b.milkAmount), fmt(b.bonus), fmt(b.deduction), fmt(b.netPayable), b.status});
            w.flush(); w.close();
            Toast.makeText(requireContext(), prefix + " Exported", Toast.LENGTH_SHORT).show();
            openFile(file, "text/csv");
        } catch (Exception e) { showExportError(e); }
    }

    private void exportCsvInventory(String prefix, List<InventoryReportModel> list) {
        if (list.isEmpty()) { showNoData(); return; }
        try {
            File file = csvFile(prefix);
            FileWriter w = new FileWriter(file);
            writeCsvRow(w, INVENTORY_HEADERS);
            for (InventoryReportModel m : list)
                writeCsvRow(w, new String[]{m.itemName, m.category,
                        fmt(m.stockIn), fmt(m.stockOut), fmt(m.currentStock), m.unit, fmt(m.stockValue)});
            w.flush(); w.close();
            Toast.makeText(requireContext(), prefix + " Exported", Toast.LENGTH_SHORT).show();
            openFile(file, "text/csv");
        } catch (Exception e) { showExportError(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PDF EXPORT
    // ─────────────────────────────────────────────────────────────────────────

    private <T> void exportPdf(String title, List<T> dataList, String[] hdrs, String prefix) {
        if (dataList.isEmpty()) { showNoData(); return; }
        if (getContext() == null) return;

        FirebaseRefs.currentUserDoc().get()
                .addOnSuccessListener(snap -> {
                    String dairyName = firstString(snap, "dairyName", "businessName");
                    if (dairyName.isEmpty()) dairyName = "Milk Dairy";
                    generatePdf(title, dataList, hdrs, prefix, dairyName);
                })
                .addOnFailureListener(e -> generatePdf(title, dataList, hdrs, prefix, "Milk Dairy"));
    }

    private <T> void generatePdf(String title, List<T> dataList, String[] hdrs, String prefix, String dairyName) {
        try {
            PdfDocument pdf    = new PdfDocument();
            Paint paint        = new Paint(); paint.setTextSize(9f);
            Paint bold         = new Paint(); bold.setTextSize(13f); bold.setFakeBoldText(true);
            Paint subPaint     = new Paint(); subPaint.setTextSize(10f); subPaint.setColor(android.graphics.Color.DKGRAY);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(842, 1190, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int y = 50;
            canvas.drawText(dairyName, 40, y, bold);         y += 22;
            canvas.drawText(title, 40, y, bold);              y += 18;
            canvas.drawText("Period: " + getFromDate() + " → " + getToDate(), 40, y, subPaint); y += 15;
            String genDate = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(new Date());
            canvas.drawText("Generated: " + genDate, 40, y, subPaint); y += 25;

            Paint line = new Paint(); line.setColor(android.graphics.Color.parseColor("#009688")); line.setStrokeWidth(1.5f);
            canvas.drawLine(40, y, 802, y, line); y += 14;

            int colW = Math.max(75, 762 / Math.max(hdrs.length, 1));
            int x    = 40;
            for (String h : hdrs) {
                bold.setColor(android.graphics.Color.parseColor("#009688"));
                canvas.drawText(clip(h, 12), x, y, bold);
                x += colW;
            }
            y += 4;
            canvas.drawLine(40, y, 802, y, line); y += 14;
            bold.setColor(android.graphics.Color.BLACK);

            for (T item : dataList) {
                if (y > 1140) {
                    pdf.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(842, 1190, pdf.getPages().size() + 1).create();
                    page     = pdf.startPage(pageInfo);
                    canvas   = page.getCanvas();
                    y        = 50;
                }
                String[] row = rowFromModel(item, hdrs);
                x = 40;
                paint.setColor(android.graphics.Color.parseColor("#111827"));
                for (String cell : row) {
                    canvas.drawText(clip(cell, 13), x, y, paint);
                    x += colW;
                }
                y += 16;
                Paint sep = new Paint(); sep.setColor(android.graphics.Color.parseColor("#E5E7EB")); sep.setStrokeWidth(0.7f);
                canvas.drawLine(40, y - 2, 802, y - 2, sep);
            }

            pdf.finishPage(page);
            File file = csvFile(prefix + "_pdf").getCanonicalFile();
            File pdfFile = new File(requireContext().getExternalFilesDir(null), prefix + "_" + todayDate() + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(requireContext(), "PDF Exported: " + pdfFile.getName(), Toast.LENGTH_SHORT).show();
            openFile(pdfFile, "application/pdf");
        } catch (Exception e) {
            showExportError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> String[] rowFromModel(T item, String[] hdrs) {
        if (item instanceof MilkEntry) {
            MilkEntry e = (MilkEntry) item;
            return new String[]{e.getShift(), e.getFarmerName(), e.getMobile(), fmt(e.getLiters()), fmt(e.getFat()), fmt(e.getSnf()), fmt(e.getRate()), fmt(e.getAmount())};
        } else if (item instanceof YieldReportModel) {
            YieldReportModel y = (YieldReportModel) item;
            return new String[]{y.farmerName, fmt(y.cowLiters), fmt(y.buffaloLiters), fmt(y.mixLiters), fmt(y.cowAmount), fmt(y.buffaloAmount), fmt(y.totalAmount)};
        } else if (item instanceof BillingReportModel) {
            BillingReportModel b = (BillingReportModel) item;
            return new String[]{b.farmerCode, b.farmerName, fmt(b.milkAmount), fmt(b.bonus), fmt(b.deduction), fmt(b.netPayable), b.status};
        } else if (item instanceof InventoryReportModel) {
            InventoryReportModel m = (InventoryReportModel) item;
            return new String[]{m.itemName, m.category, fmt(m.stockIn), fmt(m.stockOut), fmt(m.currentStock), m.unit, fmt(m.stockValue)};
        }
        return new String[hdrs.length];
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATE PICKERS
    // ─────────────────────────────────────────────────────────────────────────

    private void openDatePicker() {
        new DatePickerDialog(requireContext(), (v, y, m, d) -> {
            calendar.set(y, m, d);
            updateDateText();
            refreshData();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openFromDatePicker() {
        new DatePickerDialog(requireContext(), (v, y, m, d) -> {
            fromCalendar.set(y, m, d);
            String fromStr = sdf.format(fromCalendar.getTime());
            String toStr   = sdf.format(toCalendar.getTime());
            try {
                if (sdf.parse(fromStr).after(sdf.parse(toStr))) {
                    Toast.makeText(requireContext(), "From date cannot be after To date", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException ignored) {}
            tvFromDate.setText(fromStr);
            refreshData();
        }, fromCalendar.get(Calendar.YEAR), fromCalendar.get(Calendar.MONTH), fromCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openToDatePicker() {
        new DatePickerDialog(requireContext(), (v, y, m, d) -> {
            toCalendar.set(y, m, d);
            String toStr   = sdf.format(toCalendar.getTime());
            String fromStr = sdf.format(fromCalendar.getTime());
            try {
                if (sdf.parse(fromStr).after(sdf.parse(toStr))) {
                    Toast.makeText(requireContext(), "To date cannot be before From date", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException ignored) {}
            tvToDate.setText(toStr);
            refreshData();
        }, toCalendar.get(Calendar.YEAR), toCalendar.get(Calendar.MONTH), toCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean validateDateRange(String from, String to) {
        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid Date Range", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            if (sdf.parse(from).after(sdf.parse(to))) {
                Toast.makeText(requireContext(), "From date is after To date", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception ignored) {}
        return true;
    }

    private boolean isDateInRange(String date, String from, String to) {
        try {
            long d = parseDate(date), f = parseDate(from), t = parseDate(to);
            return d >= f && d <= t;
        } catch (Exception e) { return false; }
    }

    private boolean dateRangesOverlap(String s1, String e1, String s2, String e2) {
        try {
            String e1safe = (e1 == null || e1.isEmpty()) ? s1 : e1;
            long a = parseDate(s1), b = parseDate(e1safe), c = parseDate(s2), d = parseDate(e2);
            return a <= d && c <= b;
        } catch (Exception e) { return false; }
    }

    private long parseDate(String date) throws ParseException {
        return sdf.parse(normalizeDate(date)).getTime();
    }

    private String normalizeDate(String date) {
        if (date == null || date.trim().isEmpty()) return "";
        date = date.trim();
        for (String fmt : new String[]{"dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd"}) {
            try { return sdf.format(new SimpleDateFormat(fmt, Locale.US).parse(date)); } catch (Exception ignored) {}
        }
        return date.replace("/", "-");
    }

    private String getTxnDate(DocumentSnapshot doc) {
        String date = firstString(doc, "date", "transactionDate", "createdDate");
        if (!date.isEmpty()) return date;
        Timestamp ts = doc.getTimestamp("createdAt");
        return ts != null ? sdf.format(ts.toDate()) : "";
    }

    private double getMilkAmount(DocumentSnapshot doc) {
        double a = firstDouble(doc, "amount", "totalAmount", "total", "grossAmount");
        if (a > 0) return a;
        return firstDouble(doc, "liters", "quantity", "milkLiter") * firstDouble(doc, "rate", "milkRate");
    }

    private String firstString(DocumentSnapshot doc, String... keys) {
        for (String k : keys) {
            Object v = doc.get(k);
            if (v != null && !v.toString().trim().isEmpty()) return v.toString().trim();
        }
        return "";
    }

    private double firstDouble(DocumentSnapshot doc, String... keys) {
        for (String k : keys) {
            Object v = doc.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) try { return Double.parseDouble(v.toString().replace("₹","").trim()); } catch (Exception ignored) {}
        }
        return 0;
    }

    private boolean isCow(String v)     { return safe(v).toLowerCase(Locale.US).contains("cow"); }
    private boolean isBuffalo(String v) { return safe(v).toLowerCase(Locale.US).contains("buffalo"); }

    private boolean isStockIn(String type) {
        String v = safe(type).toUpperCase(Locale.US);
        return v.equals("STOCK_IN") || v.equals("IN") || v.contains("PURCHASE");
    }

    private boolean isFarmerStockOut(DocumentSnapshot doc) {
        String type         = firstString(doc, "type", "transactionType").toUpperCase(Locale.US);
        String stockOutType = firstString(doc, "stockOutType", "saleType").toUpperCase(Locale.US);
        String farmerId     = firstString(doc, "farmerId", "farmerUid");
        boolean out    = type.equals("STOCK_OUT") || type.equals("OUT") || type.equals("FARMER_FEED");
        boolean farmer = stockOutType.equals("FARMER_FEED") || stockOutType.equals("FARMER_SALE") || !farmerId.isEmpty();
        return out && farmer;
    }

    private void writeCsvRow(FileWriter writer, String[] cells) throws Exception {
        for (int i = 0; i < cells.length; i++) {
            String v = cells[i] == null ? "" : cells[i];
            writer.append("\"").append(v.replace("\"","\"\"")).append("\"");
            if (i < cells.length - 1) writer.append(",");
        }
        writer.append("\n");
    }

    private File csvFile(String prefix) {
        return new File(requireContext().getExternalFilesDir(null), prefix + "_" + todayDate() + ".csv");
    }

    private String todayDate() {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
    }

    private void openFile(File file, String mime) {
        try {
            if (getContext() == null) return;
            Uri uri = FileProvider.getUriForFile(
                    requireContext(), requireContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app found to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private String clip(String v, int max) {
        if (v == null) return "";
        return v.length() > max ? v.substring(0, max) : v;
    }

    private String safe(String v)  { return v == null ? "" : v; }
    private String fmt(double v)   { return String.format(Locale.US, "%.2f", v); }

    private void showNetworkError() {
        if (getContext() != null)
            Toast.makeText(requireContext(), "Unable to Load Report — Check Connection", Toast.LENGTH_LONG).show();
    }

    private void showNoData() {
        Toast.makeText(requireContext(), "No Data Available for Selected Period", Toast.LENGTH_SHORT).show();
    }

    private void showExportError(Exception e) {
        Toast.makeText(requireContext(), "Export Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    // ─── Aggregation inner classes ────────────────────────────────────────────

    private static class YieldAgg {
        String farmerName = "";
        double cowLiters, buffaloLiters, mixLiters;
        double cowAmount, buffaloAmount, mixAmount;
    }

    private static class InventoryAgg {
        String itemName = "", category = "", unit = "";
        double stockIn, stockOut, currentStock, stockValue;
    }
}