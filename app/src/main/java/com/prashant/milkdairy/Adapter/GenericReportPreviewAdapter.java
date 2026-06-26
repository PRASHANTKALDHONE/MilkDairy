package com.prashant.milkdairy.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.BillingReportModel;
import com.prashant.milkdairy.Model.InventoryReportModel;
import com.prashant.milkdairy.Model.MilkEntry;
import com.prashant.milkdairy.Model.YieldReportModel;
import com.prashant.milkdairy.R;

import java.util.List;
import java.util.Locale;

/**
 * Generic adapter for the Report Preview Dialog.
 * Supports all four report types:
 *   milk_cow / milk_buffalo / milk_mix      → MilkEntry
 *   yield_cow / yield_buffalo / yield_mix   → YieldReportModel
 *   billing_paid / billing_pending / billing_cancelled → BillingReportModel
 *   inv_stock_in / inv_stock_out / inv_low_stock       → InventoryReportModel
 */
public class GenericReportPreviewAdapter extends RecyclerView.Adapter<GenericReportPreviewAdapter.RowVH> {

    // Column width in dp for each report type
    private static final int COL_WIDTH_MILK      = 90;
    private static final int COL_WIDTH_YIELD     = 88;
    private static final int COL_WIDTH_BILLING   = 86;
    private static final int COL_WIDTH_INVENTORY = 88;

    private final Context   context;
    private final List<?>   data;
    private final String[]  headers;
    private final String    reportType;

    public GenericReportPreviewAdapter(Context context, List<?> data, String[] headers, String reportType) {
        this.context    = context;
        this.data       = data;
        this.headers    = headers;
        this.reportType = reportType;
    }

    @NonNull
    @Override
    public RowVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_report_row, parent, false);
        return new RowVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RowVH holder, int position) {
        Object item = data.get(position);
        String[] cells = buildCells(item);

        holder.llRowCells.removeAllViews();

        int colWidthDp = getColWidth();
        int colWidthPx = dpToPx(colWidthDp);

        // Alternate row background
        holder.itemView.setBackgroundColor(
                position % 2 == 0 ? Color.parseColor("#FFFFFF") : Color.parseColor("#F8FFFE")
        );

        for (int i = 0; i < cells.length; i++) {
            TextView tv = new TextView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(colWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dpToPx(2));
            tv.setLayoutParams(lp);
            tv.setText(cells[i]);
            tv.setTextSize(10f);
            tv.setMaxLines(1);
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setPadding(dpToPx(4), dpToPx(3), dpToPx(4), dpToPx(3));

            // Status column coloring for billing
            if (reportType.startsWith("billing_") && i == cells.length - 1) {
                String val = cells[i];
                if ("Paid".equalsIgnoreCase(val)) {
                    tv.setTextColor(Color.parseColor("#16A34A"));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                } else if ("Cancelled".equalsIgnoreCase(val)) {
                    tv.setTextColor(Color.parseColor("#EF4444"));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    tv.setTextColor(Color.parseColor("#F59E0B"));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }
            } else if (i == 0) {
                tv.setTextColor(Color.parseColor("#111827"));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tv.setTextColor(Color.parseColor("#374151"));
                tv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);
            }

            holder.llRowCells.addView(tv);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ─── Cell builders ────────────────────────────────────────────────────────

    private String[] buildCells(Object item) {
        if (item instanceof MilkEntry) {
            MilkEntry e = (MilkEntry) item;
            return new String[]{
                    safe(e.getShift()),
                    safe(e.getFarmerName()),
                    safe(e.getMobile()),
                    fmt(e.getLiters()) + " L",
                    fmt(e.getFat()),
                    fmt(e.getSnf()),
                    "₹" + fmt(e.getRate()),
                    "₹" + fmt(e.getAmount())
            };
        } else if (item instanceof YieldReportModel) {
            YieldReportModel y = (YieldReportModel) item;
            return new String[]{
                    safe(y.farmerName),
                    fmt(y.cowLiters),
                    fmt(y.buffaloLiters),
                    fmt(y.mixLiters),
                    "₹" + fmt(y.cowAmount),
                    "₹" + fmt(y.buffaloAmount),
                    "₹" + fmt(y.totalAmount)
            };
        } else if (item instanceof BillingReportModel) {
            BillingReportModel b = (BillingReportModel) item;
            return new String[]{
                    safe(b.farmerCode),
                    safe(b.farmerName),
                    "₹" + fmt(b.milkAmount),
                    "₹" + fmt(b.bonus),
                    "₹" + fmt(b.deduction),
                    "₹" + fmt(b.netPayable),
                    safe(b.status)
            };
        } else if (item instanceof InventoryReportModel) {
            InventoryReportModel m = (InventoryReportModel) item;
            return new String[]{
                    safe(m.itemName),
                    safe(m.category),
                    fmt(m.stockIn),
                    fmt(m.stockOut),
                    fmt(m.currentStock),
                    safe(m.unit),
                    "₹" + fmt(m.stockValue)
            };
        }
        return new String[headers.length];
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int getColWidth() {
        if (reportType.startsWith("milk_"))      return COL_WIDTH_MILK;
        if (reportType.startsWith("yield_"))     return COL_WIDTH_YIELD;
        if (reportType.startsWith("billing_"))   return COL_WIDTH_BILLING;
        if (reportType.startsWith("inv_"))       return COL_WIDTH_INVENTORY;
        return 90;
    }

    private String fmt(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private String safe(String v) {
        return v == null ? "-" : v.isEmpty() ? "-" : v;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class RowVH extends RecyclerView.ViewHolder {
        LinearLayout llRowCells;

        RowVH(@NonNull View itemView) {
            super(itemView);
            llRowCells = itemView.findViewById(R.id.llRowCells);
        }
    }
}
