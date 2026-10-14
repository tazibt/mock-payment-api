package com.mock.payment.model;

public class PaymentResponse {

    public String status;
    public String message;
    public String transactionId;
    public String errorCode;

    public PaymentResponse() {
    }

    public PaymentResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
}
