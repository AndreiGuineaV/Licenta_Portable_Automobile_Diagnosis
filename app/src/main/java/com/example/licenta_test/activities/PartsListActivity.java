package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.adapters.CarPartAdapter;
import com.example.licenta_test.entities.CarPart;
import com.example.licenta_test.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PartsListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCarParts;
    private EditText etSearch;
    private String categoryName;
    private List<CarPart> carPartList;
    private FirebaseFirestore db;
    private CarPartAdapter adapter;
    ImageView iconBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parts_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        recyclerViewCarParts = findViewById(R.id.recyclerViewCarParts);
        etSearch = findViewById(R.id.etSearch);
        carPartList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        // Get the category name from the intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            categoryName = getIntent().getSerializableExtra("CATEGORY_NAME", String.class);
        }
        else{
            categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        }
        Toast.makeText(this, "You clicked on the category: " + categoryName, Toast.LENGTH_SHORT).show();

        categoryName = categoryName.replace(",", "");

        recyclerViewCarParts.setLayoutManager(new GridLayoutManager(this, 3));

        fetchCarPartsFromFirebase();

        setupSearchBar();
    }

    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(adapter != null){
                    adapter.getFilter().filter(s);
                }
            }
        });
    }
    private void fetchCarPartsFromFirebase() {
        db.collection("Car_Parts")
                .whereEqualTo("category", categoryName)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        carPartList.clear();

                        for(QueryDocumentSnapshot document : task.getResult()){
                            CarPart carPart = document.toObject(CarPart.class);
                            carPartList.add(carPart);
                        }

                        adapter = new CarPartAdapter(this, carPartList, carPart -> {
                            showCarPartDetailsDialog(carPart);
                        });
                        recyclerViewCarParts.setAdapter(adapter);

                        Log.d("FIREBASE_READ", carPartList.size() + " car parts items have been fetched");
                    }
                    else{
                        Log.e("FIREBASE_READ", "Error reading the data", task.getException());
                        Toast.makeText(this, "There was an error during the upload of the data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCarPartDetailsDialog(CarPart carPart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(carPart.getName());

        StringBuilder message = new StringBuilder();
        message.append("Category: ").append(carPart.getCategory() != null ? carPart.getCategory() : "N/A").append("\n\n");

        message.append("Compatible Fuels:\n");
        if (carPart.getCompatibleFuels() != null && !carPart.getCompatibleFuels().isEmpty()) {
            String fuels = android.text.TextUtils.join(", ", carPart.getCompatibleFuels());
            message.append(fuels).append("\n\n");
        } else {
            message.append("Not specified\n\n");
        }

        message.append("Malfunction Symptoms:\n");
        if (carPart.getMalfunctionSymptoms() != null && !carPart.getMalfunctionSymptoms().isEmpty()) {
            for (String symptom : carPart.getMalfunctionSymptoms()) {
                message.append("• ").append(symptom).append("\n");
            }
        } else {
            message.append("Not specified\n");
        }

        builder.setMessage(message.toString());

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Learn More", (dialog, which) -> {
            String searchQuery = carPart.getName() + " car part symptoms";

            String url = "https://www.google.com/search?q=" + android.net.Uri.encode(searchQuery);

            // Create an intent to open the URL
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(browserIntent);
        });

        builder.create().show();
    }
}