package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class PendingDueReportModel {
    private String memberName;
    private String memberPhone;
    private String memberEmail;
    private int remainingAmount;
    private int totalFee;
    private int amountPaid;
    private String forMonth;
    private String planType;
    private String dueDate;
    private int overdueDays;

    // Getters and Setters
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }

    public int getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(int remainingAmount) { this.remainingAmount = remainingAmount; }

    public int getTotalFee() { return totalFee; }
    public void setTotalFee(int totalFee) { this.totalFee = totalFee; }

    public int getAmountPaid() { return amountPaid; }
    public void setAmountPaid(int amountPaid) { this.amountPaid = amountPaid; }

    public String getForMonth() { return forMonth; }
    public void setForMonth(String forMonth) { this.forMonth = forMonth; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public int getOverdueDays() { return overdueDays; }
    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
}