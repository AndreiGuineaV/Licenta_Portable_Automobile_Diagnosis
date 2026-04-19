package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    FloatingActionButton fabAddVehicle;
    ImageView iconBack;
    List<Car> carList;
    CarAdapter adapter;
    RecyclerView recyclerViewCars;

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
        adapter = new CarAdapter(carList, this, this::showDeleteDialog);
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
}