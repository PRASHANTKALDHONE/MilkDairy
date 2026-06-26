package com.prashant.milkdairy.ui.Rate_Chart;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen "All Rate Chart Entries" screen.
 *
 * Features (matching the UI image):
 *  - Receives milkType via Intent extra EXTRA_MILK_TYPE
 *  - Loads the generatedChart from Firestore in real-time
 *  - Search by FAT or SNF (live filtering)
 *  - Sort by FAT / SNF / Rate (tap sort chip to toggle asc/desc)
 *  - Pagination: 20 rows per page with prev/next controls
 *  - Per-row ⋮ menu: "Edit Rate" → opens EditRateBottomSheet,
 *    "Delete" → removes the entry and updates Firestore
 *
 * Implements EditRateBottomSheet.EditRateListener so edits made
 * here are immediately reflected in the list.
 */
public class AllRatesActivity extends AppCompatActivity
        implements EditRateBottomSheet.EditRateListener {

    public static final String EXTRA_MILK_TYPE = "extra_milk_type";

    private static final int PAGE_SIZE = 20;

    // ---- Views ----
    private ImageView  btnFilter,btnBack;
    private EditText etSearch;
    private TextView chipSortFat, chipSortSnf, chipSortRate;
    private RecyclerView recycler;
    private TextView tvPageInfo;
    private ImageView btnPrev, btnNext;

    // ---- Data ----
    private String milkType = "cow";
    private RateChartState state;

    /** Full sorted list after search filter applied */
    private List<RateEntry> filteredList = new ArrayList<>();

    /** Current page (0-indexed) */
    private int currentPage = 0;

    /** Sorting: "fat", "snf", "rate" */
    private String sortField = "fat";
    private boolean sortAsc  = true;

    private AllRateAdapter adapter;
    private ListenerRegistration firestoreListener;

    // =========================================================
    //  Lifecycle
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_rates);

        milkType = getIntent().getStringExtra(EXTRA_MILK_TYPE);
        if (milkType == null || milkType.isEmpty()) milkType = "cow";

        state = new RateChartState();
        state.milkType = milkType;

        initViews();
        setupClickListeners();
        setupSearch();
        listenFirestore();
    }

    private void initViews() {
        btnBack      = findViewById(R.id.btnBackAllRates);
        btnFilter    = findViewById(R.id.btnFilter);
        etSearch     = findViewById(R.id.etSearchRates);
        chipSortFat  = findViewById(R.id.chipSortFat);
        chipSortSnf  = findViewById(R.id.chipSortSnf);
        chipSortRate = findViewById(R.id.chipSortRate);
        recycler     = findViewById(R.id.recyclerAllRates);
        tvPageInfo   = findViewById(R.id.tvPageInfo);
        btnPrev      = findViewById(R.id.btnPrevPage);
        btnNext      = findViewById(R.id.btnNextPage);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AllRateAdapter(new ArrayList<>(), this::showRowMenu);
        recycler.setAdapter(adapter);
    }

    // =========================================================
    //  Firestore real-time listener
    // =========================================================

    private void listenFirestore() {
        firestoreListener = FirebaseRefs.rateCharts()
                .document(milkType)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        showToast("Load error: " + error.getMessage());
                        return;
                    }
                    if (doc == null || !doc.exists()) return;

                    Double base  = doc.getDouble("baseRate");
                    Double fatMin = doc.getDouble("fatMin");
                    Double fatMax = doc.getDouble("fatMax");
                    Double snfMin = doc.getDouble("snfMin");
                    Double snfMax = doc.getDouble("snfMax");

                    if (base != null)  state.baseRate = base;
                    if (fatMin != null) state.fatMin = fatMin;
                    if (fatMax != null) state.fatMax = fatMax;
                    if (snfMin != null) state.snfMin = snfMin;
                    if (snfMax != null) state.snfMax = snfMax;

                    restoreRules(doc.get("fatRules"), true);
                    restoreRules(doc.get("snfRules"), false);

                    // Restore generated chart
                    Object rawChart = doc.get("generatedChart");
                    state.generatedChart.clear();
                    if (rawChart instanceof Map) {
                        Map<?, ?> chart = (Map<?, ?>) rawChart;
                        for (Map.Entry<?, ?> entry : chart.entrySet()) {
                            if (entry.getValue() instanceof Number) {
                                state.generatedChart.put(
                                        String.valueOf(entry.getKey()),
                                        ((Number) entry.getValue()).doubleValue());
                            }
                        }
                        state.rebuildAxesFromChartKeys();
                    }

                    // Reset to page 0 and refresh
                    currentPage = 0;
                    applyFilterAndSort();
                });
    }

    @SuppressWarnings("unchecked")
    private void restoreRules(Object rawRules, boolean isFat) {
        if (!(rawRules instanceof ArrayList)) return;
        ArrayList<Map<String, Object>> rules = (ArrayList<Map<String, Object>>) rawRules;
        List<com.prashant.milkdairy.Model.RuleSlab> target = isFat ? state.fatRules : state.snfRules;
        target.clear();
        int limit = Math.min(rules.size(), RateChartState.MAX_RULE_ROWS);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> rule = rules.get(i);
            target.add(new com.prashant.milkdairy.Model.RuleSlab(
                    toDouble(rule.get("from")),
                    toDouble(rule.get("to")),
                    toDouble(rule.get("diff"))));
        }
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    // =========================================================
    //  Search
    // =========================================================

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                currentPage = 0;
                applyFilterAndSort();
            }
        });
    }

    // =========================================================
    //  Click Listeners
    // =========================================================

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnFilter.setOnClickListener(v ->
                Toast.makeText(this, "Filter — coming soon", Toast.LENGTH_SHORT).show());

        chipSortFat.setOnClickListener(v  -> setSort("fat"));
        chipSortSnf.setOnClickListener(v  -> setSort("snf"));
        chipSortRate.setOnClickListener(v -> setSort("rate"));

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                renderPage();
            }
        });

        btnNext.setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE) - 1;
            if (currentPage < maxPage) {
                currentPage++;
                renderPage();
            }
        });
    }

    private void setSort(String field) {
        if (sortField.equals(field)) {
            sortAsc = !sortAsc;   // toggle direction
        } else {
            sortField = field;
            sortAsc   = true;
        }

        updateSortChipLabels();
        currentPage = 0;
        applyFilterAndSort();
    }

    private void updateSortChipLabels() {
        String arrow = sortAsc ? " ↑" : " ↓";
        chipSortFat.setText("fat".equals(sortField)  ? "FAT"  + arrow : "Sort: FAT");
        chipSortSnf.setText("snf".equals(sortField)  ? "SNF"  + arrow : "Sort: SNF");
        chipSortRate.setText("rate".equals(sortField) ? "Rate" + arrow : "Sort: Rate");
    }

    // =========================================================
    //  Filter + Sort + Paginate
    // =========================================================

    private void applyFilterAndSort() {
        String query = etSearch.getText().toString().trim();
        List<RateEntry> all = state.toSortedEntryList();   // base FAT-asc list

        // --- Filter ---
        if (!query.isEmpty()) {
            double queryVal;
            try { queryVal = Double.parseDouble(query); }
            catch (NumberFormatException e) { queryVal = -1; }

            List<RateEntry> result = new ArrayList<>();
            double qv = queryVal;
            for (RateEntry e : all) {
                if (qv >= 0 && (Math.abs(e.fat - qv) < 0.001 || Math.abs(e.snf - qv) < 0.001)) {
                    result.add(e);
                }
            }
            filteredList = result;
        } else {
            filteredList = all;
        }

        // --- Sort ---
        Comparator<RateEntry> comparator;
        switch (sortField) {
            case "snf":  comparator = Comparator.comparingDouble((RateEntry e) -> e.snf); break;
            case "rate": comparator = Comparator.comparingDouble((RateEntry e) -> e.rate); break;
            default:     comparator = Comparator.comparingDouble((RateEntry e) -> e.fat); break;
        }
        if (!sortAsc) comparator = comparator.reversed();
        filteredList.sort(comparator);

        renderPage();
    }

    private void renderPage() {
        int total = filteredList.size();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        // Clamp current page
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        List<RateEntry> page = from < total ? filteredList.subList(from, to) : new ArrayList<>();
        adapter.updateData(new ArrayList<>(page));

        // Page info label
        if (total == 0) {
            tvPageInfo.setText("No entries");
        } else {
            tvPageInfo.setText("Showing " + (from + 1) + "–" + to + " of " + total);
        }

        btnPrev.setAlpha(currentPage > 0 ? 1f : 0.3f);
        btnNext.setAlpha(currentPage < totalPages - 1 ? 1f : 0.3f);
    }

    // =========================================================
    //  Per-row ⋮ menu
    // =========================================================

    private void showRowMenu(View anchor, RateEntry entry) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Edit Rate");
        menu.getMenu().add(0, 2, 1, "Delete Entry");

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                openEditRate(entry);
                return true;
            } else if (item.getItemId() == 2) {
                deleteEntry(entry);
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void openEditRate(RateEntry entry) {
        EditRateBottomSheet sheet = EditRateBottomSheet.newInstance(state, entry);
        sheet.setListener(this);
        sheet.show(getSupportFragmentManager(), "EditRate");
    }

    /** EditRateBottomSheet.EditRateListener */
    @Override
    public void onRateUpdated(RateEntry updatedEntry) {
        // state.generatedChart already updated inside the sheet;
        // just re-apply filter/sort to refresh the list.
        applyFilterAndSort();
    }

    private void deleteEntry(RateEntry entry) {
        // Remove from in-memory
        state.generatedChart.remove(entry.key());
        state.rebuildAxesFromChartKeys();

        // Remove from Firestore using dot-notation field delete
        java.util.Map<String, Object> deleteMap = new java.util.HashMap<>();
        deleteMap.put("generatedChart." + entry.key(),
                com.google.firebase.firestore.FieldValue.delete());
        deleteMap.put("updatedAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseRefs.rateCharts().document(milkType)
                .update(deleteMap)
                .addOnSuccessListener(unused -> showToast("Entry deleted"))
                .addOnFailureListener(e -> showToast("Delete failed: " + e.getMessage()));

        applyFilterAndSort();
    }

    // =========================================================
    //  Cleanup
    // =========================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // =========================================================
    //  Inner Adapter
    // =========================================================

    /**
     * Adapter for the full "All Rates" paginated list.
     * Shows FAT / SNF / Rate + trend icon + ⋮ menu button.
     */
    static class AllRateAdapter extends RecyclerView.Adapter<AllRateAdapter.VH> {

        interface MenuClickListener {
            void onMenuClick(View anchor, RateEntry entry);
        }

        private List<RateEntry> items;
        private final MenuClickListener menuListener;

        AllRateAdapter(List<RateEntry> items, MenuClickListener menuListener) {
            this.items        = items;
            this.menuListener = menuListener;
        }

        void updateData(List<RateEntry> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_all_rate_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RateEntry entry = items.get(position);

            holder.tvFat.setText(String.format(Locale.US, "%.1f", entry.fat));
            holder.tvSnf.setText(String.format(Locale.US, "%.1f", entry.snf));
            holder.tvRate.setText(String.format(Locale.US, "%.2f", entry.rate));

            // Simple trend icon: rate above 40 = high (up), else normal (arrow)
            // A more meaningful trend would compare to base rate or average
            holder.ivTrend.setImageResource(
                    entry.rate > 40 ? R.drawable.ic_trend_up_green : R.drawable.ic_trend_up_green);

            holder.btnMenu.setOnClickListener(v -> {
                if (menuListener != null) menuListener.onMenuClick(v, entry);
            });
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvFat, tvSnf, tvRate;
            ImageView ivTrend, btnMenu;

            VH(@NonNull View v) {
                super(v);
                tvFat   = v.findViewById(R.id.tvAllFat);
                tvSnf   = v.findViewById(R.id.tvAllSnf);
                tvRate  = v.findViewById(R.id.tvAllRate);
                ivTrend = v.findViewById(R.id.ivTrend);
                btnMenu = v.findViewById(R.id.btnRowMenu);
            }
        }
    }
}