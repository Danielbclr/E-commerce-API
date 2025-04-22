package com.danbramos.e_commerce_api.payment;

import com.danbramos.e_commerce_api.common.Address;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Embeddable class containing details related to the payment of an order.
 * This includes the payment method, status, transaction details, payment date,
 * and the billing address associated with the payment.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PaymentDetails {

    /**
     * The method used for payment (e.g., CREDIT_CARD, PAYPAL).
     * Defaults to UNKNOWN. It's mandatory.
     */
    @NotNull(message = "Payment method cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.UNKNOWN;

    /**
     * The current status of the payment (e.g., PENDING, COMPLETED, FAILED).
     * Defaults to PENDING. It's mandatory.
     */
    @NotNull(message = "Payment status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Optional transaction identifier provided by the payment gateway upon
     * successful completion.
     */
    @Column(name = "payment_transaction_id")
    private String transactionId;

    /**
     * The date and time when the payment status last changed
     * (e.g., when it was marked as COMPLETED or FAILED).
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * The billing address associated with this payment.
     * Stored as an embedded object with overridden column names.
     * Validation is enabled for the address fields.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country"))
    })
    @Valid
    private Address billingAddress;

    /**
     * Constructs new PaymentDetails with the specified method and billing address.
     * Initializes the payment status to PENDING.
     *
     * @param paymentMethod  The payment method chosen for the order.
     * @param billingAddress The billing address associated with the payment.
     */
    public PaymentDetails(PaymentMethod paymentMethod, Address billingAddress) {
        this.paymentMethod = paymentMethod;
        this.billingAddress = billingAddress;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    /**
     * Updates the payment status to COMPLETED, sets the payment date to the current time,
     * and records the provided transaction ID.
     *
     * @param transactionId The transaction identifier from the payment gateway.
     */
    public void markAsCompleted(String transactionId) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
        this.transactionId = transactionId;
    }

    /**
     * Updates the payment status to FAILED and sets the payment date to the current time.
     * The transaction ID is typically null or irrelevant in this case.
     */
    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        this.paymentDate = LocalDateTime.now();
    }
}