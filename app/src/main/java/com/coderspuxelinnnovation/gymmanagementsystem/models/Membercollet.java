package com.coderspuxelinnnovation.gymmanagementsystem.models;
import java.util.HashMap;
import java.util.Map;
public class Membercollet {
        private String phone;
        private Info info;
        private CurrentPlan currentPlan;
        private Map<String, Payment> payments;

        public static class Info {
            private String name;
            private String email;
            private String phone;
            private String gender;
            private String joinDate;
            private String status; // ACTIVE, INACTIVE

            public Info() {}

            // Getters and Setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
            public String getPhone() { return phone; }
            public void setPhone(String phone) { this.phone = phone; }
            public String getGender() { return gender; }
            public void setGender(String gender) { this.gender = gender; }
            public String getJoinDate() { return joinDate; }
            public void setJoinDate(String joinDate) { this.joinDate = joinDate; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
        }

        public static class CurrentPlan {
            private String planType;
            private double totalFee;
            private String startDate;
            private String endDate;
            private String status; // ACTIVE, EXPIRED
            private String paymentStatus; // FULLY_PAID, PENDING
            private double totalPaid;
            private double remainingAmount;

            public CurrentPlan() {}

            // Getters and Setters
            public String getPlanType() { return planType; }
            public void setPlanType(String planType) { this.planType = planType; }
            public double getTotalFee() { return totalFee; }
            public void setTotalFee(double totalFee) { this.totalFee = totalFee; }
            public String getStartDate() { return startDate; }
            public void setStartDate(String startDate) { this.startDate = startDate; }
            public String getEndDate() { return endDate; }
            public void setEndDate(String endDate) { this.endDate = endDate; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            public String getPaymentStatus() { return paymentStatus; }
            public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
            public double getTotalPaid() { return totalPaid; }
            public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }
            public double getRemainingAmount() { return remainingAmount; }
            public void setRemainingAmount(double remainingAmount) { this.remainingAmount = remainingAmount; }
        }

        public Membercollet() {
            payments = new HashMap<>();
        }

        // Getters and Setters
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Info getInfo() { return info; }
        public void setInfo(Info info) { this.info = info; }
        public CurrentPlan getCurrentPlan() { return currentPlan; }
        public void setCurrentPlan(CurrentPlan currentPlan) { this.currentPlan = currentPlan; }
        public Map<String, Payment> getPayments() { return payments; }
        public void setPayments(Map<String, Payment> payments) { this.payments = payments; }
    }