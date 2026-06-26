package com.prashant.milkdairy.ui.Reports;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Adapter.GenericReportPreviewAdapter;
import com.prashant.milkdairy.Model.BillingReportModel;
import com.prashant.milkdairy.Model.InventoryReportModel;
import com.prashant.milkdairy.Model.MilkEntry;
import com.prashant.milkdairy.Model.YieldReportModel;
import com.prashant.milkdairy.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportPreviewDialogFragment extends DialogFragment{

    private static final String KEY_TITLE      = "title";
    private static final String KEY_PERIOD     = "period";
    private static final String KEY_TYPE       = "type";
    private static final String KEY_HEADERS    = "headers";

    // Serialized data keys
    private static final String KEY_MILK_LIST      = "milk_list";
    private static final String KEY_YIELD_LIST     = "yield_list";
    private static final String KEY_BILLING_LIST   = "billing_list";
    private static final String KEY_INVENTORY_LIST = "inventory_list";

    // ─── Data ─────────────────────────────────────────────────────────────────
    private String   title, period, type;
    private String[] headers;
    private List<?>  data;

    private static final String[] MILK_HEADERS      = {"Shift", "Farmer", "Mobile", "Liters", "FAT", "SNF", "Rate", "Amount"};
    private static final String[] YIELD_HEADERS     = {"Farmer", "Cow L", "Buffalo L", "Mix L", "Cow ₹", "Buffalo ₹", "Total ₹"};
    private static final String[] BILLING_HEADERS   = {"Code", "Farmer", "Milk ₹", "Bonus", "Deduction", "Net", "Status"};
    private static final String[] INVENTORY_HEADERS = {"Item", "Category", "Stock In", "Stock Out", "Current", "Unit", "Value"};

    // ─── Factory ──────────────────────────────────────────────────────────────

    /**
     * Call this from ReportsFragment.openPreview().
     * We pass lists through static holders to avoid Parcelable overhead for now.
     */
    private static List<?> pendingData;

    @SuppressWarnings("unchecked")
    public static ReportPreviewDialogFragment newInstance(
            String title, String period, String type, List<?> data, String[] headers) {

        pendingData = data;

        Bundle args = new Bundle();
        args.putString(KEY_TITLE,   title);
        args.putString(KEY_PERIOD,  period);
        args.putString(KEY_TYPE,    type);
        args.putStringArray(KEY_HEADERS, headers);

        ReportPreviewDialogFragment f = new ReportPreviewDialogFragment();
        f.setArguments(args);
        return f;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        Bundle args = getArguments();
        if (args != null) {
            title   = args.getString(KEY_TITLE, "Report Preview");
            period  = args.getString(KEY_PERIOD, "");
            type    = args.getString(KEY_TYPE, "");
            headers = args.getStringArray(KEY_HEADERS);
        }

        // Restore headers from type if not provided
        if (headers == null || headers.length == 0) {
            headers = headersForType(type);
        }

        // Grab data from static holder
        data = pendingData != null ? pendingData : new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return inflater.inflate(R.layout.dialog_report_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Resize dialog to ~93% width
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.93);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Views
        TextView       tvDialogTitle   = view.findViewById(R.id.tvDialogTitle);
        TextView       tvDialogCount   = view.findViewById(R.id.tvDialogCount);
        TextView       tvDialogSummary = view.findViewById(R.id.tvDialogSummary);
        LinearLayout   llHeaders       = view.findViewById(R.id.llDialogHeaders);
        RecyclerView   recycler        = view.findViewById(R.id.recyclerDialog);
        TextView       tvNoData        = view.findViewById(R.id.tvDialogNoData);
        LinearLayout   btnClose        = view.findViewById(R.id.btnDialogClose);
        LinearLayout   btnExcel        = view.findViewById(R.id.btnDialogExcel);
        LinearLayout   btnPdf          = view.findViewById(R.id.btnDialogPdf);

        // Populate header bar
        tvDialogTitle.setText(title);
        tvDialogCount.setText(data.size() + " Records");
        tvDialogSummary.setText("Period: " + period + "  |  Records: " + data.size());

        // Column header row
        buildHeaderRow(llHeaders);

        // RecyclerView
        if (data.isEmpty()) {
            recycler.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);

            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setHasFixedSize(false);
            recycler.setAdapter(new GenericReportPreviewAdapter(requireContext(), data, headers, type));

            // Divider
            recycler.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    Paint p = new Paint();
                    p.setColor(Color.parseColor("#E5E7EB"));
                    p.setStrokeWidth(1f);
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View child = parent.getChildAt(i);
                        c.drawLine(child.getLeft(), child.getBottom(), child.getRight(), child.getBottom(), p);
                    }
                }
            });
        }

        // Buttons
        btnClose.setOnClickListener(v -> dismiss());
        btnExcel.setOnClickListener(v -> exportExcel());
        btnPdf.setOnClickListener(v -> exportPdf());
    }

    // ─── Header Row ───────────────────────────────────────────────────────────

    private void buildHeaderRow(LinearLayout parent) {
        parent.removeAllViews();
        int colWidthPx = dpToPx(getColWidth());

        for (String h : headers) {
            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(colWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dpToPx(2));
            tv.setLayoutParams(lp);
            tv.setText(h);
            tv.setTextSize(9.5f);
            tv.setTextColor(Color.parseColor("#009688"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setMaxLines(1);
            tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            parent.addView(tv);
        }
    }

    // ─── Export: CSV / Excel ──────────────────────────────────────────────────

    private void exportExcel() {
        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "No Data to Export", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String date    = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            String fname   = filePrefix() + "_" + date + ".csv";
            File file      = new File(requireContext().getExternalFilesDir(null), fname);
            FileWriter writer = new FileWriter(file);

            // Header row
            writeCsvRow(writer, headers);

            // Data rows
            for (Object item : data) {
                writeCsvRow(writer, buildCsvRow(item));
            }

            writer.flush();
            writer.close();
            Toast.makeText(requireContext(), "Excel Exported: " + fname, Toast.LENGTH_SHORT).show();
            openFile(file, "text/csv");
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ─── Export: PDF ──────────────────────────────────────────────────────────

    private void exportPdf() {
        if (requireContext() == null) return;
        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "No Data to Export", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            PdfDocument pdf      = new PdfDocument();
            Paint       paint    = new Paint(); paint.setTextSize(9f);
            Paint       bold     = new Paint(); bold.setTextSize(13f); bold.setFakeBoldText(true);
            Paint       subPaint = new Paint(); subPaint.setTextSize(10f); subPaint.setColor(Color.DKGRAY);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(842, 1190, 1).create();
            PdfDocument.Page     page    = pdf.startPage(pageInfo);
            Canvas               canvas  = page.getCanvas();

            int y = 50;
            canvas.drawText(title, 40, y, bold);           y += 22;
            canvas.drawText("Period: " + period, 40, y, subPaint);  y += 16;
            canvas.drawText("Records: " + data.size(), 40, y, subPaint); y += 16;
            String genDate = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(new Date());
            canvas.drawText("Generated: " + genDate, 40, y, subPaint); y += 26;

            // Divider
            Paint line = new Paint(); line.setColor(Color.parseColor("#009688")); line.setStrokeWidth(1.5f);
            canvas.drawLine(40, y, 802, y, line); y += 14;

            // Headers
            int colW = Math.max(75, 762 / Math.max(headers.length, 1));
            int x = 40;
            for (String h : headers) {
                bold.setColor(Color.parseColor("#009688"));
                canvas.drawText(clip(h, 12), x, y, bold);
                x += colW;
            }
            y += 4;
            canvas.drawLine(40, y, 802, y, line); y += 14;
            bold.setColor(Color.BLACK);

            for (Object item : data) {
                if (y > 1140) {
                    pdf.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(842, 1190, pdf.getPages().size() + 1).create();
                    page   = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y      = 50;
                }

                String[] cells = buildCsvRow(item);
                x = 40;
                paint.setColor(Color.parseColor("#111827"));
                for (String cell : cells) {
                    canvas.drawText(clip(cell, 13), x, y, paint);
                    x += colW;
                }
                y += 16;

                // light separator
                Paint sep = new Paint(); sep.setColor(Color.parseColor("#E5E7EB")); sep.setStrokeWidth(0.7f);
                canvas.drawLine(40, y - 2, 802, y - 2, sep);
            }

            pdf.finishPage(page);

            String date  = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            String fname = filePrefix() + "_" + date + ".pdf";
            File file    = new File(requireContext().getExternalFilesDir(null), fname);
            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(requireContext(), "PDF Exported: " + fname, Toast.LENGTH_SHORT).show();
            openFile(file, "application/pdf");
        } catch (Exception e) {
            Toast.makeText(requireContext(), "PDF Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String[] buildCsvRow(Object item) {
        if (item instanceof MilkEntry) {
            MilkEntry e = (MilkEntry) item;
            return new String[]{safe(e.getShift()), safe(e.getFarmerName()), safe(e.getMobile()), fmt(e.getLiters()), fmt(e.getFat()), fmt(e.getSnf()), fmt(e.getRate()), fmt(e.getAmount())};
        } else if (item instanceof YieldReportModel) {
            YieldReportModel y = (YieldReportModel) item;
            return new String[]{safe(y.farmerName), fmt(y.cowLiters), fmt(y.buffaloLiters), fmt(y.mixLiters), fmt(y.cowAmount), fmt(y.buffaloAmount), fmt(y.totalAmount)};
        } else if (item instanceof BillingReportModel) {
            BillingReportModel b = (BillingReportModel) item;
            return new String[]{safe(b.farmerCode), safe(b.farmerName), fmt(b.milkAmount), fmt(b.bonus), fmt(b.deduction), fmt(b.netPayable), safe(b.status)};
        } else if (item instanceof InventoryReportModel) {
            InventoryReportModel m = (InventoryReportModel) item;
            return new String[]{safe(m.itemName), safe(m.category), fmt(m.stockIn), fmt(m.stockOut), fmt(m.currentStock), safe(m.unit), fmt(m.stockValue)};
        }
        return new String[headers.length];
    }

    private String[] headersForType(String t) {
        if (t == null) return MILK_HEADERS;
        if (t.startsWith("milk_"))      return MILK_HEADERS;
        if (t.startsWith("yield_"))     return YIELD_HEADERS;
        if (t.startsWith("billing_"))   return BILLING_HEADERS;
        if (t.startsWith("inv_"))       return INVENTORY_HEADERS;
        return MILK_HEADERS;
    }

    private String filePrefix() {
        if (type == null) return "report";
        switch (type) {
            case "milk_cow":          return "CowMilk";
            case "milk_buffalo":      return "BuffaloMilk";
            case "milk_mix":          return "MixMilk";
            case "yield_cow":         return "CowYield";
            case "yield_buffalo":     return "BuffaloYield";
            case "yield_mix":         return "MixYield";
            case "billing_paid":      return "PaidBills";
            case "billing_pending":   return "PendingBills";
            case "billing_cancelled": return "CancelledBills";
            case "inv_stock_in":      return "StockIn";
            case "inv_stock_out":     return "StockOut";
            case "inv_low_stock":     return "LowStock";
            default:                  return "Report";
        }
    }

    private int getColWidth() {
        if (type == null) return 90;
        if (type.startsWith("milk_"))      return 90;
        if (type.startsWith("yield_"))     return 88;
        if (type.startsWith("billing_"))   return 86;
        if (type.startsWith("inv_"))       return 88;
        return 90;
    }

    private void writeCsvRow(FileWriter writer, String[] cells) throws Exception {
        for (int i = 0; i < cells.length; i++) {
            String v = cells[i] == null ? "" : cells[i];
            writer.append("\"").append(v.replace("\"", "\"\"")).append("\"");
            if (i < cells.length - 1) writer.append(",");
        }
        writer.append("\n");
    }

    private void openFile(File file, String mime) {
        try {
            if (getContext() == null) return;
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app found to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private String fmt(double v) { return String.format(Locale.US, "%.2f", v); }
    private String safe(String v) { return (v == null || v.isEmpty()) ? "-" : v; }
    private String clip(String v, int max) {
        if (v == null) return "";
        return v.length() > max ? v.substring(0, max) : v;
    }

    private int dpToPx(int dp) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}
