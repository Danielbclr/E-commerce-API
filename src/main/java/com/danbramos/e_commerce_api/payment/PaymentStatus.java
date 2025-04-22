package com.danbramos.e_commerce_api.payment;

/**
 * Represents the status of a payment associated with an order.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but is awaiting confirmation or completion.
     */
    PENDING,

    /**
     * Payment was successfully processed and confirmed.
     */
    COMPLETED,

    /**
     * Payment processing failed.
     */
    FAILED,

    /**
     * Payment has been refunded (partially or fully).
     * Note: Handling refunds might require more complex logic later.
     */
    REFUNDED
}