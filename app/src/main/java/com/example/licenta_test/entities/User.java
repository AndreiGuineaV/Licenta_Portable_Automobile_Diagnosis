package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

//A CLASS TO EXPLAIN THE USER ATTRIBUTES
public class User {
    private String uid;
    private String username;
    private String email;
    private long createdAt;
    // The password will strictly stay in Firebase Authentication for security purposes
    public User(){};

    public User(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String id) {
        this.uid = id;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
