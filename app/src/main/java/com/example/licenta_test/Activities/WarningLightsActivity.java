package com.example.licenta_test.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.Classes.WarningLight;
import com.example.licenta_test.R;
import com.example.licenta_test.Adapters.WarningLightAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WarningLightsActivity extends AppCompatActivity {

    private RecyclerView warningLightsView;
    private EditText etSearch;
    private List<WarningLight> warningLightList;
    private WarningLightAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_warning_lights);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        warningLightsView = findViewById(R.id.recyclerViewWarningLights);
        warningLightsView.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);
        warningLightList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        fetchWarningLightsFromFirebase();

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

    private void fetchWarningLightsFromFirebase() {
        db.collection("Warning_Lights")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        warningLightList.clear();

                        for(QueryDocumentSnapshot document : task.getResult())
                        {
                            WarningLight light = document.toObject(WarningLight.class);
                            warningLightList.add(light);
                        }

                        adapter = new WarningLightAdapter(this, warningLightList);
                        warningLightsView.setAdapter(adapter);

                        Log.d("FIREBASE_READ", warningLightList.size() + " warning lights items have been fetched");
                    }
                    else{
                        Log.e("FIREBASE_READ", "Eroare la citirea datelor", task.getException());
                        Toast.makeText(this, "Eroare la încărcarea datelor", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}