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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.BuildConfig;
import com.example.licenta_test.R;
import com.example.licenta_test.adapters.ChatAdapter;
import com.example.licenta_test.additional.GarageStorage;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.ChatMessage;
import com.example.licenta_test.entities.DiagnosticReport;
import com.example.licenta_test.entities.JournalEntry;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

                searchDatabaseAndDiagnose(activeCar, userSymptom, loadingMessagePosition);

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

        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("activeCarId")) {
                String activeCarId = userDoc.getString("activeCarId");

                if (activeCarId != null && !activeCarId.isEmpty()) {
                    // Descărcăm mașina efectivă
                    db.collection("Users").document(uid).collection("Cars").document(activeCarId)
                            .get()
                            .addOnSuccessListener(carDoc -> {
                                if (carDoc.exists()) {
                                    Car activeCar = carDoc.toObject(Car.class);
                                    // BINGO! Returnăm mașina către cine a cerut-o
                                    callback.onCarLoaded(activeCar);
                                } else {
                                    // Documentul mașinii nu mai există (poate a fost ștearsă)
                                    callback.onCarLoaded(null);
                                }
                            })
                            .addOnFailureListener(e -> callback.onError("Eroare la descărcarea mașinii: " + e.getMessage()));
                } else {
                    // ID-ul există dar este null (utilizatorul a deselectat mașina)
                    callback.onCarLoaded(null);
                }
            } else {
                // Nu a selectat niciodată o mașină
                callback.onCarLoaded(null);
            }
        }).addOnFailureListener(e -> callback.onError("Eroare la citirea profilului: " + e.getMessage()));
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
        String prompt = "You are an AI automotive diagnostic assistant. You must clearly act as an AI and never claim to be a human or a certified mechanic. Analyze the user's input, including any attached images.\n\n" +

                "1. VEHICLE DATA:\n" +
                "Make & Model: " + userCar.getCarName() + "\n" +
                "Year: " + userCar.getYear() + "\n" +
                "Engine: " + userCar.getEngine() + "L " + userCar.getFuel() + "\n" +
                "Mileage: " + userCar.getKm() + " km\n\n" +

                "2. USER'S INPUT / SYMPTOMS:\n\"" + userSymptom + "\"\n" +
                "(Note: The user may have attached an image. If so, thoroughly examine it for dashboard warning lights, physical damage, leaks, or broken parts, and combine this visual data with the text symptoms.)\n\n" +

                "3. MY LOCAL DATABASE (Primary source of truth):\n" + databaseContext + "\n\n" +

                "*** CRITICAL BEHAVIOR RULES ***\n" +
                "RULE 1 - OFF-TOPIC QUERIES: If the 'USER'S INPUT' is completely unrelated to cars, vehicles, driving, or automotive parts (e.g., medical questions, programming, cooking, general chat), YOU MUST ABORT THE DIAGNOSTIC PROCESS. Do NOT use the diagnostic template below. Instead, output exactly one short paragraph in a friendly tone stating: 'Hello. I am an AI automotive diagnostic assistant. I am programmed strictly to diagnose vehicle issues and cannot assist with [insert topic, e.g., medical advice]. Please consult the appropriate professional for this matter.' Then stop entirely.\n\n" +

                "RULE 2 - AUTOMOTIVE QUERIES: If the input IS related to a vehicle, you must use the EXACT RESPONSE TEMPLATE below. Do NOT use markdown symbols like asterisks (**), hashes (###), or underscores (_). Use plain text, ALL CAPS for main section titles, and standard dashes (-) for bullet points.\n\n" +
                "RULE 3 - FORMATTING: To make the text readable and spacious, you MUST leave an empty blank line between every single paragraph and between every bullet point.\n\n" +

                "*** EXACT RESPONSE TEMPLATE (FOR VEHICLE ISSUES ONLY) ***\n\n" +

                "DIAGNOSIS:\n\n" +
                "[Start with a friendly greeting. Directly explain the most likely causes based on the symptoms/visuals. Leave empty lines between paragraphs.]\n\n\n" +

                "RECOMMENDED ACTIONS:\n\n" +
                "[Provide clear actions. Leave an empty blank line between each bullet point item.]\n\n\n" +

                "ESTIMATED COSTS & PARTS:\n\n" +
                "[List approximate price ranges for parts. Suggest reputable retailers. Leave an empty blank line between items. Do NOT suggest mechanic shops.]\n\n\n" +

                "DATA SOURCE SUMMARY:\n\n" +
                "[Briefly state what was extracted from the 'Local Database' vs general AI knowledge.]\n\n\n" +

                "DISCLAIMER:\n\n" +
                "[Brief reminder that this is an AI-generated diagnosis.]";

        //initializing the ai model (Gemini)
        GenerativeModel gm = new GenerativeModel("gemini-3-flash-preview", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content.Builder contentBuilder = new Content.Builder().addText(prompt);

        if(attachedBitmap != null) {
            contentBuilder.addImage(attachedBitmap);
        }

        //packaging the prompt
        Content content = contentBuilder.build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiAnswer = result.getText();
                runOnUiThread(() -> {
                    updateChatMessage(loadingPosition, aiAnswer);
                    saveDiagnosticReport(userCar, userSymptom, aiAnswer);

                    attachedBitmap = null;
                    imgAttachmentPreview.setVisibility(View.GONE);

                    btnFindService.setVisibility(View.VISIBLE);
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