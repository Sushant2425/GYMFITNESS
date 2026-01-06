package com.coderspuxelinnnovation.gymmanagementsystem.models;


public class MemberModel {
    private Info info;
    private CurrentPlan currentPlan;
    private String phone;

    public Info getInfo() { return info; }
    public void setInfo(Info info) { this.info = info; }

    public CurrentPlan getCurrentPlan() { return currentPlan; }
    public void setCurrentPlan(CurrentPlan currentPlan) { this.currentPlan = currentPlan; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public static class Info {
        private String name, phone, email, gender, joinDate, status;

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getGender() { return gender; }
        public String getJoinDate() { return joinDate; }
        public String getStatus() { return status; }
    }

    public static class CurrentPlan {
        private String planType, startDate, endDate, status;
        private int totalFee;

        public String getPlanType() { return planType; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public int getTotalFee() { return totalFee; }
    }
}
