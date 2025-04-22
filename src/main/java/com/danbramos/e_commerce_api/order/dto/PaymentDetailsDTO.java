package com.danbramos.e_commerce_api.order.dto;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.payment.PaymentMethod;
import com.danbramos.e_commerce_api.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Details of the payment associated with the order")
public record PaymentDetailsDTO(
                                 @Schema(description = "Payment method used")
                                 PaymentMethod paymentMethod,
                                 @Schema(description = "Current status of the payment")
                                 PaymentStatus paymentStatus,
                                 @Schema(description = "Transaction ID from the payment gateway (if available)")
                                 String transactionId,
                                 @Schema(description = "Date and time the payment was processed (completed or failed)")
                                 LocalDateTime paymentDate,
                                 @Schema(description = "Billing address used for the payment")
                                 Address billingAddress) {

}