package com.example.licenta_test.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.R;
import com.example.licenta_test.adapters.DiagnosticReportAdapter;
import com.example.licenta_test.entities.DiagnosticReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerHistory;
    LinearLayout layoutHistoryEmpty;
    ImageView iconBack;
    List<DiagnosticReport> diagnosticReportList;
    DiagnosticReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_diagnostic_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        layoutHistoryEmpty = findViewById(R.id.layoutEmptyHistory);
        diagnosticReportList = new ArrayList<>();

        adapter = new DiagnosticReportAdapter(this, diagnosticReportList, report -> {
            showFullReportDialog(report);
        });
        recyclerHistory.setAdapter(adapter);

        iconBack.setOnClickListener(v -> finish());

        loadHistoryFromFirebase();
    }

    private void loadHistoryFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(user.getUid()).collection("DiagnosticHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    diagnosticReportList.clear();

                    for(var document : queryDocumentSnapshots) {
                        DiagnosticReport report = document.toObject(DiagnosticReport.class);
                        if(report != null)
                        {
                            report.setId(document.getId());
                            diagnosticReportList.add(report);
                        }
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (diagnosticReportList.isEmpty()) {
            recyclerHistory.setVisibility(View.GONE);
            layoutHistoryEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerHistory.setVisibility(View.VISIBLE);
            layoutHistoryEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showFullReportDialog(DiagnosticReport report) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Diagnostic Report");
        String dialogMessage = "SYMPTOMS REPORTED:\n" + report.getUserSymptoms() +
                "\n\n------------------------\n\n" +
                "AI DIAGNOSIS:\n" + report.getAiDiagnosis();
        builder.setMessage(dialogMessage);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}