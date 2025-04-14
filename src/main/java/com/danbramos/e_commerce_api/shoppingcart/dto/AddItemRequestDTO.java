package com.danbramos.e_commerce_api.shoppingcart.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequestDTO(
        @NotNull(message = "Product ID cannot be null")
        Long productId,

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}