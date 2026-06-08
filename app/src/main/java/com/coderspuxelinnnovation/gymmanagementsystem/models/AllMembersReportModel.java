package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class AllMembersReportModel {
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String joinDate;
    private String memberStatus;
    private String planType;
    private String startDate;
    private String endDate;
    private int totalFee;
    private int remainingAmount;
    private String planStatus;
    private int daysRemaining;

    // Getters and Setters
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

    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getTotalFee() { return totalFee; }
    public void setTotalFee(int totalFee) { this.totalFee = totalFee; }

    public int getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(int remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getPlanStatus() { return planStatus; }
    public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }
}