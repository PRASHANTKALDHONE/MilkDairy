package com.prashant.milkdairy.Adapter;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.prashant.milkdairy.Model.YieldReportModel;
import com.prashant.milkdairy.R;
import java.util.*;

public class YieldReportAdapter extends RecyclerView.Adapter<YieldReportAdapter.VH> {
    private List<YieldReportModel> list;
    public YieldReportAdapter(List<YieldReportModel> list) { this.list = list; }
    public void updateList(List<YieldReportModel> newList) { list = newList; notifyDataSetChanged(); }

    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_yield_report, p, false));
    }

    public void onBindViewHolder(@NonNull VH h, int i) {
        YieldReportModel m = list.get(i);
        h.tvFarmer.setText(m.farmerName);
        h.tvCowLiters.setText(f(m.cowLiters));
        h.tvBuffaloLiters.setText(f(m.buffaloLiters));
        h.tvMixLiters.setText(f(m.mixLiters));
        h.tvCowAmount.setText(f(m.cowAmount));
        h.tvBuffaloAmount.setText(f(m.buffaloAmount));
        h.tvTotalAmount.setText(f(m.totalAmount));
    }

    public int getItemCount() { return list.size(); }
    private String f(double v) { return String.format(Locale.US, "%.2f", v); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvFarmer, tvCowLiters, tvBuffaloLiters, tvMixLiters, tvCowAmount, tvBuffaloAmount, tvTotalAmount;
        VH(View v) {
            super(v);
            tvFarmer = v.findViewById(R.id.tvFarmer);
            tvCowLiters = v.findViewById(R.id.tvCowLiters);
            tvBuffaloLiters = v.findViewById(R.id.tvBuffaloLiters);
            tvMixLiters = v.findViewById(R.id.tvMixLiters);
            tvCowAmount = v.findViewById(R.id.tvCowAmount);
            tvBuffaloAmount = v.findViewById(R.id.tvBuffaloAmount);
            tvTotalAmount = v.findViewById(R.id.tvTotalAmount);
        }
    }
}
