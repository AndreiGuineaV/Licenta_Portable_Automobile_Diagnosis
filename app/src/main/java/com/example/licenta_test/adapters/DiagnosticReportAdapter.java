package com.example.licenta_test.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.R;
import com.example.licenta_test.entities.DiagnosticReport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiagnosticReportAdapter extends RecyclerView.Adapter<DiagnosticReportAdapter.DiagnosticViewHolder> {

    private Context context;
    private List<DiagnosticReport> diagnosticReportList;
    private OnReportClickListener clickListener;

    public interface OnReportClickListener{
        void onReportClick(DiagnosticReport report);
    }

    public DiagnosticReportAdapter(Context context, List<DiagnosticReport> diagnosticReportList, OnReportClickListener clickListener) {
        this.context = context;
        this.diagnosticReportList = diagnosticReportList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public DiagnosticReportAdapter.DiagnosticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new DiagnosticViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiagnosticReportAdapter.DiagnosticViewHolder holder, int position) {
        DiagnosticReport currentReport = diagnosticReportList.get(position);
        holder.tvCarName.setText(currentReport.getCarName());
        holder.tvDate.setText(formatTimestampToDate(currentReport.getTimestamp()));
        holder.tvSymptoms.setText(currentReport.getUserSymptoms());

        holder.itemView.setOnClickListener(v -> clickListener.onReportClick(currentReport));
    }

    @Override
    public int getItemCount() {
        return diagnosticReportList.size();
    }

    public static class DiagnosticViewHolder extends RecyclerView.ViewHolder {
        TextView tvCarName;
        TextView tvDate;
        TextView tvSymptoms;

        public DiagnosticViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarName = itemView.findViewById(R.id.tvHistoryCarName);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvSymptoms = itemView.findViewById(R.id.tvHistorySymptoms);
        }
    }
    private String formatTimestampToDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        return formatter.format(date);
    }
}
