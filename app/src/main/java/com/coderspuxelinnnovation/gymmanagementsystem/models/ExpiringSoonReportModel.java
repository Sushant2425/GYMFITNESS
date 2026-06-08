package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class ExpiringSoonReportModel {
    private String memberName;
    private String memberPhone;
    private String memberEmail;
    private String planType;
    private String planId;
    private String startDate;
    private String endDate;
    private int daysRemaining;
    private int totalFee;
    private int remainingAmount;

    // Getters and Setters
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }

    public int getTotalFee() { return totalFee; }
    public void setTotalFee(int totalFee) { this.totalFee = totalFee; }

    public int getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(int remainingAmount) { this.remainingAmount = remainingAmount; }
}