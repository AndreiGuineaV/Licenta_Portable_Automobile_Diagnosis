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
import com.example.licenta_test.entities.CarInvite;
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
    private View btnInvites;
    private View badgeInvites;

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

        btnInvites = findViewById(R.id.btnInvites);
        badgeInvites = findViewById(R.id.badgeInvites);

        ImageView btnShowAlerts = findViewById(R.id.btnShowAlerts);
        btnShowAlerts.setOnClickListener(v -> checkAndShowRemindersAlert(true));

        checkPendingInvites();
        btnInvites.setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitesActivity.class);
            invitesLauncher.launch(intent);
        });

        recyclerViewCars = findViewById(R.id.recyclerCars);
        recyclerViewCars.setLayoutManager(new LinearLayoutManager(this));

        carList = new ArrayList<>();
        adapter = new CarAdapter(carList, this, this::showDeleteDialog, this::showEditRemindersDialog, this::showEditInfoDialog, car -> {
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

    ActivityResultLauncher<Intent> invitesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                loadUserCars();
                checkPendingInvites();
            }
    );

    private void checkPendingInvites() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("Invites")
                .whereEqualTo("targetUid", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (badgeInvites != null) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            badgeInvites.setVisibility(View.VISIBLE); // Turn on the red dot
                        } else {
                            badgeInvites.setVisibility(View.GONE); // Hide the red dot
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("INVITES", "Error checking invites", e));
    }

    private void loadUserCars() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Get the user profile to see which is their active car
        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("activeCarId")) {
                String activeCarId = userDoc.getString("activeCarId");
                adapter.setActiveCarId(activeCarId);
            }

            // 2. Download cars from the GLOBAL "Vehicles" collection (Owner OR Shared)
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
                                car.setId(doc.getId()); // Very important: set the ID from Firestore into the object
                                carList.add(car);
                            }
                        }

                        updateUI();
                        checkAndShowRemindersAlert(false);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading cars: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showEditInfoDialog(Car car, int position) {

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!car.getOwnerId().equals(myUid)) {
            Toast.makeText(this, "Only the owner can edit the car's details!", Toast.LENGTH_LONG).show();
            return;
        }

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

        spinnerFuel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedFuel = parent.getItemAtPosition(position).toString();
                if (selectedFuel.equalsIgnoreCase("ELECTRIC")) {
                    etEngine.setVisibility(View.GONE);
                } else {
                    etEngine.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

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
                int newPower = Integer.parseInt(etPower.getText().toString().trim());
                String newFuel = spinnerFuel.getSelectedItem().toString();

                boolean isElectric = newFuel.equalsIgnoreCase("ELECTRIC");
                float newEngine = 0.0f;

                if (!isElectric) {
                    if (etEngine.getText().toString().trim().isEmpty()) {
                        etEngine.setError("Engine cannot be empty");
                        return;
                    }
                    newEngine = Float.parseFloat(etEngine.getText().toString().trim());
                    if (newEngine < 0.049f || newEngine > 8.0f) {
                        etEngine.setError("Invalid engine capacity");
                        return;
                    }
                }

                if (newName.isEmpty()) {
                    etName.setError("Name cannot be empty");
                    return;
                }
                if (newKm < 0) {
                    etMileage.setError("Mileage cannot be negative");
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

        // 1. Search for the user's UID by email
        db.collection("Users").whereEqualTo("email", targetEmail).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String targetUid = query.getDocuments().get(0).getId(); // Found your UID

                        // 2. Add your UID to the "sharedWith" list of the car
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

                        // Set the new owner and clear the shared list (optional)
                        db.collection("Vehicles").document(car.getId())
                                .update(
                                        "ownerId", newOwnerUid,
                                        "sharedWith", new ArrayList<String>() // Reset who has access
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Ownership transferred!", Toast.LENGTH_SHORT).show();
                                    // Here you should remove the car from the list and call adapter.notifyDataSetChanged()
                                });
                    }
                });
    }

    private void showDeleteDialog(int position) {
        Car carToDelete = carList.get(position);

        if (carToDelete.getId() == null) {
            Toast.makeText(this, "Error: car id is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = carToDelete.getOwnerId().equals(myUid);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 1. Adapt the dialog text
        if (isOwner) {
            builder.setTitle("Delete Vehicle");
            builder.setMessage("Are you sure you want to permanently delete " + carToDelete.getCarName() + " from the database?");
        } else {
            builder.setTitle("Remove Shared Vehicle");
            builder.setMessage("Are you sure you want to remove " + carToDelete.getCarName() + " from your garage? The owner will keep the vehicle.");
        }

        // 2. Adapt the positive button action
        builder.setPositiveButton(isOwner ? "Delete" : "Remove", (dialog, which) -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (isOwner) {
                // OWNER LOGIC: Deletes the document completely
                db.collection("Vehicles").document(carToDelete.getId()).delete()
                        .addOnSuccessListener(aVoid -> {
                            // If the deleted car was set as active, we uncheck it
                            if (carToDelete.getId().equals(adapter.getActiveCarId())) {
                                db.collection("Users").document(myUid).update("activeCarId", null);
                                adapter.setActiveCarId(null);
                            }

                            carList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, carList.size());
                            updateUI();

                            Toast.makeText(this, "Vehicle deleted completely.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                db.collection("Vehicles").document(carToDelete.getId())
                        .update("sharedWith", FieldValue.arrayRemove(myUid))
                        .addOnSuccessListener(aVoid -> {
                            // Uncheck the car for deletion
                            if (carToDelete.getId().equals(adapter.getActiveCarId())) {
                                db.collection("Users").document(myUid).update("activeCarId", null);
                                adapter.setActiveCarId(null);
                            }

                            carList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, carList.size());
                            updateUI();

                            Toast.makeText(this, "Vehicle removed from your garage.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error removing vehicle.", Toast.LENGTH_SHORT).show());
            }
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
        car.setOwnerId(uid); // You are the owner

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Save directly to the global "Vehicles" collection
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

    private void checkAndShowRemindersAlert(boolean forceShow) {
        // 1. Verificăm în memoria telefonului când am afișat ultima dată alertele
        android.content.SharedPreferences prefs = getSharedPreferences("GaragePrefs", MODE_PRIVATE);
        long lastShownTime = prefs.getLong("last_alert_time", 0);
        long currentTime = System.currentTimeMillis();

        // Dacă nu forțăm afișarea și nu au trecut cel puțin 12 ore (43.200.000 ms), oprim execuția
        if (!forceShow && (currentTime - lastShownTime < 43200000)) {
            return;
        }

        long thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000;

        StringBuilder expiredAlerts = new StringBuilder();
        StringBuilder upcomingAlerts = new StringBuilder();
        boolean hasAlerts = false;

        for (Car car : carList) {
            String carName = "<b>" + car.getCarName() + "</b>: ";

            // ITP
            if (car.getItpExpiration() > 0) {
                long diff = car.getItpExpiration() - currentTime;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("ITP Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("ITP expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            // RCA
            if (car.getRcaExpiration() > 0) {
                long diff = car.getRcaExpiration() - currentTime;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("RCA Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("RCA expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            // ROVINIETA
            if (car.getRovinietaExpiration() > 0) {
                long diff = car.getRovinietaExpiration() - currentTime;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Rovinieta Expired!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Rovinieta expires in ").append(daysLeft).append(" days.</font><br>");
                        hasAlerts = true;
                    }
                }
            }

            // OIL CHANGE
            if (car.getOilChangeDate() > 0) {
                long diff = car.getOilChangeDate() - currentTime;
                if (diff < 0) {
                    expiredAlerts.append("<font color='#D32F2F'>").append(carName).append("Oil Change Overdue!</font><br>");
                    hasAlerts = true;
                } else {
                    long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
                    if (daysLeft <= 30) {
                        upcomingAlerts.append("<font color='#F57F17'>").append(carName).append("Oil change needed in ").append(daysLeft).append(" days.</font><br>");
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
            builder.setPositiveButton("Understood", (dialog, which) -> {
                // 2. Când utilizatorul dă "Understood", salvăm ora curentă în memorie!
                prefs.edit().putLong("last_alert_time", System.currentTimeMillis()).apply();
                dialog.dismiss();
            });
            builder.show();
        }
    }
    private void showEditRemindersDialog(Car car, int position) {

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!car.getOwnerId().equals(myUid)) {
            Toast.makeText(this, "Only the owner can edit the reminders!", Toast.LENGTH_LONG).show();
            return;
        }

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
                        checkAndShowRemindersAlert(true);
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
        // Only the owner can share or transfer this car
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

            // Get the selected radio button
            int selectedId = radioGroup.getCheckedRadioButtonId();
            boolean isTransfer = (selectedId == R.id.radioTransfer);

            executeShareOrTransfer(car, position, targetEmail, isTransfer, dialog);
        });

        dialog.show();
    }

    private void executeShareOrTransfer(Car car, int position, String targetEmail, boolean isTransfer, AlertDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").whereEqualTo("email", targetEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No user found with this email!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String targetUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                    String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    if (targetUid.equals(myUid)) {
                        Toast.makeText(this, "You cannot share/transfer a car to yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check the car's database live
                    db.collection("Vehicles").document(car.getId()).get().addOnSuccessListener(carDoc -> {
                        if (carDoc.exists()) {
                            List<String> currentSharedWith = (List<String>) carDoc.get("sharedWith");

                            // If we want to share, check that they don't already have access
                            if (!isTransfer && currentSharedWith != null && currentSharedWith.contains(targetUid)) {
                                Toast.makeText(this, "This user already has access to this car!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                return;
                            }

                            // Check that we haven't ALREADY sent a pending invitation for this car
                            db.collection("Invites")
                                    .whereEqualTo("carId", car.getId())
                                    .whereEqualTo("targetUid", targetUid)
                                    .get()
                                    .addOnSuccessListener(inviteSnapshots -> {
                                        if (!inviteSnapshots.isEmpty()) {
                                            Toast.makeText(this, "An invite is already pending for this user!", Toast.LENGTH_LONG).show();
                                            dialog.dismiss();
                                            return;
                                        }

                                        // Everything is OK! Create the invitation. Set the type to "transfer" or "share"
                                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                        String type = isTransfer ? "transfer" : "share";

                                        CarInvite invite = new CarInvite(
                                                car.getId(),
                                                car.getCarName(),
                                                currentUser.getEmail(),
                                                targetUid,
                                                type,
                                                System.currentTimeMillis()
                                        );

                                        db.collection("Invites")
                                                .add(invite)
                                                .addOnSuccessListener(documentReference -> {
                                                    String msg = isTransfer ? "Ownership transfer invite sent!" : "Share invite sent!";
                                                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send invite.", Toast.LENGTH_SHORT).show());
                                    });
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error looking up user.", Toast.LENGTH_SHORT).show());
    }
}