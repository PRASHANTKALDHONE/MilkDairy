package com.prashant.milkdairy.ui.Rate_Chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.prashant.milkdairy.Model.RuleSlab;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * "Generate Rate Chart" bottom sheet.
 *
 * Actions:
 *  Preview  → shows scrollable FAT x SNF matrix in an AlertDialog
 *  Generate → calls state.generateChart(), notifies parent
 *  Save     → writes full chart + formula to Firestore, then dismisses
 */
public class GenerateChartBottomSheet extends BottomSheetDialogFragment {

    // ---- Listener ----
    public interface ChartActionListener {
        void onChartGenerated(RateChartState updatedState);
        void onChartSaved();
        RateChartState provideState();
    }

    private ChartActionListener listener;
    private RateChartState state;

    // ---- Views ----
    private TextView tvSelectedFormula, tvEstimatedEntries;
    private MaterialButton btnPreview, btnGenerate, btnSaveChart;
    private ImageView btnClose;

    private boolean chartGenerated = false;

    // =========================================================
    //  Factory
    // =========================================================

    public static GenerateChartBottomSheet newInstance(RateChartState state) {
        GenerateChartBottomSheet sheet = new GenerateChartBottomSheet();
        sheet.state = state;
        return sheet;
    }

    public void setListener(ChartActionListener listener) {
        this.listener = listener;
    }

    // =========================================================
    //  Lifecycle
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_generate_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (state == null) state = new RateChartState();

        initViews(view);
        bindState();
        setupClickListeners();
    }

    private void initViews(View root) {
        tvSelectedFormula  = root.findViewById(R.id.tvSelectedFormula);
        tvEstimatedEntries = root.findViewById(R.id.tvEstimatedEntries);
        btnPreview         = root.findViewById(R.id.btnPreview);
        btnGenerate        = root.findViewById(R.id.btnGenerate);
        btnSaveChart       = root.findViewById(R.id.btnSaveRateChart);
        btnClose           = root.findViewById(R.id.btnCloseGenerate);
    }

    private void bindState() {
        tvSelectedFormula.setText("Formula V3 — " + milkLabel());
        refreshEntryCount();
        if (!state.generatedChart.isEmpty()) chartGenerated = true;
    }

    private void refreshEntryCount() {
        if (!state.generatedChart.isEmpty()) {
            tvEstimatedEntries.setText(state.generatedChart.size() + " Rates");
        } else {
            int fat  = estimateSteps(state.fatMin, state.fatMax, RateChartState.FAT_STEP);
            int snf  = estimateSteps(state.snfMin, state.snfMax, RateChartState.SNF_STEP);
            tvEstimatedEntries.setText((fat * snf) + " Rates");
        }
    }

    private int estimateSteps(double min, double max, double step) {
        if (step <= 0 || min >= max) return 0;
        return (int) Math.round((max - min) / step) + 1;
    }

    // =========================================================
    //  Click Listeners
    // =========================================================

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        btnGenerate.setOnClickListener(v -> doGenerate());

        btnPreview.setOnClickListener(v -> {
            if (!chartGenerated && state.generatedChart.isEmpty()) doGenerate();
            if (!state.generatedChart.isEmpty()) showPreviewDialog();
        });

        btnSaveChart.setOnClickListener(v -> {
            if (state.generatedChart.isEmpty()) {
                showToast("Generate the chart first");
                return;
            }
            if (state.formulaDirty) {
                state.generateChart();
                refreshEntryCount();
                if (listener != null) listener.onChartGenerated(state);
            }
            saveChartToFirestore();
        });
    }

    // =========================================================
    //  Generate
    // =========================================================

    private void doGenerate() {
        if (!validateState()) return;

        btnGenerate.setEnabled(false);
        btnGenerate.setText("Generating...");

        state.generateChart();
        chartGenerated = true;
        refreshEntryCount();

        btnGenerate.setEnabled(true);
        btnGenerate.setText("Generate");

        if (listener != null) listener.onChartGenerated(state);
        showToast("Chart generated — " + state.generatedChart.size() + " rates");
    }

    private boolean validateState() {
        if (state.baseRate <= 0)       { showToast("Base rate not set — configure formula first"); return false; }
        if (state.fatMin >= state.fatMax) { showToast("Invalid FAT range — configure formula first"); return false; }
        if (state.snfMin >= state.snfMax) { showToast("Invalid SNF range — configure formula first"); return false; }
        if (state.fatRules.isEmpty())  { showToast("No FAT rules — configure formula first"); return false; }
        if (state.snfRules.isEmpty())  { showToast("No SNF rules — configure formula first"); return false; }
        return true;
    }

    // =========================================================
    //  Preview Dialog
    // =========================================================

    private void showPreviewDialog() {
        Context ctx = requireContext();

        TableLayout table = new TableLayout(ctx);
        table.setShrinkAllColumns(false);

        // Header row
        TableRow header = new TableRow(ctx);
        header.addView(createCell(ctx, "FAT \\ SNF", true));
        for (double snf : state.generatedSnfValues)
            header.addView(createCell(ctx, String.format(Locale.US, "%.2f", snf), true));
        table.addView(header);

        // Data rows
        for (double fat : state.generatedFatValues) {
            TableRow row = new TableRow(ctx);
            row.addView(createCell(ctx, String.format(Locale.US, "%.2f", fat), true));
            for (double snf : state.generatedSnfValues) {
                Double rate = state.generatedChart.get(state.chartKey(fat, snf));
                row.addView(createCell(ctx,
                        rate == null ? "--" : String.format(Locale.US, "%.2f", rate), false));
            }
            table.addView(row);
        }

        HorizontalScrollView hScroll = new HorizontalScrollView(ctx);
        hScroll.addView(table);

        ScrollView vScroll = new ScrollView(ctx);
        vScroll.addView(hScroll);

        int pad = dp(12);
        vScroll.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(ctx)
                .setTitle(milkLabel() + " — Rate Chart Preview")
                .setView(vScroll)
                .setPositiveButton("Close", null)
                .show();
    }

    private TextView createCell(Context ctx, String text, boolean isHeader) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setTextSize(11f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.MONOSPACE, isHeader ? Typeface.BOLD : Typeface.NORMAL);

        if (isHeader) {
            tv.setTextColor(Color.WHITE);
            if ("buffalo".equals(state.milkType))
                tv.setBackgroundResource(R.drawable.bg_rate_header_purple);
            else if ("mix".equals(state.milkType))
                tv.setBackgroundResource(R.drawable.bg_rate_header_amber);
            else
                tv.setBackgroundResource(R.drawable.bg_rate_header);
        } else {
            tv.setTextColor(Color.parseColor("#1A1A2E"));
            tv.setBackgroundResource(R.drawable.bg_rate_cell);
        }

        TableRow.LayoutParams p = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        p.setMargins(2, 2, 2, 2);
        tv.setLayoutParams(p);
        return tv;
    }

    // =========================================================
    //  Save to Firestore
    // =========================================================

    private void saveChartToFirestore() {
        btnSaveChart.setEnabled(false);
        btnSaveChart.setText("Saving...");

        ArrayList<Map<String, Object>> fatMap = new ArrayList<>();
        for (RuleSlab r : state.fatRules) fatMap.add(r.toMap());

        ArrayList<Map<String, Object>> snfMap = new ArrayList<>();
        for (RuleSlab r : state.snfRules) snfMap.add(r.toMap());

        Map<String, Object> doc = new HashMap<>();
        doc.put("milkType",       state.milkType);
        doc.put("baseRate",       state.baseRate);
        doc.put("fatMin",         state.fatMin);
        doc.put("fatMax",         state.fatMax);
        doc.put("snfMin",         state.snfMin);
        doc.put("snfMax",         state.snfMax);
        doc.put("fatStep",        RateChartState.FAT_STEP);
        doc.put("snfStep",        RateChartState.SNF_STEP);
        doc.put("fatRules",       fatMap);
        doc.put("snfRules",       snfMap);
        doc.put("generatedChart", state.generatedChart);
        doc.put("totalRates",     state.generatedChart.size());
        doc.put("generatedAt",    FieldValue.serverTimestamp());
        doc.put("updatedAt",      FieldValue.serverTimestamp());

        FirebaseRefs.rateCharts().document(state.milkType)
                .set(doc)
                .addOnSuccessListener(unused -> {
                    btnSaveChart.setEnabled(true);
                    btnSaveChart.setText("Save Chart");
                    state.formulaDirty = false;
                    if (listener != null) listener.onChartSaved();
                    showToast(milkLabel() + " chart saved ✓");
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    btnSaveChart.setEnabled(true);
                    btnSaveChart.setText("Save Chart");
                    showToast("Save failed: " + e.getMessage());
                });
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private String milkLabel() {
        switch (state.milkType) {
            case "buffalo": return "Buffalo Milk";
            case "mix":     return "Mix Milk";
            default:        return "Cow Milk";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}