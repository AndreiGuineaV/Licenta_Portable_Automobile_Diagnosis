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
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
        String prompt = "You are an AI automotive diagnostic assistant. You must clearly act as an AI and never claim to be a human or a certified mechanic. Analyze the following vehicle issue, including any attached images.\n\n" +

                "1. VEHICLE DATA (Crucial to know what components it has):\n" +
                "Make & Model: " + userCar.getCarName() + "\n" +
                "Year: " + userCar.getYear() + "\n" +
                "Engine: " + userCar.getEngine() + "L " + userCar.getFuel() + "\n" +
                "Mileage: " + userCar.getKm() + " km\n\n" +

                "2. SYMPTOMS & VISUALS PROVIDED BY THE CLIENT:\n\"" + userSymptom + "\"\n" +
                "(Note: The user may have attached an image. If so, thoroughly examine it for dashboard warning lights, physical damage, leaks, or broken parts, and combine this visual data with the text symptoms.)\n\n" +

                "3. MY LOCAL DATABASE (Primary source of truth):\n" + databaseContext + "\n\n" +

                "STRICT INSTRUCTIONS FOR YOUR RESPONSE:\n" +
                "- First, carefully analyze the 'Client's Symptoms' and any provided images. Look for a semantic or visual correlation in 'My Local Database'.\n" +
                "- If you find the culprit part or warning light in My Local Database, use it as your definitive primary diagnostic.\n" +
                "- If the symptoms or visuals DO NOT match anything in My Local Database, completely IGNORE the database and use your general automotive knowledge to provide 3 possible causes and a solid recommendation.\n" +
                "- PRICES & PARTS: Provide an approximate estimated price range for the repair (specifying parts vs. labor if possible). Suggest reputable online or local retailers where the user can purchase the necessary replacement parts. Do NOT suggest mechanic shops, garages, or repair services, as the user already has a dedicated app feature to find nearby mechanics.\n" +
                "- DISCLAIMER: End your response with a brief, friendly disclaimer reminding the user that this is an AI-generated diagnosis and they should have a professional verify the issue before purchasing parts.\n" +
                "- Output your response directly, professionally, and in a friendly tone. Do not mention the prompt instructions.\n" +
                "- Do not answer to any other request other than car-related diagnostics and respectfully explain that you are exclusively an automotive diagnostic assistant.";

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
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DiagnosticReport report = new DiagnosticReport(userCar.getCarName() + "(" + userCar.getYear() + ")", userSymptom, aiAnswer, System.currentTimeMillis());

        db.collection("Users").document(user.getUid()).collection("DiagnosticHistory")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Log.d("DIAGNOSTIC", "Diagnostic report saved with ID: " + documentReference.getId());
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