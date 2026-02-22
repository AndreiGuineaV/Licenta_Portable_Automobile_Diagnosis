package com.example.licenta_test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomePageActivity extends AppCompatActivity {

    ImageView iconProfile;

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

        // 1. Inițializăm instanța bazei de date Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. Creăm un obiect (Map) cu datele mașinii conform schiței tale
        Map<String, Object> masinaTest = new HashMap<>();
        masinaTest.put("marca", "Volkswagen");
        masinaTest.put("model", "Golf VII");
        masinaTest.put("combustibil", "Diesel");
        masinaTest.put("an", 2015);
        // 'id' nu îl punem aici pentru că Firestore îl va genera automat ca ID al documentului

        // 3. Adăugăm datele în colecția "Masini"
        db.collection("Masini")
                .add(masinaTest)
                .addOnSuccessListener(documentReference -> {
                    // Dacă a funcționat, afișăm în Logcat și pe ecran (Toast)
                    Log.d("FirestoreTest", "Mașina a fost adăugată cu ID-ul: " + documentReference.getId());
                    Toast.makeText(this, "Conexiune reușită! Mașină adăugată.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    // Dacă a eșuat, afișăm eroarea
                    Log.w("FirestoreTest", "Eroare la adăugarea mașinii", e);
                    Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        iconProfile = findViewById(R.id.iconProfile);

        iconProfile.setOnClickListener(v ->{
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

    }
}