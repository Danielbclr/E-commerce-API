package com.danbramos.e_commerce_api.shoppingcart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        long cartId,
        List<CartItemDTO> items,
        BigDecimal totalPrice // Calculated total for all items
) {
}