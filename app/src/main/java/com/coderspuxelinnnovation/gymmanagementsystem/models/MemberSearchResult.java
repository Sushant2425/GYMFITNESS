package com.coderspuxelinnnovation.gymmanagementsystem.models;

import com.google.firebase.database.DataSnapshot;

// Add this class INSIDE CollectPaymentActivity.java, above the main class
public class MemberSearchResult {
    public String memberId;
    public String phone;
    public String name;
    public DataSnapshot snapshot;
    
    // Constructor
    public MemberSearchResult() {
        this.memberId = memberId;
        this.phone = phone;
        this.name = name;
        this.snapshot = snapshot;
    }
}