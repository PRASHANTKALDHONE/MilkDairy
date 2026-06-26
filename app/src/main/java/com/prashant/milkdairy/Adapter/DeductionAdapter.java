package com.prashant.milkdairy.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.DeductionModel;
import com.prashant.milkdairy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeductionAdapter extends RecyclerView.Adapter<DeductionAdapter.ViewHolder> {

    public interface DeductionActionListener {
        void onEditClick(DeductionModel deduction);
        void onDeleteClick(DeductionModel deduction);
    }

    private List<DeductionModel> list;
    private final DeductionActionListener listener;

    public DeductionAdapter(List<DeductionModel> list, DeductionActionListener listener) {
        this.list = list == null ? new ArrayList<>() : list;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateList(List<DeductionModel> newList) {
        this.list = newList == null ? new ArrayList<>() : new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String id = list.get(position).getId();
        return id == null ? RecyclerView.NO_ID : id.hashCode();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView avatar, date, farmerCode, farmerName, category, amount, remaining, status;
        View edit, delete;

        ViewHolder(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.tvAvatar);
            date = v.findViewById(R.id.tvDate);
            farmerCode = v.findViewById(R.id.tvFarmerCode);
            farmerName = v.findViewById(R.id.tvFarmerName);
            category = v.findViewById(R.id.tvCategory);
            amount = v.findViewById(R.id.tvAmount);
            remaining = v.findViewById(R.id.tvRemaining);
            status = v.findViewById(R.id.tvStatus);
            edit = v.findViewById(R.id.btnEdit);
            delete = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public DeductionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deduction_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeductionAdapter.ViewHolder h, int position) {
        DeductionModel item = list.get(position);

        String farmerName = safe(item.getFarmerName());
        String farmerCode = safe(item.getFarmerCode());
        String category = safe(item.getCategory());
        String status = safe(item.getStatus());

        if (status.isEmpty()) {
            status = computeStatus(item.getAmount(), item.getRemaining());
        }

        h.avatar.setText(makeInitials(farmerName));
        h.date.setText(shortDate(item.getDate()));
        h.farmerName.setText(farmerName.isEmpty() ? "Unknown Farmer" : farmerName);
        h.farmerCode.setText(farmerCode.isEmpty() ? "# -" : "# " + farmerCode);
        h.category.setText(category.isEmpty() ? "OTHER" : category.toUpperCase(Locale.US));
        h.amount.setText(String.format(Locale.US, "₹ %.2f", item.getAmount()));
        h.remaining.setText(String.format(Locale.US, "₹ %.2f", item.getRemaining()));
        h.status.setText(status.toUpperCase(Locale.US));

        if ("CLEARED".equalsIgnoreCase(status)) {
            h.status.setTextColor(Color.parseColor("#10B26C"));
            h.remaining.setTextColor(Color.parseColor("#10B26C"));
        } else if ("PARTIAL".equalsIgnoreCase(status)) {
            h.status.setTextColor(Color.parseColor("#FF9800"));
            h.remaining.setTextColor(Color.parseColor("#FF9800"));
        } else {
            h.status.setTextColor(Color.parseColor("#EF4444"));
            h.remaining.setTextColor(Color.parseColor("#EF4444"));
        }

        h.edit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(item);
            }
        });

        h.delete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    private String computeStatus(double amount, double remaining) {
        if (remaining <= 0) return "CLEARED";
        if (remaining < amount) return "PARTIAL";
        return "PENDING";
    }

    private String shortDate(String value) {
        if (value == null) return "-";
        value = value.trim();
        if (value.length() >= 5) return value.substring(0, 5);
        return value;
    }

    private String makeInitials(String name) {
        name = safe(name);
        if (name.isEmpty()) return "?";

        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.US);
        }

        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}