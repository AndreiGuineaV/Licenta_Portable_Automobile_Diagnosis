package com.example.licenta_test.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.licenta_test.R;
import com.example.licenta_test.entities.JournalEntry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {

    private List<JournalEntry> journalList;
    private OnJournalActionListener listener;
    public interface OnJournalActionListener {
        void onItemClick(JournalEntry entry);
        void onItemLongClick(JournalEntry entry, int position);
    }

    public JournalAdapter(List<JournalEntry> journalList, OnJournalActionListener listener) {
        this.journalList = journalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);

        holder.tvLogTitle.setText(entry.getTitle());
        holder.tvLogDescription.setText(entry.getDescription());
        holder.tvLogKm.setText("🔧 " + entry.getMileageAtLog() + " km");

        if (entry.getCost() > 0) {
            holder.tvLogCost.setText("💰 " + entry.getCost() + " RON");
            holder.tvLogCost.setVisibility(View.VISIBLE);
        } else {
            holder.tvLogCost.setVisibility(View.GONE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.tvLogDate.setText(sdf.format(new Date(entry.getTimestamp())));

        // Badge coloring
        holder.tvLogTypeBadge.setText(entry.getType());
        switch (entry.getType()) {
            case "DIAGNOSTIC":
                holder.tvLogTypeBadge.setBackgroundColor(Color.parseColor("#9C27B0")); // Mov
                break;
            case "REPAIR":
                holder.tvLogTypeBadge.setBackgroundColor(Color.parseColor("#D32F2F")); // Rosu
                break;
            case "MAINTENANCE":
                holder.tvLogTypeBadge.setBackgroundColor(Color.parseColor("#388E3C")); // Verde
                break;
            default:
                holder.tvLogTypeBadge.setBackgroundColor(Color.parseColor("#2962FF")); // Albastru
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            // Check if the item still exists (wasn't already animating out)
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(journalList.get(currentPos));
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemLongClick(journalList.get(currentPos), currentPos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public static class JournalViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogTypeBadge, tvLogDate, tvLogTitle, tvLogDescription, tvLogKm, tvLogCost;

        public JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogTypeBadge = itemView.findViewById(R.id.tvLogTypeBadge);
            tvLogDate = itemView.findViewById(R.id.tvLogDate);
            tvLogTitle = itemView.findViewById(R.id.tvLogTitle);
            tvLogDescription = itemView.findViewById(R.id.tvLogDescription);
            tvLogKm = itemView.findViewById(R.id.tvLogKm);
            tvLogCost = itemView.findViewById(R.id.tvLogCost);
        }
    }
}