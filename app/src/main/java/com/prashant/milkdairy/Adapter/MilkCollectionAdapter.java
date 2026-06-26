package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.MilkCollectionModel;
import com.prashant.milkdairy.R;

import java.util.List;
import java.util.Locale;

public class MilkCollectionAdapter extends RecyclerView.Adapter<MilkCollectionAdapter.ViewHolder> {

    private List<MilkCollectionModel> list;

    public MilkCollectionAdapter(List<MilkCollectionModel> list) {
        this.list = list;
    }

    public void updateList(List<MilkCollectionModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_milk_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        MilkCollectionModel m = list.get(position);

        h.avatar.setText(getInitials(m.getFarmerName()));
        h.name.setText(safe(m.getFarmerName()));
        h.code.setText("#" + safe(m.getFarmerCode()));
        h.date.setText(safe(m.getDate()));
        h.shift.setText(safe(m.getShift()));
        h.type.setText(safe(m.getMilkType()).toUpperCase(Locale.US));
        h.liters.setText(String.format(Locale.US, "%.2f L", m.getLiters()));
        h.fat.setText(String.format(Locale.US, "%.1f", m.getFat()));
        h.snf.setText(String.format(Locale.US, "%.1f", m.getSnf()));
        h.rate.setText(String.format(Locale.US, "₹ %.2f", m.getRate()));
        h.total.setText(String.format(Locale.US, "₹ %.2f", m.getTotal()));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView avatar, name, code, date, shift, type, liters, fat, snf, rate, total;

        ViewHolder(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.tvAvatar);
            name = v.findViewById(R.id.tvName);
            code = v.findViewById(R.id.tvCode);
            date = v.findViewById(R.id.tvDate);
            shift = v.findViewById(R.id.tvShift);
            type = v.findViewById(R.id.tvType);
            liters = v.findViewById(R.id.tvLiters);
            fat = v.findViewById(R.id.tvFat);
            snf = v.findViewById(R.id.tvSnf);
            rate = v.findViewById(R.id.tvRate);
            total = v.findViewById(R.id.tvTotal);
        }
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "F";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}