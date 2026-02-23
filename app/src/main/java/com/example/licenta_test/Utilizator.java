package com.example.licenta_test;

import com.google.firebase.firestore.DocumentId;

public class Utilizator {
    @DocumentId
    private String id;
    private String username;
    private String email;
    // Parola va sta strict in Firebase Authentication pentru securitate
    public Utilizator(){};

    public Utilizator(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
