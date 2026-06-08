package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class PaymentReportModel {
    private String memberName;
    private String memberPhone;
    private int amount;
    private String paymentMode;
    private long paymentDate;
    private String forMonth;
    private String planType;
    private String status;

    // Getters and Setters
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public long getPaymentDate() { return paymentDate; }
    public void setPaymentDate(long paymentDate) { this.paymentDate = paymentDate; }

    public String getForMonth() { return forMonth; }
    public void setForMonth(String forMonth) { this.forMonth = forMonth; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}