package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class Plan {
    private String planId;
    private String planName;  // Must match "planName" in Firebase
    private int fee;          // Must match "fee" in Firebase
    private int duration;     // Must match "duration" in Firebase
    private boolean active;   // Must match "active" in Firebase
    private String createdAt; // Must match "createdAt" in Firebase

    // Default constructor required for Firebase
    public Plan() {
        // Important: Initialize defaults
        this.active = true;
    }

    public Plan(String planName, int fee, int duration) {
        this.planName = planName;
        this.fee = fee;
        this.duration = duration;
        this.active = true;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters - MUST use exact naming convention
    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public int getFee() {
        return fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "planId='" + planId + '\'' +
                ", planName='" + planName + '\'' +
                ", fee=" + fee +
                ", duration=" + duration +
                ", active=" + active +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}