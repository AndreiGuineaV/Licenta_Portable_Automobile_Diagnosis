package com.example.licenta_test.entities;

public class ChatMessage {
    private String message;
    private boolean isUser; //true = user, false = AI

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getUser() {
        return isUser;
    }
}