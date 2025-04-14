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
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Import SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException; // Import EntityNotFoundException
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.web.bind.annotation.*; // Import DeleteMapping, PathVariable
import org.springframework.web.server.ResponseStatusException; // Make sure this is imported

import java.util.Map; // For response body

/**
 * Controller responsible for handling authentication-related requests,
 * primarily user registration and administrative user management.
 */
@Slf4j
@RestController
@RequestMapping("${api-base-path}/auth") // Consider moving user management to a separate /users controller later
@RequiredArgsConstructor
@Tag(name = "Authentication & User Management", description = "APIs for user registration, login, and admin user management") // Updated Tag
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

    @Operation(summary = "Delete a specific user", description = "Deletes a user by their ID. Requires ADMIN role. Cannot delete other admins or self.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User successfully deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cannot delete admin users or self", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - User with the specified ID does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Secure this endpoint for ADMINs only
    @SecurityRequirement(name = "bearerAuth") // Indicate security requirement for Swagger
    public ResponseEntity<Void> deleteUserById(
            @Parameter(description = "ID of the user to delete", required = true) @PathVariable Long id) {
        log.warn("[ADMIN ACTION] Received request to delete user with ID: {}", id);
        try {
            userService.deleteUserById(id);
            log.warn("[ADMIN ACTION] Successfully deleted user with ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // Handle cases like trying to delete admin or self
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ADMIN ACTION] Error deleting user with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while deleting the user.", e);
        }
    }

    // --- Admin: Delete All Non-Admin Users Endpoint ---
    @Operation(summary = "Delete all non-admin users", description = "Deletes all users who do not have the ADMIN role. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Non-admin users successfully deleted (or none found)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))), // Return count
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error (e.g., ROLE_ADMIN not found)", content = @Content)
    })
    @DeleteMapping("/users/non-admin")
    @PreAuthorize("hasRole('ADMIN')") // Secure this endpoint for ADMINs only
    @SecurityRequirement(name = "bearerAuth") // Indicate security requirement for Swagger
    public ResponseEntity<Map<String, Object>> deleteAllNonAdminUsers() {
        log.warn("[ADMIN ACTION] Received request to delete all non-ADMIN users.");
        try {
            long deletedCount = userService.deleteNonAdminUsers();
            log.warn("[ADMIN ACTION] Completed deletion of non-ADMIN users. Count: {}", deletedCount);
            Map<String, Object> response = Map.of(
                    "message", "Successfully processed deletion of non-admin users.",
                    "deletedCount", deletedCount
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Handle case where ROLE_ADMIN is missing
            log.error("[ADMIN ACTION] Critical error during non-admin user deletion: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ADMIN ACTION] Error deleting non-ADMIN users: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while deleting non-admin users.", e);
        }
    }

    // --- Future Login Endpoint ---
    // @PostMapping("/login")
    // public ResponseEntity<?> loginUser(...) { ... }
}