package com.danbramos.e_commerce_api.order.dto;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Schema(description = "Detailed representation of a customer order")
public record OrderDTO( // Use 'record' keyword
                        @Schema(description = "Unique identifier for the order")
                        Long id,

                        @Schema(description = "ID of the user who placed the order")
                        Long userId,

                        @Schema(description = "List of items included in the order")
                        List<OrderItemDTO> orderItems,

                        @Schema(description = "Total amount for the order")
                        BigDecimal totalAmount,

                        @Schema(description = "Date and time the order was placed")
                        LocalDateTime orderDate,

                        @Schema(description = "Shipping address for the order")
                        Address shippingAddress,

                        @Schema(description = "Payment details for the order")
                        PaymentDetailsDTO paymentDetails,

                        @Schema(description = "Current status of the order")
                        OrderStatus orderStatus
) {
}