package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.R;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the "Latest Generated Chart" 5-row preview list on
 * the main Rate Chart screen.
 *
 * Shows FAT | SNF | Rate (₹/L) | [Edit] button.
 * The [Edit] button delegates to OnEditClickListener so the parent
 * fragment can open EditRateBottomSheet.
 */
public class RateEntryAdapter extends RecyclerView.Adapter<RateEntryAdapter.ViewHolder> {

    public interface OnEditClickListener {
        void onEditClick(RateEntry entry);
    }

    private List<RateEntry> entries;
    private final OnEditClickListener listener;

    public RateEntryAdapter(List<RateEntry> entries, OnEditClickListener listener) {
        this.entries  = entries;
        this.listener = listener;
    }

    /** Replaces the list and redraws. */
    public void updateData(List<RateEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rate_entry_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RateEntry entry = entries.get(position);

        holder.tvFat.setText(String.format(Locale.US, "%.1f", entry.fat));
        holder.tvSnf.setText(String.format(Locale.US, "%.1f", entry.snf));
        holder.tvRate.setText(String.format(Locale.US, "%.2f", entry.rate));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(entry);
        });
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFat, tvSnf, tvRate;
        MaterialButton btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFat   = itemView.findViewById(R.id.tvRowFat);
            tvSnf   = itemView.findViewById(R.id.tvRowSnf);
            tvRate  = itemView.findViewById(R.id.tvRowRate);
            btnEdit = itemView.findViewById(R.id.btnRowEdit);
        }
    }
}