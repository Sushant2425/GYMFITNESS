                    package com.coderspuxelinnnovation.gymmanagementsystem.models;

                    public class PlanExpiryModel {

                        public String name;
                        public String phone;
                        public String planType;
                        public String endDate;
                        public String status;
                        public int remainingDays;  // New: Positive for future, 0 for today, negative for expired
                        public PlanExpiryModel(String name, String phone, String planType,
                                               String endDate, String status) {
                            this.name = name;
                            this.phone = phone;
                            this.planType = planType;
                            this.endDate = endDate;
                            this.status = status;
                        }
                    }
