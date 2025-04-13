package com.danbramos.e_commerce_api.user;

import com.danbramos.e_commerce_api.role.Role;
import io.swagger.v3.oas.annotations.Operation; // Assuming you might use Swagger later
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Import security annotations if you add them later
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller for managing user-related operations.
 * Provides endpoints for retrieving user information.
 * Note: User registration is currently included but might be better placed in a dedicated AuthController.
 */
@RestController
@RequestMapping("${api-base-path}/users") // Base path from application properties
@RequiredArgsConstructor // Lombok constructor injection
@Slf4j // Lombok logger
@Tag(name = "User Management", description = "APIs for retrieving user information") // For Swagger grouping
public class UserController {

    private final UserService userService;

    /**
     * Retrieves a list of all registered users.
     * **Security Note:** This endpoint exposes potentially sensitive user data and
     * should be strictly secured, typically accessible only by administrators.
     *
     * @return A ResponseEntity containing a list of {@link UserDTO} objects.
     */
    @Operation(summary = "Get all users", description = "Retrieves a list of all registered users. Requires ADMIN role.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth")}) // Example security requirement
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges (ADMIN role required)", content = @Content)
    })
    @GetMapping
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment and use appropriate security
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.debug("Request received to get all users");
        // Transactionality (readOnly=true) should be handled in userService.findAllUsers()
        List<UserDTO> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a specific user by their email address.
     * **Security Note:** Access should be restricted, e.g., only admins or the user themselves
     * should be able to retrieve user details.
     *
     * @param email The email address of the user to retrieve.
     * @return A ResponseEntity containing the {@link UserDTO} if found (status 200 OK),
     *         or throws ResponseStatusException (status 404 Not Found) if the user doesn't exist.
     */
    @Operation(summary = "Get user by email", description = "Retrieves user details by their email address. Requires appropriate authorization.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found with the specified email", content = @Content)
    })
    @GetMapping("/email/{email}")
    // @PreAuthorize("hasRole('ADMIN') or #email == authentication.principal.username") // Example security
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "Email address of the user to retrieve", required = true)
            @PathVariable String email) {
        log.debug("Request received to get user by email: {}", email);
        // Transactionality (readOnly=true) should be handled in userService.findUserByEmail()
        User user = userService.findUserByEmail(email);
        if (user == null) {
            log.warn("User not found with email: {}", email);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email);
        }

        // Convert entity to DTO for response - ensures password etc. aren't exposed
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName) // Assumes Role::getName exists
                .collect(Collectors.toSet());
        UserDTO userDto = UserDTO.fromUser(user);

        log.debug("Returning user details for email: {}", email);
        return ResponseEntity.ok(userDto);
    }

}