package com.danbramos.e_commerce_api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
// No need for Set here if roles aren't part of this specific DTO
// import java.util.Set;

public record UserDTO(
        Long id,
        @NotEmpty(message = "Name should not be empty")
        String name,
        @NotEmpty(message = "Email should not be empty")
        @Email(message = "Please provide a valid email")
        String email
) {
    // The fromUser method becomes much simpler or could even be replaced
    // by the mapToUserDto method in the service layer.
    public static UserDTO fromUser(User user) {
        // Password is null here, suitable for response DTOs
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }
}
    