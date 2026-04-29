package com.example.licenta_test.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private OnReportLongClickListener longClickListener;
    private OnFeedbackListener feedbackListener;

    public interface OnReportClickListener{
        void onReportClick(DiagnosticReport report);
    }

    public interface OnReportLongClickListener{
        void onReportLongClick(DiagnosticReport report, int position);
    }

    public interface OnFeedbackListener{
        void onFeedbackClick(DiagnosticReport report, int position, boolean isUseful);
    }
    public DiagnosticReportAdapter(Context context, List<DiagnosticReport> diagnosticReportList, OnReportClickListener clickListener, OnReportLongClickListener longClickListener, OnFeedbackListener feedbackListener) {
        this.context = context;
        this.diagnosticReportList = diagnosticReportList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.feedbackListener = feedbackListener;
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

        ImageView btnUp = holder.itemView.findViewById(R.id.btnThumbsUp);
        ImageView btnDown = holder.itemView.findViewById(R.id.btnThumbsDown);

        btnUp.setColorFilter(android.graphics.Color.parseColor("#CCCCCC"));
        btnDown.setColorFilter(android.graphics.Color.parseColor("#CCCCCC"));
        btnUp.setEnabled(true);
        btnDown.setEnabled(true);

        // We color the selected button based on the feedback status and disable them
        if (currentReport.getFeedbackStatus() == 1) {
            btnUp.setColorFilter(android.graphics.Color.parseColor("#388E3C")); // Verde
            btnUp.setEnabled(false);
            btnDown.setEnabled(false);
        } else if (currentReport.getFeedbackStatus() == -1) {
            btnDown.setColorFilter(android.graphics.Color.parseColor("#D32F2F")); // Roșu
            btnUp.setEnabled(false);
            btnDown.setEnabled(false);
        }

        btnUp.setOnClickListener(v -> {
            if (feedbackListener != null) feedbackListener.onFeedbackClick(currentReport, position, true);
        });

        btnDown.setOnClickListener(v -> {
            if (feedbackListener != null) feedbackListener.onFeedbackClick(currentReport, position, false);
        });

        holder.itemView.setOnClickListener(v -> clickListener.onReportClick(currentReport));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onReportLongClick(currentReport, position);
            return true;
        });
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
