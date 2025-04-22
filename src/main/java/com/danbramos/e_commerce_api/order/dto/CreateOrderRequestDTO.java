package com.danbramos.e_commerce_api.order.dto;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating a new order")
public record CreateOrderRequestDTO (
                                     @NotNull(message = "Shipping address cannot be null")
                                     @Valid
                                     @Schema(description = "The address where the order should be shipped")
                                     Address shippingAddress,

                                     @NotNull(message = "Billing address cannot be null")
                                     @Valid
                                     @Schema(description = "The address associated with the payment method")
                                     Address billingAddress,

                                     @NotNull(message = "Payment method cannot be null")
                                     @Schema(description = "The payment method chosen for the order", example = "CREDIT_CARD")
                                     PaymentMethod paymentMethod
) {
}