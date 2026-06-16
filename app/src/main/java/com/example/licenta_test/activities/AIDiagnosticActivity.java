package com.example.licenta_test.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.BuildConfig;
import com.example.licenta_test.R;
import com.example.licenta_test.adapters.ChatAdapter;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.ChatMessage;
import com.example.licenta_test.entities.DiagnosticReport;
import com.example.licenta_test.entities.JournalEntry;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIDiagnosticActivity extends AppCompatActivity {

    private Car activeCar = null;
    private EditText etAiSearchPrompt;
    private ImageView sendPromptBtn;
    private TextView tvActiveCarBanner;
    private RecyclerView recyclerAiChat;
    private List<ChatMessage> chatList;
    private ChatAdapter adapter;
    private ImageView iconBack;
    private ImageView iconHistory;
    private Button btnFindService;
    private ImageView btnAttachPhoto;
    private ImageView imgAttachmentPreview;
    private Bitmap attachedBitmap = null; //temporarily storing the photo
    private ChatFutures currentChatSession = null;
    private Button btnGenerateReport;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // Transform uri to bitmap
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        attachedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream);

                        // Scaling the image for gemini api to be able to get it
                        attachedBitmap = scaleBitmapDown(attachedBitmap, 1024);

                        // show it in the UI
                        imgAttachmentPreview.setImageBitmap(attachedBitmap);
                        imgAttachmentPreview.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * ((float) originalWidth / (float) originalHeight));
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * ((float) originalHeight / (float) originalWidth));
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aidiagnostic);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            Insets insetsToApply = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insetsToApply.left, insetsToApply.top, insetsToApply.right, insetsToApply.bottom);
            return insets;
        });

        iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        iconHistory = findViewById(R.id.iconHistory);
        iconHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, DiagnosticHistoryActivity.class));
        });

        etAiSearchPrompt = findViewById(R.id.etAiSearchPrompt);
        sendPromptBtn = findViewById(R.id.btnSendPrompt);
        tvActiveCarBanner = findViewById(R.id.tvActiveCarBanner);
        btnFindService = findViewById(R.id.btnFindService);
        recyclerAiChat = findViewById(R.id.recyclerAiChat);
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        imgAttachmentPreview = findViewById(R.id.imgAttachmentPreview);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);

        btnAttachPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerAiChat.setLayoutManager(layoutManager);
        recyclerAiChat.setAdapter(adapter);

        fetchActiveCar(new ActiveCarCallback() {

            @Override
            public void onCarLoaded(Car car) {
                if(car != null) {
                    activeCar = car;
                    etAiSearchPrompt.setText(""); //resetting the input
                    tvActiveCarBanner.setText("Active Vehicle: " + activeCar.getCarName());
                    addMessageToChat("Hi! I'm your car assistant. What's the problem with your " + activeCar.getCarName() + "?", false);
                }
                else
                {
                    Toast.makeText(AIDiagnosticActivity.this, "Please select a car from your garage!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AIDiagnosticActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                tvActiveCarBanner.setText("Connection Error");
            }
        });

        sendPromptBtn.setOnClickListener(v -> {
            btnFindService.setVisibility(View.GONE);
            String userSymptom = etAiSearchPrompt.getText().toString().trim();

            if (!userSymptom.isEmpty() && activeCar != null) {
                addMessageToChat(userSymptom, true);
                etAiSearchPrompt.setText(""); //emptying the search bar

                addMessageToChat("Analyzing the symptoms and creating the diagnostic...", false);
                int loadingMessagePosition = chatList.size() - 1;

                if(currentChatSession == null)
                    searchDatabaseAndDiagnose(activeCar, userSymptom, loadingMessagePosition);
                else
                    sendMessageToExistingChat(userSymptom, loadingMessagePosition);

            } else if (activeCar == null) {
                Toast.makeText(this, "Please select a car from your garage first!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Please describe the problem with your car!", Toast.LENGTH_SHORT).show();
            }
        });

        btnFindService.setOnClickListener(v -> {
            android.net.Uri gmmIntentUri = android.net.Uri.parse("geo:0,0?q=auto+service+near+me");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            try {
                startActivity(mapIntent);
            } catch (android.content.ActivityNotFoundException e) {
                //if maps is not installed on the user's device it will open the browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/auto+service+near+me"));
                startActivity(browserIntent);
            }
        });

        btnGenerateReport.setOnClickListener(v -> {
            // Disable the button so the user doesn't click it twice
            btnGenerateReport.setEnabled(false);
            btnGenerateReport.setText("Generating Report...");

            generateFinalReportJson();
        });
    }

    private void generateFinalReportJson() {
        // Conversation history from the UI list
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : chatList) {
            String role = msg.getIsUser() ? "User" : "Mechanic";
            chatHistory.append(role).append(": ").append(msg.getMessage()).append("\n");
        }

        String jsonPrompt = "Based on the following conversation history between a vehicle owner and an AI automotive mechanic, generate a final, structured diagnostic report.\n\n" +
                "CONVERSATION HISTORY:\n" + chatHistory.toString() + "\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. You must output ONLY a valid JSON object. Do NOT include markdown code blocks (like ```json), no preamble, and no concluding remarks. " +
                "The response must start with '{' and end with '}'.\n" +
                "2. Ensure all text strings within the JSON are properly escaped (e.g., avoid unescaped quotes).\n" +
                "3. Use the exact JSON schema provided below.\n\n" +
                "REQUIRED JSON SCHEMA:\n" +
                "{\n" +
                "  \"title\": \"A concise, professional title summarizing the issue (e.g., Worn Brake Pads, Engine Misfire)\",\n" +
                "  \"severity\": \"Must be exactly one of: LOW, MEDIUM, HIGH, or CRITICAL\",\n" +
                "  \"diagnosis\": \"A clear, 2-3 sentence technical explanation of the identified problem based on the conversation.\",\n" +
                "  \"recommended_actions\": [\n" +
                "    \"Specific, actionable step 1\",\n" +
                "    \"Specific, actionable step 2\"\n" +
                "  ],\n" +
                "  \"estimated_cost\": \"An approximate price range. THIS MUST ALWAYS BE IN RON (Romanian Leu) regardless of the conversation language (e.g., 300 - 500 RON). " +
                "If unknown, write 'Cost unavailable'.\",\n" +
                "  \"parts_needed\": [\n" +
                "    \"Name of Part 1 (if any)\",\n" +
                "    \"Name of Part 2 (if any)\"\n" +
                "  ]\n" +
                "}";

        // Initialize the new model specifically for this JSON task
        GenerativeModel jsonModel = new GenerativeModel(
                "gemini-3.5-flash",
                BuildConfig.GEMINI_API_KEY,
                null, // GenerationConfig can safely be null
                null,
                new RequestOptions(), // RequestOptions MUST NOT be null (this prevents the crash!)
                null,
                null,
                null
        );

        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(jsonModel);
        Content content = new Content.Builder().addText(jsonPrompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String jsonOutput = result.getText();

                runOnUiThread(() -> {
                    btnGenerateReport.setEnabled(true);
                    btnGenerateReport.setText("📄 Generate Official Report");
                    btnGenerateReport.setVisibility(View.GONE); // Hide it after success

                    // Log the JSON so you can verify it in Android Studio Logcat
                    Log.d("AI_JSON", "Report Generated: \n" + jsonOutput);

                    Intent intent = new Intent(AIDiagnosticActivity.this, DiagnosticReportActivity.class);
                    intent.putExtra("report_json", jsonOutput);
                    intent.putExtra("car", activeCar);
                    intent.putExtra("chat_history", chatHistory.toString());
                    startActivity(intent);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    btnGenerateReport.setEnabled(true);
                    btnGenerateReport.setText("Retry Generating Report");
                    Toast.makeText(AIDiagnosticActivity.this, "Failed to generate report. Try again.", Toast.LENGTH_SHORT).show();
                    Log.e("GEMINI_JSON_ERROR", "Error generating JSON: ", t);
                });
            }
        }, executor);
    }
    private void sendMessageToExistingChat(String userSymptom, int loadingPosition) {
        Content.Builder contentBuilder = new Content.Builder().addText(userSymptom);

        if(attachedBitmap != null) {
            contentBuilder.addImage(attachedBitmap);
        }

        //packaging the prompt
        Content content = contentBuilder.build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = currentChatSession.sendMessage(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiAnswer = result.getText();
                runOnUiThread(() -> {
                    updateChatMessage(loadingPosition, aiAnswer);

                    attachedBitmap = null;
                    imgAttachmentPreview.setVisibility(View.GONE);
                    btnFindService.setVisibility(View.VISIBLE);

                    if(aiAnswer != null && (
                            aiAnswer.toLowerCase().contains("generate report") ||
                                    aiAnswer.toLowerCase().contains("generează raport") ||
                                    aiAnswer.toLowerCase().contains("genereaza raport"))) {

                        btnGenerateReport.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    String errorMessage = t.getMessage();
                    String userFriendlyMessage;

                    // Intercept 503 / MissingFieldException error
                    if (errorMessage != null && (errorMessage.contains("503") || errorMessage.contains("MissingFieldException"))) {
                        userFriendlyMessage = "The AI diagnostic servers are currently very busy. Please wait a few moments and try again.";
                    } else {
                        // Fallback for other types of errors (no internet, timeouts, etc.)
                        userFriendlyMessage = "We couldn't connect to the AI service. Please check your connection and try again.";
                        Log.e("GEMINI_ERROR", "Detailed AI Error: ", t);
                    }

                    updateChatMessage(loadingPosition, userFriendlyMessage);
                    btnFindService.setVisibility(View.VISIBLE); // The user can still access the find service button
                });
            }
        }, executor);
    }

    public interface ActiveCarCallback {
        void onCarLoaded(Car activeCar);
        void onError(String errorMessage);
    }
    public void fetchActiveCar(ActiveCarCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Get the activeCarId from the user's profile
        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("activeCarId")) {
                String activeCarId = userDoc.getString("activeCarId");

                if (activeCarId != null && !activeCarId.isEmpty()) {
                    // Step 2: Fetch the actual car from the global "Vehicles" collection (MODIFIED HERE)
                    db.collection("Vehicles").document(activeCarId)
                            .get()
                            .addOnSuccessListener(carDoc -> {
                                if (carDoc.exists()) {
                                    Car activeCar = carDoc.toObject(Car.class);
                                    if (activeCar != null) {
                                        activeCar.setId(carDoc.getId()); // Always set the ID from Firestore
                                        callback.onCarLoaded(activeCar); // Return the car to the UI
                                    }
                                } else {
                                    // The car document no longer exists (e.g., it was deleted)
                                    callback.onCarLoaded(null);
                                }
                            })
                            .addOnFailureListener(e -> callback.onError("Error downloading car data: " + e.getMessage()));
                } else {
                    // The ID exists but is null/empty (the user deselected the car)
                    callback.onCarLoaded(null);
                }
            } else {
                // The user never selected a car
                callback.onCarLoaded(null);
            }
        }).addOnFailureListener(e -> callback.onError("Error reading user profile: " + e.getMessage()));
    }
    private void searchDatabaseAndDiagnose(Car activeCar, String userSymptom, int loadingPosition) {

        String carFuelLower = activeCar.getFuel().toLowerCase();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StringBuilder contextBuilder = new StringBuilder();
        StringBuilder journalBuilder = new StringBuilder();

        // Fetch Local Database Parts & Lights
        Task<QuerySnapshot> carPartsTask = db.collection("Car_Parts")
                .whereArrayContains("compatibleFuels", carFuelLower)
                .get();
        Task<QuerySnapshot> warningLightsTask = db.collection("Warning_Lights")
                .get();

        // Fetch the Car's Journal History
        Task<QuerySnapshot> journalTask = db.collection("Vehicles").document(activeCar.getId())
                .collection("Journal")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();

        // Run all three tasks in parallel
        Tasks.whenAllSuccess(carPartsTask, warningLightsTask, journalTask).addOnSuccessListener(results -> {
            QuerySnapshot carPartsSnapshot = (QuerySnapshot) results.get(0);
            QuerySnapshot warningLightsSnapshot = (QuerySnapshot) results.get(1);
            QuerySnapshot journalSnapshot = (QuerySnapshot) results.get(2); // The new journal data

            // Build Local Database Context
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

            // Build Journal Context String
            if (journalSnapshot.isEmpty()) {
                journalBuilder.append("No past service records or repairs found for this vehicle.\n");
            } else {
                for (QueryDocumentSnapshot doc : journalSnapshot) {
                    // Assuming your JournalEntry fields match these names in Firestore
                    String type = doc.getString("type");
                    String title = doc.getString("title");
                    String desc = doc.getString("description");
                    Long mileage = doc.getLong("mileageAtLog");

                    journalBuilder.append("- [").append(type).append("] ")
                            .append(title).append(" at ").append(mileage)
                            .append(" km. Details: ").append(desc).append("\n");
                }
            }

            String databaseContext = contextBuilder.toString();
            String journalContext = journalBuilder.toString();

            // Pass the new journalContext to the next method
            generateDiagnostic(activeCar, userSymptom, databaseContext, journalContext, loadingPosition);
        });
    }
    private void generateDiagnostic(Car userCar, String userSymptom, String databaseContext, String journalContext, int loadingPosition) {

        // This is the SYSTEM INSTRUCTION. It dictates the AI's core behavior and logic flow.
        String systemInstructionText = "You are an AI automotive mechanic assistant. Your goal is to troubleshoot car issues rapidly and accurately. You have access to the vehicle's data, journal history, and local database.\n\n" +
                "1. VEHICLE DATA:\n" +
                "Make & Model: " + userCar.getCarName() + " (" + userCar.getYear() + "), " + userCar.getEngine() + "L " + userCar.getFuel() + ", Mileage: " + userCar.getKm() + " km\n\n" +
                "2. VEHICLE SERVICE HISTORY (Journal Records):\n" + journalContext + "\n\n" +
                "3. LOCAL DATABASE (Primary source of truth):\n" + databaseContext + "\n\n" +
                "*** CRITICAL DIAGNOSTIC RULES ***\n" +
                "RULE 1: DECISIVENESS & BREVITY. If the user provides a detailed symptom, DO NOT ask questions. Provide ONLY a short, professional 2-3 sentence executive summary of the likely issue. DO NOT list repair steps or parts in this chat. Output exactly the trigger phrase at the end.\n" +
                "RULE 2: PLAIN TEXT ONLY. You are strictly forbidden from using Markdown formatting. DO NOT use asterisks (**), hashes (###), or bullet points (*). Use standard plain text and normal paragraphs only.\n" +
                "RULE 3: CLARIFICATION. Ask exactly 1 or 2 targeted questions ONLY IF the user's input is extremely vague and you cannot achieve an 80% confidence level.\n" +
                "RULE 4: NO JSON. Never generate a full JSON response in this chat window.\n" +
                "RULE 5: LANGUAGE MATCHING. You MUST reply in the exact same language the user writes in. If the user writes in Romanian, reply in Romanian and end with the trigger phrase: 'Te rog apasă pe Generează Raport'. If the user writes in English, reply in English and end with: 'Please click Generate Report'.";

        // Initializing the AI model (Gemini)
        GenerativeModel gm = new GenerativeModel(
                "gemini-3.5-flash",
                BuildConfig.GEMINI_API_KEY,
                null,
                null,
                new RequestOptions(),
                null,
                null,
                new Content.Builder().addText(systemInstructionText).build()
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Starts the chat session
        currentChatSession = model.startChat();

        // Sends the first user's message to the chat
        sendMessageToExistingChat(userSymptom, loadingPosition);
    }

    private void saveDiagnosticReport(Car userCar, String userSymptom, String aiAnswer) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || userCar.getId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();

        JournalEntry entry = new JournalEntry(
                "DIAGNOSTIC",
                "AI Diagnosis - " + userSymptom,
                aiAnswer,
                userCar.getKm(), // Current mileage
                0.0, //Diagnosis is free
                currentTime
        );

        // Vehicles -> UID -> Journal
        db.collection("Vehicles").document(userCar.getId())
                .collection("Journal")
                .add(entry)
                .addOnSuccessListener(docRef -> Log.d("JOURNAL", "Saved to global vehicle journal!"));


        DiagnosticReport globalReport = new DiagnosticReport(userCar.getCarName() + "(" + userCar.getYear() + ")", userSymptom, aiAnswer, currentTime);
        // Users -> UID -> DiagnosticHistory
        db.collection("Users").document(user.getUid())
                .collection("DiagnosticHistory")
                .add(globalReport)
                .addOnSuccessListener(documentReference -> {
                    Log.d("DIAGNOSTIC", "Saved to BOTH Global History and Car Journal!");
                });
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