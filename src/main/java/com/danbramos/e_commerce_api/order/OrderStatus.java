package com.danbramos.e_commerce_api.order;

/**
 * Represents the possible states of an Order in the e-commerce system.
 */
public enum OrderStatus {
    /**
     * Order has been created, but payment has not yet been confirmed.
     * The system is awaiting payment completion.
     */
    PENDING_PAYMENT,

    /**
     * Payment has been successfully received, and the order is being prepared
     * for shipment (e.g., items are being picked and packed).
     */
    PROCESSING,

    /**
     * The order has been handed over to the carrier and is in transit to the customer.
     */
    SHIPPED,

    /**
     * The carrier has confirmed the delivery of the order to the customer.
     */
    DELIVERED,

    /**
     * The order has been cancelled. This could be due to payment failure,
     * user request, or other reasons before shipment.
     */
    CANCELLED
}