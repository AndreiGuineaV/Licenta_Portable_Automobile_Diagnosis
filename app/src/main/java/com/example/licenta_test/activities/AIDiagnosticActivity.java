package com.example.licenta_test.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.BuildConfig;
import com.example.licenta_test.R;
import com.example.licenta_test.adapters.ChatAdapter;
import com.example.licenta_test.additional.GarageStorage;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.ChatMessage;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIDiagnosticActivity extends AppCompatActivity {

    EditText etAiSearchPrompt;
    ImageView sendPromptBtn;
    TextView tvActiveCarBanner;
    RecyclerView recyclerAiChat;
    List<ChatMessage> chatList;
    ChatAdapter adapter;
    ImageView iconBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aidiagnostic);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        etAiSearchPrompt = findViewById(R.id.etAiSearchPrompt);
        sendPromptBtn = findViewById(R.id.btnSendPrompt);
        tvActiveCarBanner = findViewById(R.id.tvActiveCarBanner);

        recyclerAiChat = findViewById(R.id.recyclerAiChat);
        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerAiChat.setLayoutManager(layoutManager);
        recyclerAiChat.setAdapter(adapter);

        Car activeCar = GarageStorage.getSelectedCar(this);
        if(activeCar != null)
        {
            etAiSearchPrompt.setText(""); //resetting the input
            tvActiveCarBanner.setText("Active Vehicle: " + activeCar.getCarName());

            addMessageToChat("Hi! I'm your car assistant. What's the problem with your " + activeCar.getCarName() + "?", false);
        }
        else
        {
            Toast.makeText(this, "Please select a car from your garage!", Toast.LENGTH_SHORT).show();
        }

        sendPromptBtn.setOnClickListener(v -> {
            String userSymptom = etAiSearchPrompt.getText().toString().trim();

            if (!userSymptom.isEmpty() && activeCar != null) {
                addMessageToChat(userSymptom, true);
                etAiSearchPrompt.setText(""); //emptying the search bar

                addMessageToChat("Analyzing the symptoms and creating the diagnostic...", false);
                int loadingMessagePosition = chatList.size() - 1;

                searchDatabaseAndDiagnose(activeCar, userSymptom, loadingMessagePosition);

            } else if (activeCar == null) {
                Toast.makeText(this, "Please select a car from your garage first!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Please describe the problem with your car!", Toast.LENGTH_SHORT).show();
            }


        });
    }

    private void searchDatabaseAndDiagnose(Car activeCar, String userSymptom, int loadingPosition) {

        String carFuelLower = activeCar.getFuel().toLowerCase();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StringBuilder contextBuilder = new StringBuilder();

        Task<QuerySnapshot> carPartsTask = db.collection("Car_Parts")
                .whereArrayContains("compatibleFuels", carFuelLower)
                .get();
        Task<QuerySnapshot> warningLightsTask = db.collection("Warning_Lights")
                .get();

        Tasks.whenAllSuccess(carPartsTask, warningLightsTask).addOnSuccessListener(results -> {
            QuerySnapshot carPartsSnapshot = (QuerySnapshot) results.get(0);
            QuerySnapshot warningLightsSnapshot = (QuerySnapshot) results.get(1);

            contextBuilder.append("--- MY LOCAL DATABASE ---\n");

            contextBuilder.append("\nWARNING LIGHTS AND THEIR SYMPTOMS:\n");
            for (QueryDocumentSnapshot doc : warningLightsSnapshot) {
                String name = doc.getString("name");
                String otherDetails = doc.getString("otherDetails");
                List<String> causes = (List<String>) doc.get("causes");
                List<String> symptomsList = (List<String>) doc.get("symptoms");

                contextBuilder.append("- ").append(name)
                        .append(" | Causes: ").append(causes)
                        .append(" | Other Details: ").append(otherDetails)
                        .append(" | Symptoms: ").append(symptomsList).append("\n");
            }

            contextBuilder.append("\nCAR PARTS AND THEIR SYMPTOMS:\n");
            for (QueryDocumentSnapshot doc : carPartsSnapshot) {
                String name = doc.getString("name");
                List<String> symptomsList = (List<String>) doc.get("malfunctionSymptoms");

                contextBuilder.append("- Part: ").append(name)
                        .append(" | Symptoms: ").append(symptomsList).append("\n");
            }

            contextBuilder.append("------------------------\n");

            String databaseContext = contextBuilder.toString();
            generateDiagnostic(activeCar, userSymptom, databaseContext, loadingPosition);
        });
    }

    private void generateDiagnostic(Car userCar, String userSymptom, String databaseContext, int loadingPosition) {
        String prompt = "You are a top-tier expert auto mechanic. Analyze the following vehicle issue.\n\n" +

                "1. VEHICLE DATA (Crucial to know what components it has):\n" +
                "Make & Model: " + userCar.getCarName() + "\n" +
                "Year: " + userCar.getYear() + "\n" +
                "Engine: " + userCar.getEngine() + "L " + userCar.getFuel() + "\n" +
                "Mileage: " + userCar.getKm() + " km\n\n" +

                "2. SYMPTOMS DESCRIBED BY THE CLIENT:\n\"" + userSymptom + "\"\n\n" +

                "3. MY LOCAL DATABASE (Primary source of truth):\n" + databaseContext + "\n\n" +

                "STRICT INSTRUCTIONS FOR YOUR RESPONSE:\n" +
                "- First, carefully analyze the 'Client's Symptoms' and look for a semantic correlation in 'My Local Database'.\n" +
                "- If you find the culprit part or warning light in My Local Database, use it as your definitive primary diagnostic.\n" +
                "- If the symptoms DO NOT match anything in My Local Database, completely IGNORE the database and use your general mechanical knowledge to provide 3 possible causes and a solid recommendation.\n" +
                "- Output your response directly, professionally, and in a friendly tone. Do not mention the prompt instructions." +
                "- Do not answer to any other request other than car related diagnostics and explain that you are not meant to respond to that type of request.";

        //initializing the ai model (Gemini)
        GenerativeModel gm = new GenerativeModel("gemini-3-flash-preview", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        //packaging the prompt
        Content content = new Content.Builder().addText(prompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiAnswer = result.getText();
                runOnUiThread(() -> {
                    updateChatMessage(loadingPosition, aiAnswer);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    String errMsg = "AI Diagnostic Error: " + t.getMessage();
                    updateChatMessage(loadingPosition, errMsg);
                });
            }
        }, executor);
    }

    private void addMessageToChat(String message, boolean isUser) {
        chatList.add(new ChatMessage(message, isUser));
        adapter.notifyItemInserted(chatList.size() - 1);
        recyclerAiChat.smoothScrollToPosition(chatList.size() - 1); //automatically scrolls to last message
    }
    private void updateChatMessage(int loadingPosition, String newText) {
        if(loadingPosition >= 0 && loadingPosition < chatList.size()) {
            chatList.get(loadingPosition).setMessage(newText);
            adapter.notifyItemChanged(loadingPosition);
            recyclerAiChat.smoothScrollToPosition(loadingPosition);
        }
    }
}