package com.prashant.milkdairy.ui.Rate_Chart;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FieldValue;
import com.prashant.milkdairy.Model.RuleSlab;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Configure Formula" bottom sheet.
 *
 * Lets the user edit:
 *  - Base Rate
 *  - FAT Range (min/max)
 *  - FAT Difference rules (up to 2, using item_fat_row.xml)
 *  - SNF Range (min/max)
 *  - SNF Difference rules (up to 2, using item_snf_row.xml)
 *
 * On "Save Formula":
 *  - validates inputs
 *  - writes the formula fields to Firestore (RateCharts/{milkType})
 *  - notifies the listener so the parent fragment can update its
 *    in-memory RateChartState and summary card.
 */
public class ConfigureFormulaBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_STATE = "arg_state";

    public interface FormulaSaveListener {
        void onFormulaSaved(RateChartState updatedState);
    }

    private FormulaSaveListener listener;
    private RateChartState state;

    private EditText etBaseRate, etFatMin, etFatMax, etSnfMin, etSnfMax;
    private LinearLayout fatContainer, snfContainer;
    private TextView tvFatRuleCount, tvSnfRuleCount;
    private TextView btnAddFatDiff, btnAddSnfDiff;
    private com.google.android.material.button.MaterialButton btnSaveFormula;
    private ImageView btnClose;

    private boolean restoring = false;

    public static ConfigureFormulaBottomSheet newInstance(RateChartState state) {
        ConfigureFormulaBottomSheet sheet = new ConfigureFormulaBottomSheet();
        sheet.state = cloneState(state);
        return sheet;
    }

    public void setListener(FormulaSaveListener listener) {
        this.listener = listener;
    }

    /** Deep-ish copy so edits in this sheet don't mutate parent state until saved. */
    private static RateChartState cloneState(RateChartState src) {
        RateChartState copy = new RateChartState();
        copy.milkType = src.milkType;
        copy.baseRate = src.baseRate;
        copy.fatMin = src.fatMin;
        copy.fatMax = src.fatMax;
        copy.snfMin = src.snfMin;
        copy.snfMax = src.snfMax;

        for (RuleSlab r : src.fatRules) {
            copy.fatRules.add(new RuleSlab(r.from, r.to, r.diff));
        }
        for (RuleSlab r : src.snfRules) {
            copy.snfRules.add(new RuleSlab(r.from, r.to, r.diff));
        }

        // Keep reference to generated chart (not edited here)
        copy.generatedChart = src.generatedChart;
        copy.generatedFatValues = src.generatedFatValues;
        copy.generatedSnfValues = src.generatedSnfValues;
        copy.lastGeneratedAt = src.lastGeneratedAt;
        copy.formulaDirty = src.formulaDirty;

        return copy;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_configure_formula, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (state == null) {
            state = new RateChartState();
        }

        initViews(view);
        bindStateToFields();
        setupWatchers();
        setupClickListeners();
    }

    private void initViews(View root) {
        etBaseRate = root.findViewById(R.id.etBaseRate);
        etFatMin = root.findViewById(R.id.etFatMin);
        etFatMax = root.findViewById(R.id.etFatMax);
        etSnfMin = root.findViewById(R.id.etSnfMin);
        etSnfMax = root.findViewById(R.id.etSnfMax);

        fatContainer = root.findViewById(R.id.fatContainer);
        snfContainer = root.findViewById(R.id.snfContainer);
        tvFatRuleCount = root.findViewById(R.id.tvFatRuleCount);
        tvSnfRuleCount = root.findViewById(R.id.tvSnfRuleCount);

        btnAddFatDiff = root.findViewById(R.id.btnAddFatDiff);
        btnAddSnfDiff = root.findViewById(R.id.btnAddSnfDiff);

        btnSaveFormula = root.findViewById(R.id.btnSaveFormula);
        btnClose = root.findViewById(R.id.btnCloseConfigure);
    }

    private void bindStateToFields() {
        restoring = true;

        etBaseRate.setText(formatPlain(state.baseRate));
        etFatMin.setText(formatPlain(state.fatMin));
        etFatMax.setText(formatPlain(state.fatMax));
        etSnfMin.setText(formatPlain(state.snfMin));
        etSnfMax.setText(formatPlain(state.snfMax));

        fatContainer.removeAllViews();
        for (RuleSlab rule : state.fatRules) {
            addFatRow(formatPlain(rule.from), formatPlain(rule.to), formatPlain(rule.diff));
        }
        if (fatContainer.getChildCount() == 0) {
            addFatRow("3.0", "4.0", "0.10");
        }

        snfContainer.removeAllViews();
        for (RuleSlab rule : state.snfRules) {
            addSnfRow(formatPlain(rule.from), formatPlain(rule.to), formatPlain(rule.diff));
        }
        if (snfContainer.getChildCount() == 0) {
            addSnfRow("7.5", "8.5", "0.10");
        }

        updateRuleCounts();
        restoring = false;
    }

    private String formatPlain(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    // =========================================================
    //  ROW MANAGEMENT (reusing item_fat_row.xml / item_snf_row.xml)
    // =========================================================

    private void addFatRow(String from, String to, String diff) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_fat_row, fatContainer, false);

        EditText etFrom = row.findViewById(R.id.etFatFrom);
        EditText etTo = row.findViewById(R.id.etFatTo);
        EditText etDiff = row.findViewById(R.id.etFatDiff);
        TextView tvDisplay = row.findViewById(R.id.tvFatDiffDisplay);
        ImageView btnDelete = row.findViewById(R.id.btnDeleteFat);

        etFrom.setText(from);
        etTo.setText(to);
        etDiff.setText(diff);
        tvDisplay.setText("+" + diff);

        etDiff.addTextChangedListener(new SimpleDisplayWatcher(tvDisplay));

        btnDelete.setOnClickListener(v -> {
            fatContainer.removeView(row);
            updateRuleCounts();
        });

        fatContainer.addView(row);
        updateRuleCounts();
    }

    private void addSnfRow(String from, String to, String diff) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_snf_row, snfContainer, false);

        EditText etFrom = row.findViewById(R.id.etSnfFrom);
        EditText etTo = row.findViewById(R.id.etSnfTo);
        EditText etDiff = row.findViewById(R.id.etSnfDiff);
        TextView tvDisplay = row.findViewById(R.id.tvSnfDiffDisplay);
        ImageView btnDelete = row.findViewById(R.id.btnDeleteSnf);

        etFrom.setText(from);
        etTo.setText(to);
        etDiff.setText(diff);
        tvDisplay.setText("+" + diff);

        etDiff.addTextChangedListener(new SimpleDisplayWatcher(tvDisplay));

        btnDelete.setOnClickListener(v -> {
            snfContainer.removeView(row);
            updateRuleCounts();
        });

        snfContainer.addView(row);
        updateRuleCounts();
    }

    /** Updates the "+0.10" style display label live as the diff EditText changes. */
    private class SimpleDisplayWatcher implements TextWatcher {
        private final TextView display;

        SimpleDisplayWatcher(TextView display) {
            this.display = display;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            display.setText("+" + s.toString());
        }
    }

    /**
     * Updates "X Rules" labels and disables the Add buttons once
     * RateChartState.MAX_RULE_ROWS (2) rows are present, per the
     * provided Java logic.
     */
    private void updateRuleCounts() {
        int fatCount = fatContainer.getChildCount();
        int snfCount = snfContainer.getChildCount();

        tvFatRuleCount.setText(fatCount + " Rule" + (fatCount == 1 ? "" : "s"));
        tvSnfRuleCount.setText(snfCount + " Rule" + (snfCount == 1 ? "" : "s"));

        boolean fatCanAdd = fatCount < RateChartState.MAX_RULE_ROWS;
        boolean snfCanAdd = snfCount < RateChartState.MAX_RULE_ROWS;

        btnAddFatDiff.setEnabled(fatCanAdd);
        btnAddFatDiff.setAlpha(fatCanAdd ? 1f : 0.45f);

        btnAddSnfDiff.setEnabled(snfCanAdd);
        btnAddSnfDiff.setAlpha(snfCanAdd ? 1f : 0.45f);
    }

    private void setupWatchers() {
        // Live-update could be added here if a "Formula Preview" calculation
        // box needs to react in real time. Currently the preview text is static.
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        btnAddFatDiff.setOnClickListener(v -> {
            if (fatContainer.getChildCount() >= RateChartState.MAX_RULE_ROWS) {
                updateRuleCounts();
                showToast("Only two FAT rules allowed");
                return;
            }
            addFatRow("4.0", "5.0", "0.15");
        });

        btnAddSnfDiff.setOnClickListener(v -> {
            if (snfContainer.getChildCount() >= RateChartState.MAX_RULE_ROWS) {
                updateRuleCounts();
                showToast("Only two SNF rules allowed");
                return;
            }
            addSnfRow("8.5", "10.5", "0.15");
        });

        btnSaveFormula.setOnClickListener(v -> onSaveFormula());
    }

    // =========================================================
    //  VALIDATION + SAVE
    // =========================================================

    private void onSaveFormula() {
        double base = value(etBaseRate);
        double fatMin = value(etFatMin);
        double fatMax = value(etFatMax);
        double snfMin = value(etSnfMin);
        double snfMax = value(etSnfMax);

        if (base <= 0) {
            etBaseRate.setError("Base rate must be > 0");
            etBaseRate.requestFocus();
            return;
        }

        if (fatMin >= fatMax) {
            etFatMax.setError("Max FAT must be > Min FAT");
            etFatMax.requestFocus();
            return;
        }

        if (snfMin >= snfMax) {
            etSnfMax.setError("Max SNF must be > Min SNF");
            etSnfMax.requestFocus();
            return;
        }

        List<RuleSlab> fatRules = readRules(fatContainer, true);
        List<RuleSlab> snfRules = readRules(snfContainer, false);

        if (fatRules.isEmpty()) {
            showToast("Add at least one FAT difference rule");
            return;
        }

        if (snfRules.isEmpty()) {
            showToast("Add at least one SNF difference rule");
            return;
        }

        for (RuleSlab rule : fatRules) {
            if (rule.diff <= 0 || rule.from >= rule.to) {
                showToast("Check FAT rule values (From < To, Rate > 0)");
                return;
            }
        }

        for (RuleSlab rule : snfRules) {
            if (rule.diff <= 0 || rule.from >= rule.to) {
                showToast("Check SNF rule values (From < To, Rate > 0)");
                return;
            }
        }

        state.baseRate = base;
        state.fatMin = fatMin;
        state.fatMax = fatMax;
        state.snfMin = snfMin;
        state.snfMax = snfMax;
        state.fatRules = fatRules;
        state.snfRules = snfRules;
        state.formulaDirty = true;

        saveFormulaToFirestore();
    }

    private void saveFormulaToFirestore() {
        Map<String, Object> map = new HashMap<>();
        map.put("milkType", state.milkType);
        map.put("baseRate", state.baseRate);
        map.put("fatMin", state.fatMin);
        map.put("fatMax", state.fatMax);
        map.put("snfMin", state.snfMin);
        map.put("snfMax", state.snfMax);

        ArrayList<Map<String, Object>> fatRulesMap = new ArrayList<>();
        for (RuleSlab r : state.fatRules) fatRulesMap.add(r.toMap());

        ArrayList<Map<String, Object>> snfRulesMap = new ArrayList<>();
        for (RuleSlab r : state.snfRules) snfRulesMap.add(r.toMap());

        map.put("fatRules", fatRulesMap);
        map.put("snfRules", snfRulesMap);
        map.put("updatedAt", FieldValue.serverTimestamp());

        btnSaveFormula.setEnabled(false);
        btnSaveFormula.setText("Saving...");

        FirebaseRefs.rateCharts().document(state.milkType)
                .set(map, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    btnSaveFormula.setEnabled(true);
                    btnSaveFormula.setText("Save Formula");

                    if (listener != null) {
                        listener.onFormulaSaved(state);
                    }

                    dismiss();
                })
                .addOnFailureListener(e -> {
                    btnSaveFormula.setEnabled(true);
                    btnSaveFormula.setText("Save Formula");
                    showToast("Save failed: " + e.getMessage());
                });
    }

    private List<RuleSlab> readRules(LinearLayout container, boolean isFat) {
        List<RuleSlab> rules = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);

            EditText etFrom = isFat ? row.findViewById(R.id.etFatFrom) : row.findViewById(R.id.etSnfFrom);
            EditText etTo = isFat ? row.findViewById(R.id.etFatTo) : row.findViewById(R.id.etSnfTo);
            EditText etDiff = isFat ? row.findViewById(R.id.etFatDiff) : row.findViewById(R.id.etSnfDiff);

            if (etFrom == null || etTo == null || etDiff == null) continue;

            rules.add(new RuleSlab(value(etFrom), value(etTo), value(etDiff)));
        }

        return rules;
    }

    private double value(EditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
