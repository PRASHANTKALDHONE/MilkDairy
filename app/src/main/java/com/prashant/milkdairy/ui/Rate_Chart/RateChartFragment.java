package com.prashant.milkdairy.ui.Rate_Chart;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.prashant.milkdairy.Adapter.RateEntryAdapter;
import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.Model.RuleSlab;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main Rate Chart screen.
 *
 * Holds the shared RateChartState (formula + generated chart) for the
 * currently selected milk type, and launches the various bottom sheets:
 *  - ConfigureFormulaBottomSheet
 *  - GenerateChartBottomSheet
 *  - FindRateBottomSheet
 *  - ImportExportBottomSheet
 *
 * Also hosts the "Latest Generated Chart" preview list and the
 * "Rate Chart Overview" footer stats.
 */
public class RateChartFragment extends Fragment
        implements ConfigureFormulaBottomSheet.FormulaSaveListener,
        GenerateChartBottomSheet.ChartActionListener {

    // ---- Milk type tabs ----
    private LinearLayout layoutCow, layoutBuffalo, layoutMix;
    private TextView btCow, btBuffalo, btMix;

    // ---- Formula summary card ----
    private TextView tvSummaryBaseRate, tvSummaryFatRange, tvSummarySnfRange;
    private TextView tvLastUpdated;
    private TextView btnViewDetails;

    // ---- Quick action cards ----
    private CardView btnConfigureFormula, btnGenerateChart, btnFindRate, btnImportExport;

    // ---- Latest chart list ----
    private RecyclerView recyclerLatestChart;
    private TextView tvTotalEntriesLabel;
    private RateEntryAdapter latestAdapter;

    // ---- Overview footer ----
    private TextView tvTotalRates, tvMinRate, tvMaxRate, tvGeneratedOn;

    private TextView btnViewAllRates;

    private String selectedMilkType = "cow";

    private ListenerRegistration rateChartListener;

    // Shared state for the currently selected milk type
    private RateChartState state = new RateChartState();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_rate_chart, container, false);

        initViews(root);
        setupClickListeners();
        selectMilkType("cow");

        return root;
    }

    private void initViews(View root) {
        layoutCow = root.findViewById(R.id.layoutCow);
        layoutBuffalo = root.findViewById(R.id.layoutBuffalo);
        layoutMix = root.findViewById(R.id.layoutMix);

        btCow = root.findViewById(R.id.btCow);
        btBuffalo = root.findViewById(R.id.btBuffalo);
        btMix = root.findViewById(R.id.btMix);

        tvSummaryBaseRate = root.findViewById(R.id.tvSummaryBaseRate);
        tvSummaryFatRange = root.findViewById(R.id.tvSummaryFatRange);
        tvSummarySnfRange = root.findViewById(R.id.tvSummarySnfRange);
        tvLastUpdated = root.findViewById(R.id.tvLastUpdated);
        btnViewDetails = root.findViewById(R.id.btnViewDetails);

        btnConfigureFormula = root.findViewById(R.id.btnConfigureFormula);
        btnGenerateChart = root.findViewById(R.id.btnGenerateChart);
        btnFindRate = root.findViewById(R.id.btnFindRate);
        btnImportExport = root.findViewById(R.id.btnImportExport);

        recyclerLatestChart = root.findViewById(R.id.recyclerLatestChart);
        tvTotalEntriesLabel = root.findViewById(R.id.tvTotalEntriesLabel);

        tvTotalRates = root.findViewById(R.id.tvTotalRates);
        tvMinRate = root.findViewById(R.id.tvMinRate);
        tvMaxRate = root.findViewById(R.id.tvMaxRate);
        tvGeneratedOn = root.findViewById(R.id.tvGeneratedOn);

        btnViewAllRates = root.findViewById(R.id.btnViewAllRates);

        recyclerLatestChart.setLayoutManager(new LinearLayoutManager(requireContext()));
        latestAdapter = new RateEntryAdapter(new ArrayList<>(), entry ->
                openEditRate(entry));
        recyclerLatestChart.setAdapter(latestAdapter);
    }

    private void setupClickListeners() {
        layoutCow.setOnClickListener(v -> selectMilkType("cow"));
        layoutBuffalo.setOnClickListener(v -> selectMilkType("buffalo"));
        layoutMix.setOnClickListener(v -> selectMilkType("mix"));


        btnConfigureFormula.setOnClickListener(v -> openConfigureFormula());
        btnViewDetails.setOnClickListener(v -> openConfigureFormula());

        btnGenerateChart.setOnClickListener(v -> openGenerateChart());

        btnFindRate.setOnClickListener(v -> openFindRate());

        btnImportExport.setOnClickListener(v -> openImportExport());

        btnViewAllRates.setOnClickListener(v -> openAllRates());
    }

    // =========================================================
    //  MILK TYPE SELECTION
    // =========================================================

    private void selectMilkType(String milkType) {
        selectedMilkType = milkType;
        resetMilkTabs();

        if ("buffalo".equals(milkType)) {
            layoutBuffalo.setBackgroundResource(R.drawable.bg_milk_selected_green);
            btBuffalo.setTextColor(Color.WHITE);
            btBuffalo.setTypeface(null, Typeface.BOLD);
            loadDefaults(42.00, 5.0, 10.0, 8.0, 12.0,
                    "5.0", "10.0", "0.60",
                    "8.0", "12.0", "0.40");
        } else if ("mix".equals(milkType)) {
            layoutMix.setBackgroundResource(R.drawable.bg_milk_selected_green);
            btMix.setTextColor(Color.WHITE);
            btMix.setTypeface(null, Typeface.BOLD);
            loadDefaults(38.00, 3.0, 9.0, 7.0, 12.0,
                    "3.0", "9.0", "0.50",
                    "7.0", "12.0", "0.35");
        } else {
            layoutCow.setBackgroundResource(R.drawable.bg_milk_selected_green);
            btCow.setTextColor(Color.WHITE);
            btCow.setTypeface(null, Typeface.BOLD);
            loadDefaults(34.50, 3.0, 8.0, 7.5, 10.5,
                    "3.0", "4.0", "0.10",
                    "7.5", "8.5", "0.10");
        }

        updateSummaryCard();
        listenSavedChart();
    }

    private void resetMilkTabs() {
        layoutCow.setBackgroundResource(R.drawable.bg_milk_unselected);
        layoutBuffalo.setBackgroundResource(R.drawable.bg_milk_unselected);
        layoutMix.setBackgroundResource(R.drawable.bg_milk_unselected);

        btCow.setTextColor(Color.parseColor("#8E9AAF"));
        btBuffalo.setTextColor(Color.parseColor("#8E9AAF"));
        btMix.setTextColor(Color.parseColor("#8E9AAF"));

        btCow.setTypeface(null, Typeface.NORMAL);
        btBuffalo.setTypeface(null, Typeface.NORMAL);
        btMix.setTypeface(null, Typeface.NORMAL);
    }

    private void loadDefaults(double base, double fatMin, double fatMax,
                              double snfMin, double snfMax,
                              String fatFrom, String fatTo, String fatDiff,
                              String snfFrom, String snfTo, String snfDiff) {
        state = new RateChartState();
        state.milkType = selectedMilkType;
        state.baseRate = base;
        state.fatMin = fatMin;
        state.fatMax = fatMax;
        state.snfMin = snfMin;
        state.snfMax = snfMax;

        state.fatRules.clear();
        state.fatRules.add(new RuleSlab(parseD(fatFrom), parseD(fatTo), parseD(fatDiff)));

        state.snfRules.clear();
        state.snfRules.add(new RuleSlab(parseD(snfFrom), parseD(snfTo), parseD(snfDiff)));

        state.generatedChart.clear();
        latestAdapter.updateData(new ArrayList<>());
        resetSummary();
    }

    private double parseD(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    //  FIRESTORE: load saved chart / formula for selected milk type
    // =========================================================

    private void listenSavedChart() {
        if (rateChartListener != null) {
            rateChartListener.remove();
        }

        rateChartListener = FirebaseRefs.rateCharts()
                .document(selectedMilkType)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        showToast("Load failed: " + error.getMessage());
                        return;
                    }

                    if (doc == null || !doc.exists()) {
                        return;
                    }

                    Double base = doc.getDouble("baseRate");
                    Double fatMin = doc.getDouble("fatMin");
                    Double fatMax = doc.getDouble("fatMax");
                    Double snfMin = doc.getDouble("snfMin");
                    Double snfMax = doc.getDouble("snfMax");

                    if (base != null) state.baseRate = base;
                    if (fatMin != null) state.fatMin = fatMin;
                    if (fatMax != null) state.fatMax = fatMax;
                    if (snfMin != null) state.snfMin = snfMin;
                    if (snfMax != null) state.snfMax = snfMax;

                    restoreRules(doc.get("fatRules"), true);
                    restoreRules(doc.get("snfRules"), false);
                    restoreGeneratedChart(doc.get("generatedChart"));

                    updateSummaryCard();
                    refreshLatestChartList();
                    updateOverviewFooter();

                    Timestamp updatedAt = doc.getTimestamp("updatedAt");
                    if (updatedAt != null) {
                        tvLastUpdated.setText("Last Updated: " +
                                new SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                                        .format(updatedAt.toDate()));
                    } else {
                        tvLastUpdated.setText("Last Updated: --");
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void restoreRules(Object rawRules, boolean isFat) {
        if (!(rawRules instanceof ArrayList)) return;

        ArrayList<Map<String, Object>> rules = (ArrayList<Map<String, Object>>) rawRules;
        List<RuleSlab> target = isFat ? state.fatRules : state.snfRules;
        target.clear();

        int limit = Math.min(rules.size(), RateChartState.MAX_RULE_ROWS);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> rule = rules.get(i);
            double from = toDouble(rule.get("from"));
            double to = toDouble(rule.get("to"));
            double diff = toDouble(rule.get("diff"));
            target.add(new RuleSlab(from, to, diff));
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreGeneratedChart(Object rawChart) {
        state.generatedChart.clear();

        if (!(rawChart instanceof Map)) return;

        Map<String, Object> chart = (Map<String, Object>) rawChart;

        for (Map.Entry<String, Object> entry : chart.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number) {
                state.generatedChart.put(entry.getKey(), ((Number) value).doubleValue());
            }
        }

        state.rebuildAxesFromChartKeys();
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    //  UI UPDATES
    // =========================================================

    private void updateSummaryCard() {
        tvSummaryBaseRate.setText(String.format(Locale.US, "%.2f", state.baseRate));
        tvSummaryFatRange.setText(String.format(Locale.US, "%.1f - %.1f", state.fatMin, state.fatMax));
        tvSummarySnfRange.setText(String.format(Locale.US, "%.1f - %.1f", state.snfMin, state.snfMax));
    }

    private void refreshLatestChartList() {
        List<RateEntry> entries = state.toSortedEntryList();
        // Show top 5 for the "Latest Generated Chart" preview
        List<RateEntry> preview = entries.size() > 5 ? entries.subList(0, 5) : entries;
        latestAdapter.updateData(new ArrayList<>(preview));
        tvTotalEntriesLabel.setText("Total Entries: " + entries.size());
    }

    private void updateOverviewFooter() {
        if (state.generatedChart.isEmpty()) {
            resetSummary();
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (double rate : state.generatedChart.values()) {
            min = Math.min(min, rate);
            max = Math.max(max, rate);
        }

        tvTotalRates.setText(String.valueOf(state.generatedChart.size()));
        tvMinRate.setText(String.format(Locale.US, "\u20B9 %.2f", min));
        tvMaxRate.setText(String.format(Locale.US, "\u20B9 %.2f", max));

        if (state.lastGeneratedAt != null) {
            tvGeneratedOn.setText(state.lastGeneratedAt);
        }
    }

    private void resetSummary() {
        tvTotalRates.setText("0");
        tvMinRate.setText("\u20B9 --");
        tvMaxRate.setText("\u20B9 --");
        tvGeneratedOn.setText("--");
        tvTotalEntriesLabel.setText("Total Entries: 0");
        latestAdapter.updateData(new ArrayList<>());
    }

    // =========================================================
    //  BOTTOM SHEETS
    // =========================================================

    private void openConfigureFormula() {
        ConfigureFormulaBottomSheet sheet = ConfigureFormulaBottomSheet.newInstance(state);
        sheet.setListener(this);
        sheet.show(getChildFragmentManager(), "ConfigureFormula");
    }

    @Override
    public void onFormulaSaved(RateChartState updatedState) {
        // Copy updated formula values back into shared state, keep generated chart as-is
        state.baseRate = updatedState.baseRate;
        state.fatMin = updatedState.fatMin;
        state.fatMax = updatedState.fatMax;
        state.snfMin = updatedState.snfMin;
        state.snfMax = updatedState.snfMax;
        state.fatRules = updatedState.fatRules;
        state.snfRules = updatedState.snfRules;
        state.formulaDirty = true;

        updateSummaryCard();
        showToast("Formula updated");
    }

    private void openGenerateChart() {
        GenerateChartBottomSheet sheet = GenerateChartBottomSheet.newInstance(state);
        sheet.setListener(this);
        sheet.show(getChildFragmentManager(), "GenerateChart");
    }

    @Override
    public void onChartGenerated(RateChartState updatedState) {
        state.generatedChart = updatedState.generatedChart;
        state.generatedFatValues = updatedState.generatedFatValues;
        state.generatedSnfValues = updatedState.generatedSnfValues;
        state.lastGeneratedAt = updatedState.lastGeneratedAt;
        state.formulaDirty = false;

        refreshLatestChartList();
        updateOverviewFooter();
        showToast("Rate chart generated");
    }

    @Override
    public void onChartSaved() {
        tvLastUpdated.setText("Last Updated: " + currentDateTime());
        showToast(selectedMilkType + " rate chart saved");
    }

    @Override
    public RateChartState provideState() {
        return state;
    }

    private void openFindRate() {
        FindRateBottomSheet sheet = FindRateBottomSheet.newInstance(state);
        sheet.show(getChildFragmentManager(), "FindRate");
    }

    private void openImportExport() {
        ImportExportBottomSheet sheet = ImportExportBottomSheet.newInstance(state);
        sheet.show(getChildFragmentManager(), "ImportExport");
    }

    private void openEditRate(RateEntry entry) {
        EditRateBottomSheet sheet = EditRateBottomSheet.newInstance(state, entry);
        sheet.setListener(updated -> {
            state.generatedChart.put(
                    String.format(Locale.US, "%.2f_%.2f", updated.fat, updated.snf),
                    updated.rate);
            refreshLatestChartList();
            updateOverviewFooter();
            saveGeneratedChartSilently();
        });
        sheet.show(getChildFragmentManager(), "EditRate");
    }

    private void saveGeneratedChartSilently() {
        Map<String, Object> map = new HashMap<>();
        map.put("generatedChart", state.generatedChart);
        map.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseRefs.rateCharts().document(selectedMilkType)
                .set(map, com.google.firebase.firestore.SetOptions.merge());
    }

    private void openAllRates() {
        Intent intent = new Intent(requireContext(), AllRatesActivity.class);
        intent.putExtra(AllRatesActivity.EXTRA_MILK_TYPE, selectedMilkType);
        startActivity(intent);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private String currentDateTime() {
        return new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.US).format(new java.util.Date());
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rateChartListener != null) {
            rateChartListener.remove();
            rateChartListener = null;
        }
    }
}