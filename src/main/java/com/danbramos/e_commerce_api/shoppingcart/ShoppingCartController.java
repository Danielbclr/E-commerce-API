package com.danbramos.e_commerce_api.shoppingcart;

import com.danbramos.e_commerce_api.exception.InsufficientStockException;
import com.danbramos.e_commerce_api.shoppingcart.dto.AddItemRequestDTO;
import com.danbramos.e_commerce_api.shoppingcart.dto.CartDTO;
import com.danbramos.e_commerce_api.shoppingcart.dto.CartItemDTO;
import com.danbramos.e_commerce_api.shoppingcart.dto.UpdateItemQuantityDTO;
import com.danbramos.e_commerce_api.user.User;
import com.danbramos.e_commerce_api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing user shopping carts.
 * Provides endpoints for viewing, adding items to, updating items in,
 * and clearing the shopping cart associated with the authenticated user.
 */
@RestController
@RequestMapping("${api-base-path}/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shopping Cart", description = "Endpoints for managing user shopping carts")
@SecurityRequirement(name = "bearerAuth")
public class ShoppingCartController {

    /**
     * Service layer dependency for handling shopping cart business logic.
     */
    private final ShoppingCartService shoppingCartService;
    /**
     * Service layer dependency for retrieving user details.
     */
    private final UserService userService;

    /**
     * Retrieves the full {@link User} entity corresponding to the authenticated user's details.
     *
     * @param userDetails The {@link UserDetails} object representing the authenticated user, injected by Spring Security.
     * @return The {@link User} entity.
     * @throws ResponseStatusException with status 401 (Unauthorized) if userDetails is null.
     * @throws ResponseStatusException with status 500 (Internal Server Error) if the user cannot be found in the database.
     */
    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            log.error("AuthenticationPrincipal UserDetails is null.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        User user = userService.findUserByEmail(userDetails.getUsername());
        if (user == null) {
            log.error("Authenticated user '{}' not found in the database.", userDetails.getUsername());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user data not found.");
        }
        return user;
    }

    /**
     * Maps a {@link ShoppingCart} entity to its corresponding {@link CartDTO} representation.
     * Calculates the subtotal for each item and the total price for the cart.
     *
     * @param cart The {@link ShoppingCart} entity to map.
     * @return The mapped {@link CartDTO}.
     */
    private CartDTO mapToCartDTO(ShoppingCart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> new CartItemDTO(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getProduct().getPrice(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .collect(Collectors.toList());

        BigDecimal totalPrice = itemDTOs.stream()
                .map(CartItemDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(cart.getId(), itemDTOs, totalPrice);
    }

    /**
     * Handles GET requests to retrieve the shopping cart for the currently authenticated user.
     *
     * @param userDetails The details of the authenticated user, injected by Spring Security via {@link AuthenticationPrincipal}.
     * @return A {@link ResponseEntity} containing the {@link CartDTO} and HTTP status 200 (OK).
     */
    @Operation(summary = "Get User Cart", description = "Retrieves the shopping cart for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved cart", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CartDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Could not find authenticated user data or other server issue", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CartDTO> getUserCart(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received request to get cart for user: {}", userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);
        ShoppingCart cart = shoppingCartService.getCartByUser(currentUser);
        CartDTO cartDto = mapToCartDTO(cart);
        log.info("Returning cart ID: {} for user: {}", cartDto.cartId(), userDetails.getUsername());
        return ResponseEntity.ok(cartDto);
    }

    /**
     * Handles POST requests to add an item to the authenticated user's shopping cart.
     * If the item already exists in the cart, its quantity is updated.
     * Performs stock validation before adding/updating.
     *
     * @param userDetails The details of the authenticated user.
     * @param requestDto  The request body containing the product ID and quantity to add. Must be valid.
     * @return A {@link ResponseEntity} with HTTP status 201 (Created) on success.
     * @throws ResponseStatusException for various errors like insufficient stock (400), product not found (404), etc.
     */
    @Operation(summary = "Add Item to Cart", description = "Adds a specified quantity of a product to the current user's cart. If the item already exists, its quantity is updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item successfully added or updated", content = @Content),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input (e.g., non-positive quantity, insufficient stock)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Product with the specified ID does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToCart(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Product ID and quantity to add", required = true,
                    content = @Content(schema = @Schema(implementation = AddItemRequestDTO.class)))
            @Valid @RequestBody AddItemRequestDTO requestDto) {
        log.info("Received request to add item to cart for user: {}. ProductId: {}, Quantity: {}",
                userDetails.getUsername(), requestDto.productId(), requestDto.quantity());
        User currentUser = getCurrentUser(userDetails);
        try {
            shoppingCartService.addItemToCart(currentUser, requestDto.productId(), requestDto.quantity());
            log.info("Item added successfully for user: {}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (InsufficientStockException e) {
            log.warn("Stock check failed for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.error("Product not found for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding item to cart for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding item to cart", e);
        }
    }

    /**
     * Handles PUT requests to update the quantity of a specific item in the authenticated user's cart.
     * Performs stock validation before updating.
     *
     * @param userDetails The details of the authenticated user.
     * @param itemId      The ID of the {@link ShoppingCartItem} to update, taken from the path.
     * @param requestDto  The request body containing the new quantity. Must be valid.
     * @return A {@link ResponseEntity} with HTTP status 200 (OK) on success.
     * @throws ResponseStatusException for various errors like insufficient stock (400), item not found (404), etc.
     */
    @Operation(summary = "Update Cart Item Quantity", description = "Updates the quantity of a specific item within the current user's cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item quantity successfully updated", content = @Content),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input (e.g., non-positive quantity, insufficient stock) or item does not belong to the user's cart", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Cart item with the specified ID does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Void> updateCartItemQuantity(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the cart item to update", required = true) @PathVariable long itemId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New quantity for the item", required = true, content = @Content(schema = @Schema(implementation = UpdateItemQuantityDTO.class)))
            @Valid @RequestBody UpdateItemQuantityDTO requestDto) {
        log.info("Received request to update quantity for item ID: {} to {} for user: {}",
                itemId, requestDto.quantity(), userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);
        try {
            shoppingCartService.updateItemQuantity(currentUser, itemId, requestDto.quantity());
            log.info("Item quantity updated successfully for item ID: {} for user: {}", itemId, userDetails.getUsername());
            return ResponseEntity.ok().build();
        } catch (InsufficientStockException e) {
            log.warn("Stock check failed during update for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.error("Cart item not found for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during update for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating item quantity for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating item quantity", e);
        }
    }

    /**
     * Handles DELETE requests to remove a specific item from the authenticated user's cart.
     *
     * @param userDetails The details of the authenticated user.
     * @param itemId      The ID of the {@link ShoppingCartItem} to remove, taken from the path.
     * @return A {@link ResponseEntity} with HTTP status 204 (No Content) on success.
     * @throws ResponseStatusException if the item is not found (404) or doesn't belong to the user (400).
     */
    @Operation(summary = "Remove Item from Cart", description = "Removes a specific item from the current user's cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item successfully removed", content = @Content),
            @ApiResponse(responseCode = "400", description = "Bad Request - Item does not belong to the user's cart", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Cart item with the specified ID does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the cart item to remove", required = true) @PathVariable long itemId) {
        log.info("Received request to remove item ID: {} for user: {}", itemId, userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);
        try {
            shoppingCartService.removeItemFromCart(currentUser, itemId);
            log.info("Item removed successfully for item ID: {} for user: {}", itemId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (jakarta.persistence.EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error removing item from cart for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error removing item from cart", e);
        }
    }

    /**
     * Handles DELETE requests to remove all items from the authenticated user's shopping cart.
     *
     * @param userDetails The details of the authenticated user.
     * @return A {@link ResponseEntity} with HTTP status 204 (No Content) on success.
     * @throws ResponseStatusException if an internal error occurs (500).
     */
    @Operation(summary = "Clear Cart", description = "Removes all items from the current user's shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart successfully cleared", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> clearUserCart(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received request to clear cart for user: {}", userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);
        try {
            shoppingCartService.clearCart(currentUser);
            log.info("Cart cleared successfully for user: {}", userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error clearing cart for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error clearing cart", e);
        }
    }
}