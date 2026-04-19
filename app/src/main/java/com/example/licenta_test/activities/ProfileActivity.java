package com.example.licenta_test.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.licenta_test.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView tvUserName, tvUserEmail, btnChangeUsername, btnChangePassword, btnDeleteAcc, tvDate;
    Button btnLogout;
    FirebaseAuth auth;
    FirebaseFirestore db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); //for later

        tvDate = findViewById(R.id.tvCreatedAt);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnBack = findViewById(R.id.btnBack);
        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAcc = findViewById(R.id.btnDeleteAccount);

//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null && currentUser.getDisplayName() != null) {
//            tvUserName.setText(currentUser.getDisplayName());
//        }
        loadUserData();

        btnBack.setOnClickListener(v->{
            finish();
        });
        btnLogout.setOnClickListener(v-> logoutUser());
        btnDeleteAcc.setOnClickListener(v-> {
            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
            deleteDialog.setTitle("Account Deletion")
                    .setMessage("Are you sure you want to delete your account?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> deleteUserAccount())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();

        });
        btnChangeUsername.setOnClickListener(v->showChangeUsernameDialog());
        btnChangePassword.setOnClickListener(v->showChangePasswordDialog());

    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText oldPassInput = new EditText(this);
        oldPassInput.setHint("Current Password");
        oldPassInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(oldPassInput);

        final EditText newPassInput = new EditText(this);
        newPassInput.setHint("New Password (min 6 chars)");
        newPassInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPassInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = oldPassInput.getText().toString().trim();
            String newPassword = newPassInput.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your current password!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            reauthenticateAndUpdatePassword(currentPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndUpdatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // Creating an authentication credential with the current password
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            // Sending the authentication request
            user.reauthenticate(credential).addOnSuccessListener(aVoid -> {

                // Now the password can be changed as normal
                user.updatePassword(newPassword).addOnSuccessListener(aVoid1 -> {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

            }).addOnFailureListener(e -> {
                // Authentication failed (probably wrong old password)
                Toast.makeText(this, "Authentication failed! Current password is incorrect.", Toast.LENGTH_LONG).show();
            });
        }
    }
    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            tvUserEmail.setText(user.getEmail());

            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            String cachedUsername = prefs.getString("username", "User");
            tvUserName.setText(cachedUsername);

            FirebaseUserMetadata metadata = user.getMetadata();
            if (metadata != null) {
                long creationTimestamp = metadata.getCreationTimestamp();
                tvDate.setText("Account created at: " + formatTimestampToDate(creationTimestamp));
            }
        }
    }
    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Username");
        final EditText input = new EditText(this);
        input.setHint("Enter new username");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                updateUsernameInDatabase(newUsername);
            } else {
                Toast.makeText(this, "Username cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUsernameInDatabase(String newUsername) {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build();

            user.updateProfile(profileUpdates).addOnSuccessListener(aVoid -> {

                db.collection("Users").document(user.getUid())
                        .update("username", newUsername)
                        .addOnSuccessListener(aVoid2 -> {

                            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
                            prefs.edit().putString("username", newUsername).apply();

                            tvUserName.setText(newUsername);
                            Toast.makeText(this, "Username updated!", Toast.LENGTH_SHORT).show();

                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (user != null) {
            String uid = user.getUid();

            db.collection("Users").document(uid).delete().addOnSuccessListener(aVoid -> {

                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();

                        Toast.makeText(this, "Contul a fost șters definitiv.", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Eroare la ștergerea contului: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }
    private String formatTimestampToDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        return formatter.format(date);
    }
}