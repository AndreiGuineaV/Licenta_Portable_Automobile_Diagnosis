package com.example.licenta_test.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licenta_test.R;
import com.example.licenta_test.activities.HomePageActivity;
import com.example.licenta_test.activities.LoginActivity;
import com.example.licenta_test.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etRegisterUsername);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void performRegistration() {
        // Trim to remove any leading or trailing spaces
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required!");
            etUsername.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required!");
            etEmail.requestFocus();
            return;
        }

        // Verifying if the email is valid with a built in regex
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please provide a valid email!");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required!");
            etPassword.requestFocus();
            return;
        }

        // Firebase Authentication requires a minimum password length of 6 characters
        if (password.length() < 6) {
            etPassword.setError("Min password length should be 6 characters!");
            etPassword.requestFocus();
            return;
        }

        // Disabling the button to avoid multiple clicks
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account...");

        //
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Gets the user ID created by Firebase
                    String userId = authResult.getUser().getUid();

                    User newUser = new User(userId, username, email);

                    // Save the new user in Firestore "Users" collection
                    db.collection("Users").document(userId).set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();

                                SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
                                prefs.edit().putString("username", username).apply();

                                // Redirecting to the home page
                                startActivity(new Intent(RegisterActivity.this, HomePageActivity.class));
                                finish(); // Closing the registration activity
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("Sign Up");
                                Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Sign Up");

                    if (e instanceof FirebaseAuthUserCollisionException) {
                        etEmail.setError("Email is already registered!");
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}