package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class Payment {
    private String paymentId;
    private double amountPaid;
    private double totalFee;
    private double remaining;
    private String mode; // Cash, UPI, Card
    private long date;
    private String status; // PAID, PARTIAL
    private PlanReference planReference;

    public static class PlanReference {
        private String planType;
        private String startDate;
        private String endDate;

        public PlanReference() {}

        public PlanReference(String planType, String startDate, String endDate) {
            this.planType = planType;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters and Setters
        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    public Payment() {}

    public Payment(String paymentId, double amountPaid, double totalFee, double remaining, 
                   String mode, long date, String status, PlanReference planReference) {
        this.paymentId = paymentId;
        this.amountPaid = amountPaid;
        this.totalFee = totalFee;
        this.remaining = remaining;
        this.mode = mode;
        this.date = date;
        this.status = status;
        this.planReference = planReference;
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public double getTotalFee() { return totalFee; }
    public void setTotalFee(double totalFee) { this.totalFee = totalFee; }
    public double getRemaining() { return remaining; }
    public void setRemaining(double remaining) { this.remaining = remaining; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public PlanReference getPlanReference() { return planReference; }
    public void setPlanReference(PlanReference planReference) { this.planReference = planReference; }
}