package com.danbramos.e_commerce_api.exception; // Place it in your exception package

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to perform an operation
 * (like creating an order) that requires a non-empty shopping cart,
 * but the cart is found to be empty.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Shopping cart cannot be empty for this operation.")
public class EmptyCartException extends RuntimeException {

    /**
     * Constructs a new EmptyCartException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EmptyCartException(String message) {
        super(message);
    }

}