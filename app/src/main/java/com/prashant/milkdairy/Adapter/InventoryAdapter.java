package com.prashant.milkdairy.Adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.Model.InventoryModel;
import com.prashant.milkdairy.R;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    public interface InventoryActionListener {
        void onStockIn(InventoryModel item);
        void onStockOut(InventoryModel item);
        void onSell(InventoryModel item);
        void onEdit(InventoryModel item);
        void onDelete(InventoryModel item);
    }

    private List<InventoryModel> list;
    private final InventoryActionListener listener;

    public InventoryAdapter(List<InventoryModel> list, InventoryActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<InventoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel model = list.get(position);

        holder.txtCode.setText(model.getItemCode());
        holder.txtItemName.setText(model.getItemName());
        holder.txtCategory.setText(model.getCategory());
        holder.txtStock.setText(model.getStock());
        holder.txtMinStock.setText(model.getMinStock());
        holder.txtPurchaseRate.setText(model.getPurchaseRate());
        holder.txtStockValue.setText(model.getStockValue());
        holder.txtLastUpdated.setText(model.getLastUpdated());

        if (model.getCurrentStockValue() <= 0) {
            setStockBadge(holder.txtStock, "#EF4444");
        } else if (model.getCurrentStockValue() <= model.getMinimumStockValue()) {
            setStockBadge(holder.txtStock, "#F59E0B");
        } else {
            setStockBadge(holder.txtStock, "#14B8A6");
        }

        holder.btnStockIn.setOnClickListener(v -> listener.onStockIn(model));
        holder.btnStockOut.setOnClickListener(v -> listener.onStockOut(model));
        holder.btnSell.setOnClickListener(v -> listener.onSell(model));
        holder.btnMenu.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(
                    holder.itemView.getContext(),
                    holder.btnMenu
            );

            popup.getMenu().add(0, 1, 0, "Edit Item");
            popup.getMenu().add(0, 2, 1, "Delete Item");

            popup.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();

                if (id == 1) {
                    if (listener != null) {
                        listener.onEdit(model);
                    }
                    return true;
                }

                if (id == 2) {
                    if (listener != null) {
                        listener.onDelete(model);
                    }
                    return true;
                }

                return false;
            });

            popup.show();
        });
    }
    private void setStockBadge(TextView view, String color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(24f);
        view.setBackground(drawable);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtCode, txtItemName, txtCategory, txtStock;
        TextView txtMinStock, txtPurchaseRate, txtStockValue, txtLastUpdated;
        TextView btnStockIn, btnStockOut;
        MaterialButton btnSell;
        ImageButton btnMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtCode = itemView.findViewById(R.id.txtCode);
            txtItemName = itemView.findViewById(R.id.txtItemName);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtStock = itemView.findViewById(R.id.txtStock);
            txtMinStock = itemView.findViewById(R.id.txtMinStock);
            txtPurchaseRate = itemView.findViewById(R.id.txtPurchaseRate);
            txtStockValue = itemView.findViewById(R.id.txtStockValue);
            txtLastUpdated = itemView.findViewById(R.id.txtLastUpdated);

            btnStockIn = itemView.findViewById(R.id.btnStockIn);
            btnStockOut = itemView.findViewById(R.id.btnStockOut);
            btnSell = itemView.findViewById(R.id.btnSell);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
