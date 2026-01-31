package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class GymMember {
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String joinDate;
    private String status;
    private PlanDetails currentPlan;
    private int outstandingBalance;
    private long daysExpired;

    public GymMember() {
        // Required empty constructor for Firebase
    }

    public GymMember(String name, String phone, String email, String gender, 
                     String joinDate, String status, PlanDetails currentPlan) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.gender = gender;
        this.joinDate = joinDate;
        this.status = status;
        this.currentPlan = currentPlan;
        this.outstandingBalance = 0;
        this.daysExpired = 0;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public String getStatus() {
        return status;
    }

    public PlanDetails getCurrentPlan() {
        return currentPlan;
    }

    public int getOutstandingBalance() {
        return outstandingBalance;
    }

    public long getDaysExpired() {
        return daysExpired;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCurrentPlan(PlanDetails currentPlan) {
        this.currentPlan = currentPlan;
    }

    public void setOutstandingBalance(int outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public void setDaysExpired(long daysExpired) {
        this.daysExpired = daysExpired;
    }

    // Helper method to check if member has dues
    public boolean hasPendingDues() {
        return outstandingBalance > 0;
    }

    // Helper method to check if plan is expired
    public boolean isPlanExpired() {
        if (currentPlan == null) return true;
        return "EXPIRED".equals(currentPlan.getStatus()) || daysExpired > 0;
    }

    /**
     * Inner class for Plan Details
     */
    public static class PlanDetails {
        private String planId;
        private String planType;
        private String startDate;
        private String endDate;
        private int totalFee;
        private String status;

        public PlanDetails() {
            // Required empty constructor for Firebase
        }

        public PlanDetails(String planId, String planType, String startDate, 
                          String endDate, int totalFee, String status) {
            this.planId = planId;
            this.planType = planType;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalFee = totalFee;
            this.status = status;
        }

        // Getters
        public String getPlanId() {
            return planId;
        }

        public String getPlanType() {
            return planType;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public int getTotalFee() {
            return totalFee;
        }

        public String getStatus() {
            return status;
        }

        // Setters
        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public void setPlanType(String planType) {
            this.planType = planType;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public void setTotalFee(int totalFee) {
            this.totalFee = totalFee;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}