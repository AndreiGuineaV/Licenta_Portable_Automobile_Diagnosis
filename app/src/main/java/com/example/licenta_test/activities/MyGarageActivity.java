package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.R;
import com.example.licenta_test.adapters.CarAdapter;
import com.example.licenta_test.entities.Car;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyGarageActivity extends AppCompatActivity {

    private FloatingActionButton fabAddVehicle;
    private ImageView iconBack;
    private List<Car> carList;
    private CarAdapter adapter;
    private RecyclerView recyclerViewCars;
    private long tempItp = 0, tempRca = 0, tempRovinieta = 0, tempOil = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_garage);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        recyclerViewCars = findViewById(R.id.recyclerCars);
        recyclerViewCars.setLayoutManager(new LinearLayoutManager(this));

        carList = new ArrayList<>();
        adapter = new CarAdapter(carList, this, this::showDeleteDialog, this::showEditRemindersDialog);
        recyclerViewCars.setAdapter(adapter);

        loadUserCars();

        fabAddVehicle = findViewById(R.id.fabAddVehicle);
        fabAddVehicle.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCarActivity.class);
            launcher.launch(intent);
        });
    }

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Car car = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        car = result.getData().getSerializableExtra("car", Car.class);
                    } else {
                        car = (Car) result.getData().getSerializableExtra("car");
                    }

                    if (car != null) {
                        saveCarToUser(car);
                    }
                }
            }
    );

    private void loadUserCars() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("activeCarId")) {
                String activeCarId = userDoc.getString("activeCarId");
                adapter.setActiveCarId(activeCarId);
            }

            db.collection("Users").document(uid).collection("Cars")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        carList.clear();

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Car car = doc.toObject(Car.class);
                            if (car != null) {
                                carList.add(car);
                            }
                        }
                        updateUI();

                        checkAndShowRemindersAlert();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading cars: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Vehicle");
        builder.setMessage("Are you sure you want to delete " + carList.get(position).getCarName() + "?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            Car carToDelete = carList.get(position);

            if (carToDelete.getId() == null) {
                Toast.makeText(this, "Erorr: car id is null", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("Users").document(uid).collection("Cars").document(carToDelete.getId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        if (carToDelete.getId().equals(adapter.getActiveCarId())) {
                            db.collection("Users").document(uid).update("activeCarId", null);
                            adapter.setActiveCarId(null);
                        }

                        carList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, carList.size());
                        updateUI();

                        Toast.makeText(this, "Vehicle deleted", Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void updateUI() {
        if (carList.isEmpty()) {
            recyclerViewCars.setVisibility(View.GONE);
        } else {
            recyclerViewCars.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    public void saveCarToUser(Car car) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(uid).collection("Cars")
                .add(car)
                .addOnSuccessListener(documentReference -> {
                    car.setId(documentReference.getId());
                    carList.add(car);
                    adapter.notifyItemInserted(carList.size() - 1);
                    updateUI();

                    Toast.makeText(this, "Car added to your garage!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error adding car to user: ", e);
                });
    }

    private void checkAndShowRemindersAlert() {
        long now = System.currentTimeMillis();
        long thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000; // 30 de zile în milisecunde

        StringBuilder expiredAlerts = new StringBuilder();
        StringBuilder upcomingAlerts = new StringBuilder();

        boolean hasAlerts = false;

        for (Car car : carList) {
            String carName = "<b>" + car.getCarName() + "</b>: ";

            if (car.getItpExpiration() > 0) {
                if (car.getItpExpiration() < now) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("ITP Expired!</font><br>");
                    hasAlerts = true;
                } else if (car.getItpExpiration() - now <= thirtyDaysInMs) {
                    upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in less than 30 days.</font><br>");
                    hasAlerts = true;
                }
            }

            if (car.getRcaExpiration() > 0) {
                if (car.getRcaExpiration() < now) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("RCA Expired!</font><br>");
                    hasAlerts = true;
                } else if (car.getRcaExpiration() - now <= thirtyDaysInMs) {
                    upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in less than 30 days.</font><br>");
                    hasAlerts = true;
                }
            }

            if (car.getRovinietaExpiration() > 0) {
                if (car.getRovinietaExpiration() < now) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Rovinieta Expired!</font><br>");
                    hasAlerts = true;
                } else if (car.getRovinietaExpiration() - now <= thirtyDaysInMs) {
                    upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in less than 30 days.</font><br>");
                    hasAlerts = true;
                }
            }

            if (car.getOilChangeDate() > 0) {
                if (car.getOilChangeDate() < now) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Oil Change Overdue!</font><br>");
                    hasAlerts = true;
                } else if (car.getOilChangeDate() - now <= thirtyDaysInMs) {
                    upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed soon.</font><br>");
                    hasAlerts = true;
                }
            }
        }

        if (hasAlerts) {
            String finalMessage = expiredAlerts.toString() + "<br>" + upcomingAlerts.toString();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("⚠️ Vehicle Reminders");
            builder.setMessage(Html.fromHtml(finalMessage, Html.FROM_HTML_MODE_COMPACT));
            builder.setPositiveButton("Understood", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    private void showEditRemindersDialog(Car car, int position) {
        // loading the current data
        tempItp = car.getItpExpiration();
        tempRca = car.getRcaExpiration();
        tempRovinieta = car.getRovinietaExpiration();
        tempOil = car.getOilChangeDate();

        // building the custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_reminders, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        EditText etItp = view.findViewById(R.id.dialogItpDate);
        EditText etRca = view.findViewById(R.id.dialogRcaDate);
        EditText etRovinieta = view.findViewById(R.id.dialogRovinietaDate);
        EditText etOil = view.findViewById(R.id.dialogOilDate);
        Button btnCancel = view.findViewById(R.id.btnDialogCancel);
        Button btnSave = view.findViewById(R.id.btnDialogSave);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        if (tempItp > 0) etItp.setText(sdf.format(new java.util.Date(tempItp)));
        if (tempRca > 0) etRca.setText(sdf.format(new java.util.Date(tempRca)));
        if (tempRovinieta > 0) etRovinieta.setText(sdf.format(new java.util.Date(tempRovinieta)));
        if (tempOil > 0) etOil.setText(sdf.format(new java.util.Date(tempOil)));

        //click to open up calendar
        etItp.setOnClickListener(v -> showDatePickerDialog(etItp, tempItp, timestamp -> tempItp = timestamp));
        etRca.setOnClickListener(v -> showDatePickerDialog(etRca, tempRca, timestamp -> tempRca = timestamp));
        etRovinieta.setOnClickListener(v -> showDatePickerDialog(etRovinieta, tempRovinieta, timestamp -> tempRovinieta = timestamp));
        etOil.setOnClickListener(v -> showDatePickerDialog(etOil, tempOil, timestamp -> tempOil = timestamp));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            // update the objects
            car.setItpExpiration(tempItp);
            car.setRcaExpiration(tempRca);
            car.setRovinietaExpiration(tempRovinieta);
            car.setOilChangeDate(tempOil);

            // Save in Firebase
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(uid).collection("Cars").document(car.getId())
                    .set(car) // Suprascrie mașina cu datele noi
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reminders Updated!", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position); // Actualizăm UI-ul cardului
                        checkAndShowRemindersAlert(); // Recalculăm alertele globale (dacă e nevoie)
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving dates.", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    //Function to open the calendar
    private void showDatePickerDialog(EditText targetEditText, long currentTimestamp, DateSelectListener listener) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // if there is a current timestamp, it opens up the calendar at that date
        if (currentTimestamp > 0) {
            calendar.setTimeInMillis(currentTimestamp);
        }

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            java.util.Calendar newCalendar = java.util.Calendar.getInstance();
            newCalendar.set(year, month, dayOfMonth);
            long selectedTimestamp = newCalendar.getTimeInMillis();

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            targetEditText.setText(sdf.format(newCalendar.getTime()));

            listener.onDateSelected(selectedTimestamp);

        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private interface DateSelectListener {
        void onDateSelected(long timestamp);
    }
}