package com.example.licenta_test.entities;

import java.io.Serializable;

public class JournalEntry implements Serializable {
    private String id;
    private String type; // "DIAGNOSTIC", "REPAIR", "MAINTENANCE", "DOCUMENT"
    private String title; // ex: "Oil Change", "RCA newed"
    private String description; // Car details
    private int mileageAtLog; // Mileage at that time
    private double cost; // Repair cost (0 if it's just a diagnosis/document)
    private long timestamp;

    public JournalEntry() {}

    public JournalEntry(String type, String title, String description, int mileageAtLog, double cost, long timestamp) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.mileageAtLog = mileageAtLog;
        this.cost = cost;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMileageAtLog() { return mileageAtLog; }
    public void setMileageAtLog(int mileageAtLog) { this.mileageAtLog = mileageAtLog; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}