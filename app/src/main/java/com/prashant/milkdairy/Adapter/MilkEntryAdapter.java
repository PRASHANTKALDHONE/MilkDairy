package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.MilkEntry;
import com.prashant.milkdairy.R;
import java.util.List;

public class MilkEntryAdapter extends RecyclerView.Adapter<MilkEntryAdapter.ViewHolder> {

    private List<MilkEntry> list;

    public MilkEntryAdapter(List<MilkEntry> list) {
        this.list = list;
    }

    public void updateList(List<MilkEntry> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_milk_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        MilkEntry item = list.get(position);

        h.tvShift.setText(item.getShift());
        h.tvFarmer.setText(item.getFarmerName());
        h.tvMobile.setText(item.getMobile());
        h.tvLiters.setText(String.valueOf(item.getLiters()));
        h.tvFat.setText(String.valueOf(item.getFat()));
        h.tvSnf.setText(String.valueOf(item.getSnf()));
        h.tvRate.setText(String.valueOf(item.getRate()));
        h.tvAmount.setText(String.valueOf(item.getAmount()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvShift, tvFarmer, tvMobile, tvLiters, tvFat, tvSnf, tvRate, tvAmount;

        ViewHolder(View itemView) {
            super(itemView);

            tvShift = itemView.findViewById(R.id.tvShift);
            tvFarmer = itemView.findViewById(R.id.tvFarmer);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvLiters = itemView.findViewById(R.id.tvLiters);
            tvFat = itemView.findViewById(R.id.tvFat);
            tvSnf = itemView.findViewById(R.id.tvSnf);
            tvRate = itemView.findViewById(R.id.tvRate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
