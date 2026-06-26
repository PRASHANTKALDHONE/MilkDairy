package com.prashant.milkdairy.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FarmerAdapter extends RecyclerView.Adapter<FarmerAdapter.ViewHolder> {

    public interface FarmerActionListener {
        void onEditClick(Farmer farmer);
        void onDeleteClick(Farmer farmer);

        void onCallClick(Farmer farmer);
    }

    private List<Farmer> list = new ArrayList<>();
    private final FarmerActionListener listener;

    public FarmerAdapter(List<Farmer> list, FarmerActionListener listener) {
        this.list = list == null ? new ArrayList<>() : list;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateList(List<Farmer> newList) {
        this.list = newList == null ? new ArrayList<>() : new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String id = list.get(position).getId();
        return id == null ? RecyclerView.NO_ID : id.hashCode();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView avatar, name, milkType, status, code, mobile;
        View statusDot, btnEdit, btnDelete , btnCallFarmer;;

        public ViewHolder(@NonNull View v) {
            super(v);

            avatar = v.findViewById(R.id.tvAvatar);
            name = v.findViewById(R.id.tvFarmerName);
            milkType = v.findViewById(R.id.tvMilkType);
            status = v.findViewById(R.id.tvStatus);
            code = v.findViewById(R.id.tvFarmerCode);
            mobile = v.findViewById(R.id.tvMobile);
            statusDot = v.findViewById(R.id.viewStatusDot);

            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
            btnCallFarmer=v.findViewById(R.id.btnCallFarmer);
        }
    }

    @NonNull
    @Override
    public FarmerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farmer, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmerAdapter.ViewHolder holder, int position) {
        Farmer farmer = list.get(position);

        String name = safe(farmer.getName());
        String code = safe(farmer.getCode());
        String mobile = safe(farmer.getMobile());
        String milkType = safe(farmer.getMilkType());
        String status = safe(farmer.getStatus());

        if (status.isEmpty()) {
            status = "Active";
        }

        holder.avatar.setText(makeInitials(name));
        holder.name.setText(name.isEmpty() ? "Unknown Farmer" : name);
        holder.code.setText(code.isEmpty() ? "# -" : "# " + code);
        holder.mobile.setText(mobile.isEmpty() ? "-" : "+91 " + mobile);
        holder.milkType.setText(milkType.isEmpty() ? "MIX" : milkType.toUpperCase(Locale.US));
        holder.status.setText(status.toUpperCase(Locale.US));

        boolean active = "Active".equalsIgnoreCase(status);

        if (holder.statusDot != null) {
            holder.statusDot.setAlpha(active ? 1f : 0.45f);
        }

        holder.status.setTextColor(active ? Color.parseColor("#10B26C") : Color.parseColor("#EF4444"));

        holder.btnCallFarmer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(farmer);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(farmer);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(farmer);
            }
        });

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), holder.name.getText(), Toast.LENGTH_SHORT).show());
    }

    private String makeInitials(String name) {
        name = safe(name).trim();

        if (name.isEmpty()) {
            return "?";
        }

        String[] parts = name.split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.US);
        }

        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}