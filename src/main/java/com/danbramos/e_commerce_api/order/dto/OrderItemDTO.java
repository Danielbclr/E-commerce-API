package com.danbramos.e_commerce_api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Details of a single item within an order")
public record OrderItemDTO(
                            @Schema(description = "ID of the order item record")
                            Long id,

                            @Schema(description = "ID of the product ordered")
                            Long productId,

                            @Schema(description = "Name of the product at the time of order")
                            String productName,

                            @Schema(description = "Quantity ordered")
                            int quantity,

                            @Schema(description = "Price per unit at the time of order")
                            BigDecimal pricePerUnit,

                            @Schema(description = "Subtotal for this line item (quantity * pricePerUnit)")
                            BigDecimal subtotal
) {

}