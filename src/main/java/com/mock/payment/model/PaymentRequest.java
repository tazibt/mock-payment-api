package com.mock.payment.model;

public class PaymentRequest {

    public double amount;
    public String cardType;
    public String customerId;

    public PaymentRequest() {
    }

    public PaymentRequest(double amount, String cardType, String customerId) {
        this.amount = amount;
        this.cardType = cardType;
        this.customerId = customerId;
    }
}
