package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.BonusResultModel;
import com.prashant.milkdairy.R;

import java.util.List;
import java.util.Locale;

public class BonusResultAdapter extends RecyclerView.Adapter<BonusResultAdapter.ViewHolder> {

    private List<BonusResultModel> list;

    public BonusResultAdapter(List<BonusResultModel> list) {
        this.list = list;
    }

    public void updateList(List<BonusResultModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BonusResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bonus_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BonusResultAdapter.ViewHolder h, int position) {
        BonusResultModel model = list.get(position);

        h.tvCode.setText(safe(model.getFarmerCode()));
        h.tvName.setText(safe(model.getFarmerName()));
        h.tvLiters.setText(String.format(Locale.US, "%.2f", model.getTotalLiters()));
        h.tvAmount.setText(String.format(Locale.US, "\u20B9 %.2f", model.getMilkAmount()));
        h.tvBonus.setText(String.format(Locale.US, "\u20B9 %.2f", model.getBonusAmount()));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvCode, tvName, tvLiters, tvAmount, tvBonus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCode = itemView.findViewById(R.id.tvCode);
            tvName = itemView.findViewById(R.id.tvName);
            tvLiters = itemView.findViewById(R.id.tvLiters);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvBonus = itemView.findViewById(R.id.tvBonus);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}