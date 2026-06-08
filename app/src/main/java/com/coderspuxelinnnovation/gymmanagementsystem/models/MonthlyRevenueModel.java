package com.coderspuxelinnnovation.gymmanagementsystem.models;

public class MonthlyRevenueModel {
    private int monthNumber;
    private String monthName;
    private int year;
    private int totalRevenue;
    private int paymentCount;
    private int cashAmount;
    private int upiAmount;
    private int cardAmount;

    // Getters and Setters
    public int getMonthNumber() { return monthNumber; }
    public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(int totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getPaymentCount() { return paymentCount; }
    public void setPaymentCount(int paymentCount) { this.paymentCount = paymentCount; }

    public int getCashAmount() { return cashAmount; }
    public void setCashAmount(int cashAmount) { this.cashAmount = cashAmount; }

    public int getUpiAmount() { return upiAmount; }
    public void setUpiAmount(int upiAmount) { this.upiAmount = upiAmount; }

    public int getCardAmount() { return cardAmount; }
    public void setCardAmount(int cardAmount) { this.cardAmount = cardAmount; }
}