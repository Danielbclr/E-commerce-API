package com.danbramos.e_commerce_api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record UserRegistrationDTO(
        @NotEmpty(message = "Name should not be empty")
        String name,
        @NotEmpty(message = "Email should not be empty")
        @Email(message = "Please provide a valid email")
        String email,
        @NotEmpty(message = "Password should not be empty")
        String password

) {
}
    