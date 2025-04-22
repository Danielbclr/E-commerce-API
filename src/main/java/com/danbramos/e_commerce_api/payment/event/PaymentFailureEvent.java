package com.danbramos.e_commerce_api.payment.event;

public class PaymentFailureEvent extends PaymentEvent {
    public PaymentFailureEvent(Long orderId) {
        super(orderId);
    }
}
