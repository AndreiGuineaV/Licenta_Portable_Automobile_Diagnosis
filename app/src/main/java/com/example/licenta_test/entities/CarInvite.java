package com.example.licenta_test.entities;

public class CarInvite {
    private String id;
    private String carId;
    private String carName;
    private String senderEmail;
    private String targetUid;
    private String inviteType;
    private long timestamp;

    public CarInvite() {
    }

    public CarInvite(String carId, String carName, String senderEmail, String targetUid, String inviteType, long timestamp) {
        this.carId = carId;
        this.carName = carName;
        this.senderEmail = senderEmail;
        this.targetUid = targetUid;
        this.inviteType = inviteType;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getTargetUid() { return targetUid; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }
    public String getInviteType() { return inviteType; }
    public void setInviteType(String inviteType) { this.inviteType = inviteType; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}