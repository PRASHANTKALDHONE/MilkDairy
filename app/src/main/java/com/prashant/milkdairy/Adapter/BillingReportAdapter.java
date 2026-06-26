package com.prashant.milkdairy.Adapter;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.prashant.milkdairy.Model.BillingReportModel;
import com.prashant.milkdairy.R;
import java.util.*;

public class BillingReportAdapter extends RecyclerView.Adapter<BillingReportAdapter.VH> {
    private List<BillingReportModel> list;
    public BillingReportAdapter(List<BillingReportModel> list) { this.list = list; }
    public void updateList(List<BillingReportModel> newList) { list = newList; notifyDataSetChanged(); }

    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_billing_report, p, false));
    }

    public void onBindViewHolder(@NonNull VH h, int i) {
        BillingReportModel m = list.get(i);
        h.tvCode.setText(m.farmerCode);
        h.tvFarmer.setText(m.farmerName);
        h.tvMilkAmount.setText(f(m.milkAmount));
        h.tvBonus.setText(f(m.bonus));
        h.tvDeduction.setText(f(m.deduction));
        h.tvNetPayable.setText(f(m.netPayable));
        h.tvStatus.setText(m.status);
    }

    public int getItemCount() { return list.size(); }
    private String f(double v) { return String.format(Locale.US, "%.2f", v); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvFarmer, tvMilkAmount, tvBonus, tvDeduction, tvNetPayable, tvStatus;
        VH(View v) {
            super(v);
            tvCode = v.findViewById(R.id.tvCode);
            tvFarmer = v.findViewById(R.id.tvFarmer);
            tvMilkAmount = v.findViewById(R.id.tvMilkAmount);
            tvBonus = v.findViewById(R.id.tvBonus);
            tvDeduction = v.findViewById(R.id.tvDeduction);
            tvNetPayable = v.findViewById(R.id.tvNetPayable);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
