package com.danbramos.e_commerce_api.payment.event; // New sub-package 'event'

import lombok.Getter;

@Getter
public abstract class PaymentEvent {
    private final Long orderId;
    protected PaymentEvent(Long orderId) { this.orderId = orderId; }
}

