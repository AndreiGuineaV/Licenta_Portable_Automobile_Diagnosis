package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import com.example.licenta_test.entities.FuelType;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Filter;
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
        adapter = new CarAdapter(carList, this, this::showDeleteDialog, this::showEditRemindersDialog, this::showEditInfoDialog, car ->{
            Intent intent = new Intent(this, CarJournalActivity.class);
            intent.putExtra("car", car);
            startActivity(intent);
        }, this::showShareTransferDialog);
        recyclerViewCars.setAdapter(adapter);

        loadUserCars();

        fabAddVehicle = findViewById(R.id.fabAddVehicle);
        fabAddVehicle.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCarActivity.class);
            launcher.launch(intent);
        });
    }

    private interface DateSelectListener {
        void onDateSelected(long timestamp);
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

        // 1. Preluăm profilul utilizatorului pentru a vedea care este mașina lui activă
        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("activeCarId")) {
                String activeCarId = userDoc.getString("activeCarId");
                adapter.setActiveCarId(activeCarId);
            }

            // 2. Descărcăm mașinile din colecția GLOBALĂ "Vehicles" (Owner SAU Shared)
            db.collection("Vehicles")
                    .where(Filter.or(
                            Filter.equalTo("ownerId", uid),
                            Filter.arrayContains("sharedWith", uid)
                    ))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        carList.clear();

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Car car = doc.toObject(Car.class);
                            if (car != null) {
                                car.setId(doc.getId()); // Foarte important: setăm ID-ul din Firestore în obiect
                                carList.add(car);
                            }
                        }

                        updateUI();
                        checkAndShowRemindersAlert();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading cars: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void showEditInfoDialog(Car car, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_car_info, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        EditText etName = view.findViewById(R.id.dialogEtCarName);
        EditText etMileage = view.findViewById(R.id.dialogEtMileage);
        Spinner spinnerFuel = view.findViewById(R.id.dialogSpinnerFuelType);
        EditText etEngine = view.findViewById(R.id.dialogEtEngine);
        EditText etPower = view.findViewById(R.id.dialogEtPower);
        Button btnCancel = view.findViewById(R.id.btnDialogCancelInfo);
        Button btnSave = view.findViewById(R.id.btnDialogSaveInfo);

        ArrayAdapter<FuelType> fuelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FuelType.values());
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuel.setAdapter(fuelAdapter);

        etName.setText(car.getCarName());
        etMileage.setText(String.valueOf(car.getKm()));
        etEngine.setText(String.valueOf(car.getEngine()));
        etPower.setText(String.valueOf(car.getPower()));

        if (car.getFuel() != null) {
            for (int i = 0; i < fuelAdapter.getCount(); i++) {
                if (fuelAdapter.getItem(i).name().equalsIgnoreCase(car.getFuel())) {
                    spinnerFuel.setSelection(i);
                    break;
                }
            }
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            try {
                String newName = etName.getText().toString().trim();
                int newKm = Integer.parseInt(etMileage.getText().toString().trim());
                float newEngine = Float.parseFloat(etEngine.getText().toString().trim());
                int newPower = Integer.parseInt(etPower.getText().toString().trim());
                String newFuel = spinnerFuel.getSelectedItem().toString();

                if (newName.isEmpty()) {
                    etName.setError("Name cannot be empty");
                    return;
                }
                if (newKm < 0) {
                    etMileage.setError("Mileage cannot be negative");
                    return;
                }
                if (newEngine < 0) {
                    etEngine.setError("Engine cannot be negative");
                    return;
                }
                if (newPower < 0) {
                    etPower.setError("Power cannot be negative");
                    return;
                }

                car.setCarName(newName);
                car.setKm(newKm);
                car.setEngine(newEngine);
                car.setPower(newPower);
                car.setFuel(newFuel);

                // Save in Firestore
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("Vehicles").document(car.getId())
                        .set(car)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Vehicle Updated!", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error saving updates.", Toast.LENGTH_SHORT).show());

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please check your numbers (Mileage, Engine, Power).", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void shareCarWithUser(Car car, String targetEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Căutăm UID-ul utilizatorului după email
        db.collection("Users").whereEqualTo("email", targetEmail).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String targetUid = query.getDocuments().get(0).getId(); // Am găsit UID-ul tău

                        // 2. Adăugăm UID-ul tău în lista "sharedWith" a mașinii
                        db.collection("Vehicles").document(car.getId())
                                .update("sharedWith", FieldValue.arrayUnion(targetUid))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Car shared successfully!", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void transferCarOwnership(Car car, String newOwnerEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").whereEqualTo("email", newOwnerEmail).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String newOwnerUid = query.getDocuments().get(0).getId();

                        // Setăm noul owner și curățăm lista de shared (opțional)
                        db.collection("Vehicles").document(car.getId())
                                .update(
                                        "ownerId", newOwnerUid,
                                        "sharedWith", new ArrayList<String>() // Resetăm cine are acces
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Ownership transferred!", Toast.LENGTH_SHORT).show();
                                    // Aici ar trebui să scoți mașina din lista tatălui și să dai adapter.notifyDataSetChanged()
                                });
                    }
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

            db.collection("Vehicles").document(carToDelete.getId()).delete()
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
        car.setOwnerId(uid); // Tu ești proprietarul

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // MODIFICAT: Salvăm direct în colecția globală "Vehicles"
        db.collection("Vehicles")
                .add(car)
                .addOnSuccessListener(documentReference -> {
                    car.setId(documentReference.getId());
                    carList.add(car);
                    adapter.notifyItemInserted(carList.size() - 1);
                    updateUI();

                    Toast.makeText(this, "Car added to your garage!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error adding car: ", e);
                });
    }

    private void checkAndShowRemindersAlert() {
        long now = System.currentTimeMillis();
        long thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000; // 30 days in milliseconds

        StringBuilder expiredAlerts = new StringBuilder();
        StringBuilder upcomingAlerts = new StringBuilder();

        boolean hasAlerts = false;

        for (Car car : carList) {
            String carName = "<b>" + car.getCarName() + "</b>: ";

            if (car.getItpExpiration() > 0) {
                long diff = car.getItpExpiration() - now;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("ITP Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft == 0) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in less than 1 day (TODAY).</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 3) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 15) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in less than 15 days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in less than 30 days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            if (car.getRcaExpiration() > 0) {
                long diff = car.getRcaExpiration() - now;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("RCA Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft == 0) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in less than 1 day (TODAY).</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 3) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 15) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in less than 15 days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in less than 30 days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            if (car.getRovinietaExpiration() > 0) {
                long diff = car.getRovinietaExpiration() - now;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Rovinieta Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft == 0) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in less than 1 day (TODAY).</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 3) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 15) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in less than 15 days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in less than 30 days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            if (car.getOilChangeDate() > 0) {
                long diff = car.getOilChangeDate() - now;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Oil Change Overdue!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft == 0) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed TODAY.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 3) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 15) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed in less than 15 days.</font><br>");
                        hasAlerts = true;
                    } else if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed soon.</font><br>");
                        hasAlerts = true;
                    }
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

            db.collection("Vehicles").document(car.getId())
                    .set(car)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reminders Updated!", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                        checkAndShowRemindersAlert();
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

    private void showShareTransferDialog(Car car, int position) {
        // Verificăm dacă utilizatorul curent este proprietarul real al mașinii
        // Nu poți transfera sau da share la o mașină care ți-a fost doar partajată!
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!car.getOwnerId().equals(myUid)) {
            Toast.makeText(this, "Only the owner can share or transfer this car!", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_share_transfer, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText etEmail = view.findViewById(R.id.etTargetEmail);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupAction);
        Button btnCancel = view.findViewById(R.id.btnCancelAction);
        Button btnConfirm = view.findViewById(R.id.btnConfirmAction);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String targetEmail = etEmail.getText().toString().trim();

            if (targetEmail.isEmpty()) {
                etEmail.setError("Email is required!");
                return;
            }

            // Găsim RadioButton-ul selectat
            int selectedId = radioGroup.getCheckedRadioButtonId();
            boolean isTransfer = (selectedId == R.id.radioTransfer);

            executeShareOrTransfer(car, position, targetEmail, isTransfer, dialog);
        });

        dialog.show();
    }

    private void executeShareOrTransfer(Car car, int position, String targetEmail, boolean isTransfer, AlertDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Căutăm utilizatorul țintă după Email în colecția "Users"
        db.collection("Users").whereEqualTo("email", targetEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No user found with this email!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Luăm UID-ul utilizatorului găsit
                    String targetUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                    String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    if (targetUid.equals(myUid)) {
                        Toast.makeText(this, "You cannot share a car with yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Executăm logica în colecția "Vehicles"
                    if (isTransfer) {
                        // TRANSFER DE PROPRIETATE
                        db.collection("Vehicles").document(car.getId())
                                .update(
                                        "ownerId", targetUid,
                                        "sharedWith", new ArrayList<String>() // Opțional: Resetăm accesul altora
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Ownership transferred successfully!", Toast.LENGTH_SHORT).show();
                                    // Deoarece nu mai ești proprietar, scoatem mașina din lista ta locală
                                    carList.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Transfer failed.", Toast.LENGTH_SHORT).show());
                    } else {
                        // SHARE ACCES
                        // Folosim arrayUnion ca să adăugăm UID-ul doar dacă nu există deja
                        db.collection("Vehicles").document(car.getId())
                                .update("sharedWith", FieldValue.arrayUnion(targetUid))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Car shared successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Share failed.", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error looking up user.", Toast.LENGTH_SHORT).show());
    }
}