package com.example.licenta_test.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.licenta_test.Classes.WarningLight;
import com.example.licenta_test.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class HomePageActivity extends AppCompatActivity {

    ImageView iconProfile;
    LinearLayout warningLightsBtn;

    LinearLayout carPartsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconProfile = findViewById(R.id.iconProfile);
        iconProfile.setOnClickListener(v ->{
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        //uploadWarningLightsToFirebase(this);
        warningLightsBtn = findViewById(R.id.warningLightsLay);
        warningLightsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, WarningLightsActivity.class);
            startActivity(intent);
        });

        carPartsBtn = findViewById(R.id.carPartsLay);
        carPartsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarPartsActivity.class);
            startActivity(intent);
        });
    }

    public void uploadWarningLightsToFirebase(Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getAssets().open("warning_lights_with_symptoms.csv")));
            CSVReader csvReader = new CSVReader(reader);

            String[] line = null;
            csvReader.readNext();

            while((line = csvReader.readNext()) != null){
                    String warningLightName = line[0];
                    String[] causes = line[1].split(" \\| ");
                    String otherDetails = line[2];
                    String imageName = line[3];
                    String[] symptoms = line[4].split(";");

                    String safeDocumentId = warningLightName.replace("/", "-");// ...
                    WarningLight warningLight = new WarningLight(warningLightName, List.of(causes), otherDetails, imageName, List.of(symptoms));
                    db.collection("Warning_Lights")
                            .document(safeDocumentId)
                            .set(warningLight)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FIREBASE_UPLOAD", "Salvat cu succes: " + warningLightName);
                            })
                            .addOnFailureListener(e ->{
                                Log.e("FIREBASE_UPLOAD", "Eroare la salvarea: " + warningLightName, e);
                            });
            }
            csvReader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }
}