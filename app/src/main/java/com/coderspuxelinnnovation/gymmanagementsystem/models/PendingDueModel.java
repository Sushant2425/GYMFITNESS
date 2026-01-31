package com.coderspuxelinnnovation.gymmanagementsystem.models;

import java.util.concurrent.TimeUnit;

public class PendingDueModel {
    private String name;
    private String phone;
    private String planType;
    private String forMonth;
    private int remaining;
    private int totalFee;
    private int amountPaid;
    private String memberId;
    private String paymentId;
    private long dueDate;
    private String status;
    private String paymentMode;
    private String planId;
    private String planStartDate;
    public PendingDueModel() {
    }

    public PendingDueModel(String name, String phone, String planType, String forMonth,
                           int remaining, String memberId, String paymentId, long dueDate) {
        this.name = name;
        this.phone = phone;
        this.planType = planType;
        this.forMonth = forMonth;
        this.remaining = remaining;
        this.memberId = memberId;
        this.paymentId = paymentId;
        this.dueDate = dueDate;
        this.totalFee = 0;
        this.amountPaid = 0;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getPlanType() {
        return planType;
    }

    public String getForMonth() {
        return forMonth;
    }

    public int getRemaining() {
        return remaining;
    }

    public int getTotalFee() {
        return totalFee;
    }

    public int getAmountPaid() {
        return amountPaid;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public long getDueDate() {
        return dueDate;
    }

    public int getDaysOverdue() {
        long diff = System.currentTimeMillis() - dueDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }

    public String getPaymentStatus() {
        if (remaining == 0) {
            return "PAID";
        } else if (amountPaid > 0) {
            return "PARTIAL";
        } else {
            return "PENDING";
        }
    }

    public int getPaymentPercentage() {
        if (totalFee == 0) return 0;
        return (amountPaid * 100) / totalFee;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public void setForMonth(String forMonth) {
        this.forMonth = forMonth;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public void setTotalFee(int totalFee) {
        this.totalFee = totalFee;
    }

    public void setAmountPaid(int amountPaid) {
        this.amountPaid = amountPaid;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }
    // Add these getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanStartDate() { return planStartDate; }
    public void setPlanStartDate(String planStartDate) { this.planStartDate = planStartDate; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingDueModel that = (PendingDueModel) o;
        return paymentId != null && paymentId.equals(that.paymentId);
    }

    @Override
    public int hashCode() {
        return paymentId != null ? paymentId.hashCode() : 0;
    }
}