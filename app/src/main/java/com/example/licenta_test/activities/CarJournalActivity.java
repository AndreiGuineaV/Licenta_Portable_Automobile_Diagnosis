package com.example.licenta_test.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.licenta_test.adapters.JournalAdapter;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.JournalEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CarJournalActivity extends AppCompatActivity {

    private Car currentCar;
    private RecyclerView recyclerJournal;
    private JournalAdapter adapter;
    private List<JournalEntry> journalList;
    private FloatingActionButton fabAddLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_car_journal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the car from MyGarage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentCar = getIntent().getSerializableExtra("car", Car.class);
        } else {
            currentCar = (Car) getIntent().getSerializableExtra("car");
        }

        if (currentCar == null) {
            Toast.makeText(this, "Error loading car data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        TextView tvJournalTitle = findViewById(R.id.tvJournalTitle);
        tvJournalTitle.setText(currentCar.getCarName() + " Log");

        fabAddLog = findViewById(R.id.fabAddLog);
        fabAddLog.setOnClickListener(v -> showAddLogDialog());

        recyclerJournal = findViewById(R.id.recyclerJournal);
        recyclerJournal.setLayoutManager(new LinearLayoutManager(this));

        journalList = new ArrayList<>();
        adapter = new JournalAdapter(journalList, new JournalAdapter.OnJournalActionListener() {
            @Override
            public void onItemClick(JournalEntry entry) {
                showEntryDetailsDialog(entry);
            }

            @Override
            public void onItemLongClick(JournalEntry entry, int position) {
                showDeleteConfirmationDialog(entry, position);
            }
        });
        recyclerJournal.setAdapter(adapter);
        recyclerJournal.setAdapter(adapter);

        loadJournalFromFirebase();
    }

    private void showEntryDetailsDialog(JournalEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(entry.getTitle());

        String costString = entry.getCost() > 0 ? "Cost: " + entry.getCost() + " RON\n" : "";
        String message = "Type: " + entry.getType() + "\n" +
                "Mileage: " + entry.getMileageAtLog() + " km\n" +
                costString +
                "\nDetails/Description:\n" + entry.getDescription();

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteConfirmationDialog(JournalEntry entry, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Entry Deletion");
        builder.setMessage("Are you sure you want to delete this entry? This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteEntryFromFirebase(entry, position);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteEntryFromFirebase(JournalEntry entry, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentCar.getId() == null || entry.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Accessing the global "Vehicles" collection directly
        db.collection("Vehicles").document(currentCar.getId())
                .collection("Journal").document(entry.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    journalList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Entry deleted successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete entry.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddLogDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_journal_entry, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        Spinner spinnerType = view.findViewById(R.id.spinnerLogType);
        EditText etTitle = view.findViewById(R.id.etLogTitle);
        EditText etDesc = view.findViewById(R.id.etLogDesc);
        EditText etMileage = view.findViewById(R.id.etLogMileage);
        EditText etCost = view.findViewById(R.id.etLogCost);
        Button btnCancel = view.findViewById(R.id.btnCancelLog);
        Button btnSave = view.findViewById(R.id.btnSaveLog);

        // Types of entries
        String[] logTypes = {"MAINTENANCE", "REPAIR", "DOCUMENT"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, logTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);

        // Set current mileage as default
        etMileage.setText(String.valueOf(currentCar.getKm()));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Checks the input and saves it to Firebase
        btnSave.setOnClickListener(v -> {
            if (spinnerType.getSelectedItem() == null) {
                Toast.makeText(this, "Please select an entry type.", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedType = spinnerType.getSelectedItem().toString();

            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String mileageStr = etMileage.getText().toString().trim();
            String costStr = etCost.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Title is required!");
                etTitle.requestFocus();
                return;
            }

            if (desc.length() > 2000) {
                etDesc.setError("Description cannot exceed 2000 characters!");
                etDesc.requestFocus();
                return;
            }

            if (mileageStr.isEmpty()) {
                etMileage.setError("Mileage is required!");
                etMileage.requestFocus();
                return;
            }

            int mileage;
            try {
                mileage = Integer.parseInt(mileageStr);
                if (mileage < 0) {
                    etMileage.setError("Mileage cannot be negative!");
                    etMileage.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etMileage.setError("Invalid number format for mileage!");
                etMileage.requestFocus();
                return;
            }

            double cost = 0.0;
            if (!costStr.isEmpty()) {
                try {
                    cost = Double.parseDouble(costStr);
                    if (cost < 0) {
                        etCost.setError("Cost cannot be negative!");
                        etCost.requestFocus();
                        return;
                    }
                    if (cost > 250000.0) {
                        etCost.setError("Cost exceeds maximum allowed limit!");
                        etCost.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etCost.setError("Invalid number format for cost!");
                    etCost.requestFocus();
                    return;
                }
            }

            JournalEntry newEntry = new JournalEntry(
                    selectedType,
                    title,
                    desc,
                    mileage,
                    cost,
                    System.currentTimeMillis()
            );

            saveEntryToFirebase(newEntry, dialog, mileage);
        });
        dialog.show();
    }
    private void saveEntryToFirebase(JournalEntry entry, AlertDialog dialog, int newMileage) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentCar.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Saving the log directly under the global Vehicle
        db.collection("Vehicles").document(currentCar.getId())
                .collection("Journal")
                .add(entry)
                .addOnSuccessListener(documentReference -> {
                    entry.setId(documentReference.getId());

                    // Add to the top of the local list and update UI
                    journalList.add(0, entry);
                    adapter.notifyItemInserted(0);
                    recyclerJournal.scrollToPosition(0);

                    Toast.makeText(this, "Log added successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Update car mileage globally if the new log has a higher mileage
                    if (newMileage > currentCar.getKm()) {
                        updateCarMileageInFirebase(newMileage, db);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save log.", Toast.LENGTH_SHORT).show());
    }

    private void updateCarMileageInFirebase(int newMileage, FirebaseFirestore db) {
        currentCar.setKm(newMileage); // Update locally

        db.collection("Vehicles").document(currentCar.getId())
                .update("km", newMileage)
                .addOnSuccessListener(aVoid -> Log.d("JOURNAL", "Car mileage updated globally."));
    }

    private void loadJournalFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentCar.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetching logs from the global Vehicle document, ordered by newest first
        db.collection("Vehicles").document(currentCar.getId())
                .collection("Journal")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    journalList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        JournalEntry entry = doc.toObject(JournalEntry.class);
                        entry.setId(doc.getId());
                        journalList.add(entry);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("JOURNAL", "Error loading journal", e);
                    Toast.makeText(this, "Failed to load history.", Toast.LENGTH_SHORT).show();
                });
    }
}