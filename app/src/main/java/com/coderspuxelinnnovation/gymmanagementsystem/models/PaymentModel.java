package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class PaymentModel {
    public String paymentId, mode, status;
    public Long date;           // Timestamp (1767631081334)
    public Double amountPaid;   // ✅ FIXED: Number (500) → Double
    public Double remaining;    // Number → Double
    public Double totalFee;     // Number → Double
    public String planType; // ⭐ ADD THIS


    public PaymentModel() {}  // Firebase needs empty constructor

    // Getters only (no setters needed for display)
    public String getPaymentId() { return paymentId; }
    public String getAmountPaid() { return amountPaid != null ? amountPaid.toString() : "0"; }
    public Long getDate() { return date; }
    public String getMode() { return mode; }
    public String getStatus() { return status; }
    public double getRemaining() { return remaining != null ? remaining : 0; }
    public double getTotalFee() { return totalFee != null ? totalFee : 0; }
}
