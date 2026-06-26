package com.prashant.milkdairy.Adapter;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.prashant.milkdairy.Model.InventoryReportModel;
import com.prashant.milkdairy.R;
import java.util.*;

public class InventoryReportAdapter extends RecyclerView.Adapter<InventoryReportAdapter.VH> {
    private List<InventoryReportModel> list;
    public InventoryReportAdapter(List<InventoryReportModel> list) { this.list = list; }
    public void updateList(List<InventoryReportModel> newList) { list = newList; notifyDataSetChanged(); }

    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_inventory_report, p, false));
    }

    public void onBindViewHolder(@NonNull VH h, int i) {
        InventoryReportModel m = list.get(i);
        h.tvItem.setText(m.itemName);
        h.tvCategory.setText(m.category);
        h.tvStockIn.setText(f(m.stockIn));
        h.tvStockOut.setText(f(m.stockOut));
        h.tvCurrentStock.setText(f(m.currentStock));
        h.tvUnit.setText(m.unit);
        h.tvStockValue.setText(f(m.stockValue));
    }

    public int getItemCount() { return list.size(); }
    private String f(double v) { return String.format(Locale.US, "%.2f", v); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvItem, tvCategory, tvStockIn, tvStockOut, tvCurrentStock, tvUnit, tvStockValue;
        VH(View v) {
            super(v);
            tvItem = v.findViewById(R.id.tvItem);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvStockIn = v.findViewById(R.id.tvStockIn);
            tvStockOut = v.findViewById(R.id.tvStockOut);
            tvCurrentStock = v.findViewById(R.id.tvCurrentStock);
            tvUnit = v.findViewById(R.id.tvUnit);
            tvStockValue = v.findViewById(R.id.tvStockValue);
        }
    }
}
