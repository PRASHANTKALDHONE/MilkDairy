package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.Model.BillModel;
import com.prashant.milkdairy.R;

import java.util.List;
import java.util.Locale;

public class BillsAdapter extends RecyclerView.Adapter<BillsAdapter.ViewHolder> {

    public interface BillActionListener {
        void onPaidClick(BillModel bill);
        void onCancelClick(BillModel bill);
        void onRestoreClick(BillModel bill);
        void onShareClick(BillModel bill);
        void onPdfClick(BillModel bill);
    }

    private List<BillModel> list;
    private final BillActionListener listener;

    public BillsAdapter(List<BillModel> list, BillActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<BillModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillsAdapter.ViewHolder h, int position) {
        BillModel bill = list.get(position);

        h.tvAvatar.setText(getInitials(bill.getFarmerName()));
        h.tvFarmer.setText(safe(bill.getFarmerName()));
        h.tvBillNo.setText("#" + safe(bill.getBillNo()));
        h.tvPeriod.setText(bill.getPeriod());
        h.tvMilkAmount.setText("₹" + bill.getMilkAmount());
        h.tvBonus.setText("+₹" + bill.getBonus());
        h.tvDeduction.setText("-₹" + bill.getDeduction());
        h.tvNetPayable.setText("₹" + bill.getNetPayable());
        h.tvStatus.setText(safe(bill.getStatus()).toUpperCase(Locale.US));

        String status = bill.getStatus() == null
                ? ""
                : bill.getStatus().trim();

        h.btnMenu.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(
                    h.itemView.getContext(),
                    h.btnMenu
            );

            if (status.equalsIgnoreCase("Pending")) {

                popup.getMenu().add(0, 1, 0, "Mark as Paid");
                popup.getMenu().add(0, 2, 1, "Cancel Bill");

            } else if (status.equalsIgnoreCase("Cancelled")) {

                popup.getMenu().add(0, 3, 0, "Restore Bill");
            }

            popup.getMenu().add(0, 4, 10, "WhatsApp Share");
            popup.getMenu().add(0, 5, 11, "Generate PDF");

            popup.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {

                    case 1:
                        if (listener != null) {
                            listener.onPaidClick(bill);
                        }
                        return true;

                    case 2:
                        if (listener != null) {
                            listener.onCancelClick(bill);
                        }
                        return true;

                    case 3:
                        if (listener != null) {
                            listener.onRestoreClick(bill);
                        }
                        return true;

                    case 4:
                        if (listener != null) {
                            listener.onShareClick(bill);
                        }
                        return true;

                    case 5:
                        if (listener != null) {
                            listener.onPdfClick(bill);
                        }
                        return true;
                }

                return false;
            });

            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvFarmer, tvBillNo, tvPeriod, tvStatus;
        TextView tvMilkAmount, tvBonus, tvDeduction, tvNetPayable;
        ImageButton btnMenu;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvFarmer = itemView.findViewById(R.id.tvFarmer);
            tvBillNo = itemView.findViewById(R.id.tvBillNo);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMilkAmount = itemView.findViewById(R.id.tvMilkAmount);
            tvBonus = itemView.findViewById(R.id.tvBonus);
            tvDeduction = itemView.findViewById(R.id.tvDeduction);
            tvNetPayable = itemView.findViewById(R.id.tvNetPayable);
            btnMenu = itemView.findViewById(R.id.btnMenu);

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