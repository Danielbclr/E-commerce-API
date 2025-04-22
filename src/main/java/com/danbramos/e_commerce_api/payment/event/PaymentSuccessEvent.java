package com.danbramos.e_commerce_api.payment.event;

public class PaymentSuccessEvent extends PaymentEvent {
    private final String transactionId;
    public PaymentSuccessEvent(Long orderId, String transactionId) {
        super(orderId);
        this.transactionId = transactionId;
    }
    public String getTransactionId() { return transactionId; }
}
