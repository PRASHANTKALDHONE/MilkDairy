package com.prashant.milkdairy.ui.Rate_Chart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.R;

import java.util.Locale;

/**
 * "Edit Rate" bottom sheet.
 *
 * Pre-fills FAT, SNF from the selected RateEntry (both read-only hints),
 * lets the user change only the Rate value, then calls the listener with
 * the updated RateEntry so the parent can:
 *  1. Update state.generatedChart in-memory
 *  2. Push the single changed entry to Firestore via set(merge)
 *
 * FAT and SNF fields are non-editable (the key stays the same — only the
 * rate value changes), matching the "Edit Rate" dialog in the UI image.
 */
public class EditRateBottomSheet extends BottomSheetDialogFragment {

    public interface EditRateListener {
        void onRateUpdated(RateEntry updatedEntry);
    }

    private RateChartState state;
    private RateEntry entry;
    private EditRateListener listener;

    private EditText etFat, etSnf, etRate;
    private MaterialButton btnCancel, btnUpdate;
    private ImageView btnClose;

    // =========================================================
    //  Factory
    // =========================================================

    public static EditRateBottomSheet newInstance(RateChartState state, RateEntry entry) {
        EditRateBottomSheet sheet = new EditRateBottomSheet();
        sheet.state = state;
        sheet.entry = entry;
        return sheet;
    }

    public void setListener(EditRateListener listener) {
        this.listener = listener;
    }

    // =========================================================
    //  Lifecycle
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_edit_rate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        bindEntry();
        setupClickListeners();
    }

    private void initViews(View root) {
        etFat     = root.findViewById(R.id.etEditFat);
        etSnf     = root.findViewById(R.id.etEditSnf);
        etRate    = root.findViewById(R.id.etEditRate);
        btnCancel = root.findViewById(R.id.btnCancelEdit);
        btnUpdate = root.findViewById(R.id.btnUpdateRate);
        btnClose  = root.findViewById(R.id.btnCloseEdit);
    }

    private void bindEntry() {
        if (entry == null) return;

        etFat.setText(String.format(Locale.US, "%.1f", entry.fat));
        etSnf.setText(String.format(Locale.US, "%.1f", entry.snf));
        etRate.setText(String.format(Locale.US, "%.2f", entry.rate));

        // FAT and SNF are display-only — the key (fat+snf) must not change
        etFat.setFocusable(false);
        etFat.setFocusableInTouchMode(false);
        etFat.setAlpha(0.65f);

        etSnf.setFocusable(false);
        etSnf.setFocusableInTouchMode(false);
        etSnf.setAlpha(0.65f);

        // Focus on rate for quick editing
        etRate.requestFocus();
        etRate.selectAll();
    }

    // =========================================================
    //  Click Listeners
    // =========================================================

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        btnUpdate.setOnClickListener(v -> {
            String rateStr = etRate.getText().toString().trim();

            if (rateStr.isEmpty()) {
                etRate.setError("Enter a rate value");
                etRate.requestFocus();
                return;
            }

            double newRate;
            try {
                newRate = Double.parseDouble(rateStr);
            } catch (NumberFormatException e) {
                etRate.setError("Invalid number");
                return;
            }

            if (newRate <= 0) {
                etRate.setError("Rate must be > 0");
                return;
            }

            if (entry == null) return;

            // Build updated entry
            RateEntry updated = new RateEntry(entry.fat, entry.snf, RateChartState.round2(newRate));

            // Update in-memory chart directly so the parent doesn't need another Firestore read
            if (state != null) {
                state.generatedChart.put(updated.key(), updated.rate);
            }

            btnUpdate.setEnabled(false);
            btnUpdate.setText("Updating...");

            // Persist the single changed rate to Firestore using dot-notation field path
            // to avoid overwriting the entire generatedChart map.
            String fieldPath = "generatedChart." + updated.key();
            java.util.Map<String, Object> updateMap = new java.util.HashMap<>();
            updateMap.put(fieldPath, updated.rate);
            updateMap.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            com.prashant.milkdairy.Utils.FirebaseRefs.rateCharts()
                    .document(state != null ? state.milkType : "cow")
                    .update(updateMap)
                    .addOnSuccessListener(unused -> {
                        btnUpdate.setEnabled(true);
                        btnUpdate.setText("Update Rate");

                        if (listener != null) listener.onRateUpdated(updated);

                        showToast("Rate updated ✓");
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnUpdate.setEnabled(true);
                        btnUpdate.setText("Update Rate");
                        showToast("Update failed: " + e.getMessage());
                    });
        });
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}