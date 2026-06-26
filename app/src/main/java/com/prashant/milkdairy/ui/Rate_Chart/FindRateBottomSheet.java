package com.prashant.milkdairy.ui.Rate_Chart;

import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.R;

import java.util.Locale;

/**
 * "Find Rate" bottom sheet.
 *
 * User enters a FAT value and an SNF value, taps Search, and the
 * rate for that exact FAT/SNF combination is looked up in the
 * in-memory generatedChart map (already loaded from Firestore by
 * the parent fragment or GenerateChartBottomSheet).
 *
 * Lookup strategy:
 *  1. Exact key match  →  "%.2f_%.2f"
 *  2. Nearest-neighbour if no exact match (rounds to nearest 0.1 step)
 *  3. Shows "Not found" if still nothing.
 *
 * The found rate is displayed inline in the sheet with a purple
 * ₹ xx.xx / L result card matching the image design.
 */
public class FindRateBottomSheet extends BottomSheetDialogFragment {

    private RateChartState state;

    private EditText etFat, etSnf;
    private MaterialButton btnSearch;
    private LinearLayout layoutResult;
    private TextView tvResultRate;
    private TextView tvNoResult;
    private ImageView btnClose;

    // =========================================================
    //  Factory
    // =========================================================

    public static FindRateBottomSheet newInstance(RateChartState state) {
        FindRateBottomSheet sheet = new FindRateBottomSheet();
        sheet.state = state;
        return sheet;
    }

    // =========================================================
    //  Lifecycle
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_find_rate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (state == null) state = new RateChartState();

        initViews(view);
        setupClickListeners();
    }

    private void initViews(View root) {
        etFat        = root.findViewById(R.id.etFindFat);
        etSnf        = root.findViewById(R.id.etFindSnf);
        btnSearch    = root.findViewById(R.id.btnSearchRate);
        layoutResult = root.findViewById(R.id.layoutFindResult);
        tvResultRate = root.findViewById(R.id.tvFindResultRate);
        tvNoResult   = root.findViewById(R.id.tvFindNoResult);
        btnClose     = root.findViewById(R.id.btnCloseFind);

        // Hide result panel on open
        layoutResult.setVisibility(View.GONE);
        tvNoResult.setVisibility(View.GONE);
    }

    // =========================================================
    //  Search
    // =========================================================

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        btnSearch.setOnClickListener(v -> {
            String fatStr = etFat.getText().toString().trim();
            String snfStr = etSnf.getText().toString().trim();

            if (fatStr.isEmpty()) {
                etFat.setError("Enter FAT value");
                etFat.requestFocus();
                return;
            }
            if (snfStr.isEmpty()) {
                etSnf.setError("Enter SNF value");
                etSnf.requestFocus();
                return;
            }

            if (state.generatedChart.isEmpty()) {
                showToast("No chart data — generate the rate chart first");
                return;
            }

            double fat = parseD(fatStr);
            double snf = parseD(snfStr);

            Double rate = findRate(fat, snf);

            layoutResult.setVisibility(View.GONE);
            tvNoResult.setVisibility(View.GONE);

            if (rate != null) {
                tvResultRate.setText(String.format(Locale.US, "\u20B9 %.2f / L", rate));
                layoutResult.setVisibility(View.VISIBLE);
            } else {
                tvNoResult.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Lookup order:
     *  1. Exact match on rounded 2-decimal key
     *  2. Round fat/snf to nearest chart step (0.1) and retry
     *  3. Return null if still not found
     */
    private Double findRate(double fat, double snf) {
        // Step 1: exact key
        String exactKey = String.format(Locale.US, "%.2f_%.2f", fat, snf);
        if (state.generatedChart.containsKey(exactKey)) {
            return state.generatedChart.get(exactKey);
        }

        // Step 2: round to nearest 0.1
        double roundedFat = Math.round(fat * 10.0) / 10.0;
        double roundedSnf = Math.round(snf * 10.0) / 10.0;
        String roundedKey = String.format(Locale.US, "%.2f_%.2f", roundedFat, roundedSnf);
        if (state.generatedChart.containsKey(roundedKey)) {
            return state.generatedChart.get(roundedKey);
        }

        // Step 3: search for closest fat, closest snf
        double closestFat = closestValue(state.generatedFatValues, fat);
        double closestSnf = closestValue(state.generatedSnfValues, snf);

        if (closestFat < 0 || closestSnf < 0) return null;

        String closestKey = String.format(Locale.US, "%.2f_%.2f", closestFat, closestSnf);
        return state.generatedChart.get(closestKey);
    }

    /**
     * Returns the value in the list closest to target, or -1 if the list is empty.
     */
    private double closestValue(java.util.List<Double> values, double target) {
        if (values == null || values.isEmpty()) return -1;

        double closest = values.get(0);
        double minDiff = Math.abs(target - closest);

        for (double v : values) {
            double diff = Math.abs(target - v);
            if (diff < minDiff) {
                minDiff = diff;
                closest = v;
            }
        }

        // Only accept if within 1 step tolerance
        return (minDiff <= RateChartState.FAT_STEP * 1.5) ? closest : -1;
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private double parseD(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}