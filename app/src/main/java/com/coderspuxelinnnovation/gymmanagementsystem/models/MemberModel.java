package com.coderspuxelinnnovation.gymmanagementsystem.models;

import java.util.Map;

public class MemberModel {
    private Info info;
    private CurrentPlan currentPlan;
    private Map<String, PaymentPlan> payments;
    private String phone;

    // Existing getters and setters
    public Info getInfo() { return info; }
    public void setInfo(Info info) { this.info = info; }

    public CurrentPlan getCurrentPlan() { return currentPlan; }
    public void setCurrentPlan(CurrentPlan currentPlan) { this.currentPlan = currentPlan; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // NEW: Getter and setter for payments
    public Map<String, PaymentPlan> getPayments() { return payments; }
    public void setPayments(Map<String, PaymentPlan> payments) { this.payments = payments; }

    // Existing inner classes
    public static class Info {
        private String name, phone, email, gender, joinDate, status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getJoinDate() { return joinDate; }
        public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class CurrentPlan {
        private String planType, startDate, endDate, status, planId;
        private int totalFee;
        private int amountPaid;  // ADD THIS!
        private int remaining;   // ADD THIS!

        // Getters and setters...
        public int getAmountPaid() { return amountPaid; }
        public void setAmountPaid(int amountPaid) { this.amountPaid = amountPaid; }

        public int getRemaining() { return remaining; }
        public void setRemaining(int remaining) { this.remaining = remaining; }

        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }

        public int getTotalFee() { return totalFee; }
        public void setTotalFee(int totalFee) { this.totalFee = totalFee; }
    }

    // NEW: PaymentPlan class for old plans
    public static class PaymentPlan {
        private String planEndDate;

        private String planId;
        private String planStartDate;
        private int totalFee;
        private int amountPaid;

        private int remaining;
        private String status;
        private long date;
        private String mode;
        private String paymentId;
        private String forMonth;
        private long lastPaymentDate;  // ADD THIS
        private String lastPaymentMode; // ADD THIS
        private Map<String, PaymentHistory> paymentHistory;
        public String getPlanEndDate() { return planEndDate; }
        public void setPlanEndDate(String planEndDate) { this.planEndDate = planEndDate; }

        // Getters and Setters
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }


        public long getLastPaymentDate() { return lastPaymentDate; }
        public void setLastPaymentDate(long lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

        public String getLastPaymentMode() { return lastPaymentMode; }
        public void setLastPaymentMode(String lastPaymentMode) { this.lastPaymentMode = lastPaymentMode; }
        public String getPlanStartDate() { return planStartDate; }
        public void setPlanStartDate(String planStartDate) { this.planStartDate = planStartDate; }

        public int getTotalFee() { return totalFee; }
        public void setTotalFee(int totalFee) { this.totalFee = totalFee; }

        public int getAmountPaid() { return amountPaid; }
        public void setAmountPaid(int amountPaid) { this.amountPaid = amountPaid; }

        public int getRemaining() { return remaining; }
        public void setRemaining(int remaining) { this.remaining = remaining; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getDate() { return date; }
        public void setDate(long date) { this.date = date; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getForMonth() { return forMonth; }
        public void setForMonth(String forMonth) { this.forMonth = forMonth; }

        public Map<String, PaymentHistory> getPaymentHistory() { return paymentHistory; }
        public void setPaymentHistory(Map<String, PaymentHistory> paymentHistory) { this.paymentHistory = paymentHistory; }
    }

    // NEW: PaymentHistory class for individual payment entries
    public static class PaymentHistory {
        private String transactionId;
        private int amount;
        private long date;
        private String notes;
        private String paymentMode;
        private int remainingAfter;

        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }

        public long getDate() { return date; }
        public void setDate(long date) { this.date = date; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getPaymentMode() { return paymentMode; }
        public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

        public int getRemainingAfter() { return remainingAfter; }
        public void setRemainingAfter(int remainingAfter) { this.remainingAfter = remainingAfter; }
    }
}