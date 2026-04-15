package com.example.licenta_test.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.adapters.CarCategoryAdapter;
import com.example.licenta_test.entities.CarCategory;
import com.example.licenta_test.R;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CarPartsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCategories;
    private CarCategoryAdapter adapter;
    private List<CarCategory> carCategoryList;
    ImageView iconBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_car_parts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        recyclerViewCategories = findViewById(R.id.recyclerCarCategories);
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(this, 2));

        loadCarCategories();

        adapter = new CarCategoryAdapter(this, carCategoryList, new CarCategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(CarCategory carCategory) {
                Intent intent = new Intent(CarPartsActivity.this, PartsListActivity.class);
                intent.putExtra("CATEGORY_NAME", carCategory.getName());
                startActivity(intent);
            }
        });

        recyclerViewCategories.setAdapter(adapter);
    }

    private void loadCarCategories(){
        carCategoryList = new ArrayList<>();

        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getAssets().open("car_categories.csv")));
            CSVReader csvReader = new CSVReader(reader);

            String[] line = null;
            csvReader.readNext();

            while((line = csvReader.readNext()) != null){
                String categoryName = line[0];
                String categoryImg = line[1];

                String resourceName = categoryImg.replace(".jpg", "");

                // 2. Get the resource ID of the image drawable
                int imageResId = getResources().getIdentifier(resourceName, "drawable", getPackageName());

                if (imageResId != 0) {
                    carCategoryList.add(new CarCategory(categoryName, imageResId));
                } else {
                    // If not found, add a default image
                    carCategoryList.add(new CarCategory(categoryName, android.R.drawable.ic_menu_report_image));
                }            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }
}