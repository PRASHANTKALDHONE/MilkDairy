package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.prashant.milkdairy.Model.FarmerStat;
import com.prashant.milkdairy.R;

import java.util.List;

public class FarmerStatAdapter extends RecyclerView.Adapter<FarmerStatAdapter.ViewHolder> {

    private Context context;
    private List<FarmerStat> statList;

    public FarmerStatAdapter(Context context, List<FarmerStat> statList) {
        this.context = context;
        this.statList = statList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, value;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvStatTitle);
            value = itemView.findViewById(R.id.tvStatValue);
            card = (MaterialCardView) itemView;
        }
    }

    @NonNull
    @Override
    public FarmerStatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_stat_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmerStatAdapter.ViewHolder holder, int position) {

        FarmerStat stat = statList.get(position);

        holder.title.setText(stat.getTitle());
        holder.value.setText(stat.getValue());

        holder.card.setCardBackgroundColor(
                context.getResources().getColor(stat.getBackgroundColor())
        );
    }

    @Override
    public int getItemCount() {
        return statList.size();
    }
}