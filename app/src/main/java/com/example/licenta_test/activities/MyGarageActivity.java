package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.adapters.CarAdapter;
import com.example.licenta_test.additional.GarageStore;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());



        recyclerViewCars = findViewById(R.id.recyclerCars);
        recyclerViewCars.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        if (carList == null) {
            carList = GarageStore.loadCarList(this);
        }
        adapter = new CarAdapter(carList, this, this::showDeleteDialog);
        recyclerViewCars.setAdapter(adapter);

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
                        carList.add(car);
                        adapter.notifyItemInserted(carList.size() - 1);
                        GarageStore.saveCarList(this, carList);
                    }
                }
            }
    );
    private void showDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Vehicle");
        builder.setMessage("Are you sure you want to delete " + carList.get(position).getCarName() + "?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            carList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, carList.size());
            GarageStore.saveCarList(this, carList);

            Toast.makeText(this, "Vehicle deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}