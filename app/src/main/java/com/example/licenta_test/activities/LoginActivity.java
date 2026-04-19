package com.example.licenta_test.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licenta_test.R;
import com.example.licenta_test.activities.MyGarageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    CheckBox cbKeepSignedIn;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean keepLoggedIn = prefs.getBoolean("keepLoggedIn", false);

        if (auth.getCurrentUser() != null) {
            if (keepLoggedIn) {
                startActivity(new Intent(LoginActivity.this, HomePageActivity.class));
                finish();
                return;
            } else {
                auth.signOut();
            }
        }

        setContentView(R.layout.activity_login);

        cbKeepSignedIn = findViewById(R.id.cbKeepSignedIn);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void performLogin() {
        // Get user input and remove leading/trailing spaces
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required!");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email!");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required!");
            etPassword.requestFocus();
            return;
        }

        // Disable button to prevent multiple clicks
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String username = documentSnapshot.getString("username");
                                SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
                                prefs.edit().putString("username", username).apply();

                                Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, HomePageActivity.class));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    // Login failed. Re-enable the button
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log In");

                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(LoginActivity.this, "Invalid email or password.", Toast.LENGTH_LONG).show();
                        etPassword.setError("Check your credentials.");
                        etEmail.setError("Check your credentials.");
                        etPassword.requestFocus();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        boolean shouldKeepSignedIn = cbKeepSignedIn.isChecked();
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("keepLoggedIn", shouldKeepSignedIn).apply();
    }
}