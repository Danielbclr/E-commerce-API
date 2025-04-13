package com.danbramos.e_commerce_api.auth;

import com.danbramos.e_commerce_api.user.User; // Import User entity
import com.danbramos.e_commerce_api.user.UserDTO;
import com.danbramos.e_commerce_api.user.UserRegistrationDTO;
import com.danbramos.e_commerce_api.user.UserService;
import io.swagger.v3.oas.annotations.Operation; // Add Swagger imports if desired
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException; // Make sure this is imported

/**
 * Controller responsible for handling authentication-related requests,
 * primarily user registration. Login functionality might be added here later.
 */
@Slf4j
@RestController
@RequestMapping("${api-base-path}/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user registration and login") // Added Swagger Tag
public class AuthController {

    private final UserService userService;

    /**
     * Handles the registration request for a new user.
     * Validates the incoming user data and checks if the email is already in use.
     * If successful, creates the user via the UserService.
     *
     * @param userDto The user data transfer object containing registration details. Must be valid according to UserDTO constraints.
     * @return A {@link ResponseEntity} with status 201 (Created) and a success message upon successful registration.
     * @throws ResponseStatusException with status 409 (Conflict) if the email is already registered.
     * @throws ResponseStatusException with status 500 (Internal Server Error) if an unexpected error occurs during the process.
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account. Checks for existing email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid user data provided", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email address is already registered", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during registration", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @Parameter(description = "User details for registration", required = true,
                    schema = @Schema(implementation = UserRegistrationDTO.class))
            @Valid @RequestBody UserRegistrationDTO userDto) {
        log.info("Received registration request for email: {}", userDto.email());

        User existingUser = userService.findUserByEmail(userDto.email()); // Use findUserByEmail which returns User or null
        if (existingUser != null) {
            log.warn("Registration failed: Email already exists - {}", userDto.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already registered.");
        }

        try {
            userService.saveUser(userDto);
            log.info("User registered successfully: {}", userDto.email());
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
        } catch (Exception e) {
            log.error("Error during registration for email {}: {}", userDto.email(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during registration.", e);
        }
    }

    // --- Future Login Endpoint ---
    // @PostMapping("/login")
    // public ResponseEntity<?> loginUser(...) { ... }

}