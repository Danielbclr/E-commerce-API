package com.danbramos.e_commerce_api.shoppingcart.dto;

import java.math.BigDecimal;

public record CartItemDTO(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal
        ) {
}
