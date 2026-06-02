package com.example.licenta_test.activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.licenta_test.R;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.DiagnosticReport;
import com.example.licenta_test.entities.JournalEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DiagnosticReportActivity extends AppCompatActivity {

    private Car currentCar;
    private String rawJson;
    private String diagnosisTextToSave; // We keep this to save a clean version to the database
    private String chatHistoryToSave; // To keep the chat history in the database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_diagnostic_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Receive Intents
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentCar = getIntent().getSerializableExtra("car", Car.class);
        } else {
            currentCar = (Car) getIntent().getSerializableExtra("car");
        }
        rawJson = getIntent().getStringExtra("report_json");
        chatHistoryToSave = getIntent().getStringExtra("chat_history");

        ImageView iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish()); // Just closes the report without saving

        if (currentCar == null || rawJson == null) {
            Toast.makeText(this, "Error loading report data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvReportCarName = findViewById(R.id.tvReportCarName);
        tvReportCarName.setText("Vehicle: " + currentCar.getCarName() + " (" + currentCar.getYear() + ")");

        parseAndDisplayJson();

        Button btnSaveReport = findViewById(R.id.btnSaveReport);

        boolean isHistoryView = getIntent().getBooleanExtra("is_history_view", false);
        if (isHistoryView) {
            btnSaveReport.setVisibility(View.GONE); // Hide the button because it is already saved in the database
        }

        btnSaveReport.setOnClickListener(v -> {
            btnSaveReport.setEnabled(false);
            btnSaveReport.setText("Saving...");
            saveReportToDatabase();
        });
    }

    private void parseAndDisplayJson() {
        try {
            // Clean the string just in case the AI added hidden characters before/after the JSON
            int startIndex = rawJson.indexOf("{");
            int endIndex = rawJson.lastIndexOf("}");
            if (startIndex != -1 && endIndex != -1) {
                rawJson = rawJson.substring(startIndex, endIndex + 1);
            }

            JSONObject jsonObject = new JSONObject(rawJson);

            String title = jsonObject.getString("title");
            String severity = jsonObject.getString("severity").toUpperCase();
            String diagnosis = jsonObject.getString("diagnosis");
            String cost = jsonObject.getString("estimated_cost");

            // Format JSON Arrays into bulleted Strings
            StringBuilder actionsBuilder = new StringBuilder();
            JSONArray actionsArray = jsonObject.getJSONArray("recommended_actions");
            for (int i = 0; i < actionsArray.length(); i++) {
                actionsBuilder.append("• ").append(actionsArray.getString(i)).append("\n");
            }

            StringBuilder partsBuilder = new StringBuilder();
            JSONArray partsArray = jsonObject.getJSONArray("parts_needed");
            for (int i = 0; i < partsArray.length(); i++) {
                partsBuilder.append("• ").append(partsArray.getString(i)).append("\n");
            }
            if (partsArray.length() == 0) partsBuilder.append("None");

            // Build the string that will be saved in the database
            diagnosisTextToSave = "Diagnosis:\n" + diagnosis + "\n\nActions:\n" + actionsBuilder.toString().trim() + "\n\nParts:\n" + partsBuilder.toString().trim();

            // Bind to UI
            TextView tvIssueTitle = findViewById(R.id.tvIssueTitle);
            TextView tvSeverityBadge = findViewById(R.id.tvSeverityBadge);
            TextView tvDiagnosisDesc = findViewById(R.id.tvDiagnosisDesc);
            TextView tvActions = findViewById(R.id.tvActions);
            TextView tvParts = findViewById(R.id.tvParts);
            TextView tvCost = findViewById(R.id.tvCost);

            tvIssueTitle.setText(title);
            tvSeverityBadge.setText(severity);
            tvDiagnosisDesc.setText(diagnosis);
            tvActions.setText(actionsBuilder.toString().trim());
            tvParts.setText(partsBuilder.toString().trim());
            tvCost.setText(cost);

            // Dynamic Color Coding for Severity
            switch (severity) {
                case "CRITICAL":
                case "HIGH":
                    tvSeverityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))); // Red
                    break;
                case "MEDIUM":
                    tvSeverityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00"))); // Orange
                    break;
                case "LOW":
                    tvSeverityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#388E3C"))); // Green
                    break;
                default:
                    tvSeverityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#555555"))); // Grey fallback
                    break;
            }

        } catch (JSONException e) {
            Log.e("JSON_PARSE_ERROR", "Failed to parse JSON: " + rawJson, e);
            Toast.makeText(this, "Failed to parse the AI report.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveReportToDatabase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentCar.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();

        TextView tvIssueTitle = findViewById(R.id.tvIssueTitle);
        TextView tvCost = findViewById(R.id.tvCost);

        // 1. Create Journal Entry (For the Global Vehicle)
        // Extract numbers from cost string (e.g. "300 - 500 RON" -> save as 0 in journal or parse it if you want)
        JournalEntry entry = new JournalEntry(
                "DIAGNOSTIC",
                "AI Diagnosis: " + tvIssueTitle.getText().toString(),
                diagnosisTextToSave,
                currentCar.getKm(),
                0.0,
                currentTime
        );

        db.collection("Vehicles").document(currentCar.getId())
                .collection("Journal")
                .add(entry)
                .addOnSuccessListener(docRef -> Log.d("JOURNAL", "Saved to global vehicle journal!"));

        // 2. Create Global Report (For the User's History)
        DiagnosticReport globalReport = new DiagnosticReport(
                currentCar.getCarName() + " (" + currentCar.getYear() + ")",
                tvIssueTitle.getText().toString(), // We use the JSON title instead of the raw user symptom
                diagnosisTextToSave,
                currentTime
        );

        globalReport.setChatHistory(chatHistoryToSave);
        globalReport.setRawJson(rawJson);

        db.collection("Users").document(user.getUid())
                .collection("DiagnosticHistory")
                .add(globalReport)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Report saved successfully!", Toast.LENGTH_SHORT).show();

                    // Return to MyGarage or History
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save report.", Toast.LENGTH_SHORT).show();
                    Button btnSaveReport = findViewById(R.id.btnSaveReport);
                    btnSaveReport.setEnabled(true);
                    btnSaveReport.setText("💾 Save Report to Journal");
                });
    }
}