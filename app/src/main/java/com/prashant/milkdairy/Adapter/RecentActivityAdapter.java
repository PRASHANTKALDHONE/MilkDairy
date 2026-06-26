package com.prashant.milkdairy.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.prashant.milkdairy.Model.RecentActivity;
import com.prashant.milkdairy.R;

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<RecentActivity> list;

    public RecentActivityAdapter(List<RecentActivity> list) {
        this.list = list;
    }

    public void updateList(List<RecentActivity> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView icon, title, sub, time, amount, tag;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tvActivityIcon);
            title = itemView.findViewById(R.id.tvActivityTitle);
            sub = itemView.findViewById(R.id.tvActivitySub);
            time = itemView.findViewById(R.id.tvActivityTime);
            amount = itemView.findViewById(R.id.tvActivityAmount);
            tag = itemView.findViewById(R.id.tvActivityTag);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecentActivity item = list.get(position);

        holder.title.setText(item.getTitle());
        holder.sub.setText(item.getSubtitle());
        holder.time.setText(item.getTime());
        holder.amount.setText(item.getAmount());
        holder.tag.setText(item.getType());

        String type = item.getType();

        if ("Milk".equalsIgnoreCase(type)) {
            holder.icon.setText("M");
        } else if ("Bill".equalsIgnoreCase(type)) {
            holder.icon.setText("B");
        } else if ("Stock".equalsIgnoreCase(type)) {
            holder.icon.setText("S");
        } else if ("Alert".equalsIgnoreCase(type)) {
            holder.icon.setText("!");
        } else {
            holder.icon.setText("•");
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}
