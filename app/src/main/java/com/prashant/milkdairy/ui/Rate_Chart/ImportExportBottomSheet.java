package com.prashant.milkdairy.ui.Rate_Chart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Import / Export" bottom sheet.
 *
 * Three actions matching the UI image:
 *
 * 1. Import from Excel
 *    Picks a .csv / .xlsx file via system picker.
 *    Expected CSV format:  FAT,SNF,RATE  (header row, then data rows)
 *    Parses each row, validates, merges into state.generatedChart,
 *    and pushes the merged chart to Firestore.
 *
 * 2. Export to Excel
 *    Writes the current generatedChart as a CSV matrix (FAT\SNF header)
 *    to external files dir and opens it via FileProvider / chooser.
 *
 * 3. Download Backup
 *    Writes a flat  FAT,SNF,RATE  CSV (one row per entry) to the same
 *    external files dir and opens it for sharing — easy to re-import later.
 */
public class ImportExportBottomSheet extends BottomSheetDialogFragment {

    private RateChartState state;

    private LinearLayout rowImport, rowExport, rowBackup;
    private ImageView btnClose;

    private ActivityResultLauncher<String[]> filePicker;

    // =========================================================
    //  Factory
    // =========================================================

    public static ImportExportBottomSheet newInstance(RateChartState state) {
        ImportExportBottomSheet sheet = new ImportExportBottomSheet();
        sheet.state = state;
        return sheet;
    }

    // =========================================================
    //  Lifecycle
    // =========================================================

    /**
     * Register the file-picker launcher before the fragment attaches
     * (required by ActivityResultContracts).
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        handleImport(uri);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_import_export, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (state == null) state = new RateChartState();

        initViews(view);
        setupClickListeners();
    }

    private void initViews(View root) {
        rowImport  = root.findViewById(R.id.rowImportExcel);
        rowExport  = root.findViewById(R.id.rowExportExcel);
        rowBackup  = root.findViewById(R.id.rowDownloadBackup);
        btnClose   = root.findViewById(R.id.btnCloseImportExport);
    }

    // =========================================================
    //  Click Listeners
    // =========================================================

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        rowImport.setOnClickListener(v ->
                filePicker.launch(new String[]{
                        "text/csv",
                        "text/comma-separated-values",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }));

        rowExport.setOnClickListener(v -> exportMatrixCsv());

        rowBackup.setOnClickListener(v -> exportFlatBackupCsv());
    }

    // =========================================================
    //  IMPORT
    // =========================================================

    /**
     * Parses a CSV file with format:
     *
     *   FAT,SNF,RATE          ← header row (skipped)
     *   4.0,8.0,38.50
     *   4.0,8.1,38.90
     *   ...
     *
     * Merges parsed entries into state.generatedChart, rebuilds axes,
     * and saves merged chart to Firestore.
     */
    private void handleImport(Uri uri) {
        try {
            InputStream stream = requireContext().getContentResolver().openInputStream(uri);
            if (stream == null) {
                showToast("Cannot read file");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            int imported = 0;
            int skipped  = 0;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip header row
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("fat") || line.toLowerCase().contains("snf")) {
                        continue;
                    }
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    skipped++;
                    continue;
                }

                try {
                    double fat  = Double.parseDouble(parts[0].trim());
                    double snf  = Double.parseDouble(parts[1].trim());
                    double rate = Double.parseDouble(parts[2].trim());

                    if (rate <= 0) {
                        skipped++;
                        continue;
                    }

                    String key = String.format(Locale.US, "%.2f_%.2f", fat, snf);
                    state.generatedChart.put(key, RateChartState.round2(rate));

                    // Update axes
                    if (!state.generatedFatValues.contains(fat)) {
                        state.generatedFatValues.add(fat);
                    }
                    if (!state.generatedSnfValues.contains(snf)) {
                        state.generatedSnfValues.add(snf);
                    }

                    imported++;
                } catch (NumberFormatException e) {
                    skipped++;
                }
            }

            reader.close();
            stream.close();

            if (imported == 0) {
                showToast("No valid rows found in the file (expected: FAT,SNF,RATE)");
                return;
            }

            // Sort axes
            java.util.Collections.sort(state.generatedFatValues);
            java.util.Collections.sort(state.generatedSnfValues);

            showToast("Imported " + imported + " rates" + (skipped > 0 ? " (" + skipped + " skipped)" : ""));

            // Save to Firestore
            pushImportedChartToFirestore();

        } catch (Exception e) {
            showToast("Import failed: " + e.getMessage());
        }
    }

    private void pushImportedChartToFirestore() {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("generatedChart", state.generatedChart);
        updateMap.put("totalRates",     state.generatedChart.size());
        updateMap.put("updatedAt",      com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseRefs.rateCharts().document(state.milkType)
                .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused ->
                        showToast("Chart saved to Firestore (" + state.generatedChart.size() + " rates)"))
                .addOnFailureListener(e ->
                        showToast("Firestore save failed: " + e.getMessage()));
    }

    // =========================================================
    //  EXPORT — Matrix CSV
    // =========================================================

    /**
     * Writes a FAT-x-SNF matrix CSV:
     *
     *   FAT/SNF , 7.5 , 7.6 , ...
     *   3.0     , 34.50 , 34.60 , ...
     *   3.1     , 34.60 , 34.70 , ...
     *
     * Identical to the original RateChartFragment.exportToExcel() logic.
     */
    private void exportMatrixCsv() {
        if (state.generatedChart.isEmpty()) {
            showToast("No chart data — generate the chart first");
            return;
        }

        if (state.generatedFatValues.isEmpty() || state.generatedSnfValues.isEmpty()) {
            state.rebuildAxesFromChartKeys();
        }

        try {
            File file = new File(requireContext().getExternalFilesDir(null),
                    state.milkType + "_RateChart_Matrix.csv");

            FileWriter writer = new FileWriter(file);

            // Header
            writer.append("FAT/SNF,");
            for (double snf : state.generatedSnfValues) {
                writer.append(String.format(Locale.US, "%.2f,", snf));
            }
            writer.append("\n");

            // Data rows
            for (double fat : state.generatedFatValues) {
                writer.append(String.format(Locale.US, "%.2f,", fat));
                for (double snf : state.generatedSnfValues) {
                    Double rate = state.generatedChart.get(
                            String.format(Locale.US, "%.2f_%.2f", fat, snf));
                    writer.append(String.format(Locale.US, "%.2f,", rate == null ? 0 : rate));
                }
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            openFile(file, "text/csv");

        } catch (Exception e) {
            showToast("Export failed: " + e.getMessage());
        }
    }

    // =========================================================
    //  EXPORT — Flat Backup CSV
    // =========================================================

    /**
     * Writes a flat FAT,SNF,RATE CSV (one row per entry) — easy to re-import.
     *
     *   FAT,SNF,RATE
     *   3.0,7.5,34.50
     *   3.0,7.6,34.60
     *   ...
     */
    private void exportFlatBackupCsv() {
        if (state.generatedChart.isEmpty()) {
            showToast("No chart data — generate the chart first");
            return;
        }

        if (state.generatedFatValues.isEmpty() || state.generatedSnfValues.isEmpty()) {
            state.rebuildAxesFromChartKeys();
        }

        try {
            File file = new File(requireContext().getExternalFilesDir(null),
                    state.milkType + "_RateChart_Backup.csv");

            FileWriter writer = new FileWriter(file);
            writer.append("FAT,SNF,RATE\n");

            List<RateEntry> sorted = state.toSortedEntryList();
            for (RateEntry entry : sorted) {
                writer.append(String.format(Locale.US, "%.2f,%.2f,%.2f\n",
                        entry.fat, entry.snf, entry.rate));
            }

            writer.flush();
            writer.close();

            showToast("Backup CSV ready — " + sorted.size() + " rates");
            openFile(file, "text/csv");

        } catch (Exception e) {
            showToast("Backup failed: " + e.getMessage());
        }
    }

    // =========================================================
    //  Open file via FileProvider
    // =========================================================

    private void openFile(File file, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open File"));
        } catch (Exception e) {
            showToast("No app available to open the file");
        }
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}