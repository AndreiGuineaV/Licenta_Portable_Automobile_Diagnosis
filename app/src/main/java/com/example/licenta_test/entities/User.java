package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

//A CLASS TO EXPLAIN THE USER ATTRIBUTES
public class User {
    @DocumentId
    private String id;
    private String username;
    private String email;
    // The password will strictly stay in Firebase Authentication for security purposes
    public User(){};

    public User(String username, String email) {
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
