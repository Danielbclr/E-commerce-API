package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.exception.EmptyCartException;
import com.danbramos.e_commerce_api.exception.InsufficientStockException;
import com.danbramos.e_commerce_api.order.dto.CreateOrderRequestDTO;
import com.danbramos.e_commerce_api.order.dto.OrderDTO;
import com.danbramos.e_commerce_api.order.dto.OrderItemDTO;
import com.danbramos.e_commerce_api.order.dto.PaymentDetailsDTO;
import com.danbramos.e_commerce_api.user.User;
import com.danbramos.e_commerce_api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for handling customer order operations.
 * Provides endpoints for creating new orders from the shopping cart,
 * retrieving specific order details, and listing the order history
 * for the authenticated user.
 */
@RestController
@RequestMapping("${api-base-path}/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "Endpoints for creating and viewing customer orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * Retrieves the full {@link User} entity corresponding to the authenticated user's details.
     * This is a helper method to ensure the user context is available for service layer calls.
     *
     * @param userDetails The {@link UserDetails} object representing the authenticated user,
     *                    injected by Spring Security.
     * @return The {@link User} entity associated with the authenticated principal.
     * @throws ResponseStatusException with status 401 (Unauthorized) if userDetails is null.
     * @throws ResponseStatusException with status 500 (Internal Server Error) if the user cannot be found in the database
     *                                 (indicating a potential data inconsistency or configuration issue).
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
     * Maps an {@link Order} entity to its corresponding {@link OrderDTO} representation.
     * This includes mapping nested entities like OrderItems and PaymentDetails to their respective DTOs.
     * Returns null if the input order is null.
     *
     * @param order The {@link Order} entity to map.
     * @return The mapped {@link OrderDTO}, or null if the input order was null.
     */
    private OrderDTO mapToOrderDTO(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        PaymentDetailsDTO paymentDTO = null;
        if (order.getPaymentDetails() != null) {
            paymentDTO = new PaymentDetailsDTO(
                    order.getPaymentDetails().getPaymentMethod(),
                    order.getPaymentDetails().getPaymentStatus(),
                    order.getPaymentDetails().getTransactionId(),
                    order.getPaymentDetails().getPaymentDate(),
                    order.getPaymentDetails().getBillingAddress()
            );
        }

        return new OrderDTO(
                order.getId(),
                order.getUser().getId(),
                itemDTOs,
                order.getTotalAmount(),
                order.getOrderDate(),
                order.getShippingAddress(),
                paymentDTO,
                order.getOrderStatus()
        );
    }

    /**
     * Handles POST requests to create a new order based on the authenticated user's shopping cart.
     * Validates the request, invokes the order service to create the order,
     * and returns the created order details as a DTO.
     *
     * @param userDetails The details of the authenticated user, injected by Spring Security.
     * @param requestDTO  The request body containing shipping/billing addresses and payment method. Must be valid.
     * @return A {@link ResponseEntity} containing the created {@link OrderDTO} and HTTP status 201 (Created).
     * @throws ResponseStatusException with status 400 (Bad Request) if the cart is empty, stock is insufficient, or input is invalid.
     * @throws ResponseStatusException with status 401 (Unauthorized) if the user is not authenticated.
     * @throws ResponseStatusException with status 500 (Internal Server Error) for unexpected errors during order creation.
     */
    @Operation(summary = "Create a new order", description = "Creates an order from the user's current shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order successfully created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cart is empty, insufficient stock, or invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Order details including addresses and payment method", required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequestDTO.class)))
            @Valid @RequestBody CreateOrderRequestDTO requestDTO) {

        log.info("Received request to create order for user: {}", userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);

        try {
            Order createdOrder = orderService.createOrder(
                    currentUser,
                    requestDTO.shippingAddress(),
                    requestDTO.paymentMethod(),
                    requestDTO.billingAddress()
            );
            OrderDTO responseDTO = mapToOrderDTO(createdOrder);
            log.info("Order ID: {} created successfully for user: {}", createdOrder.getId(), userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

        } catch (EmptyCartException e) {
            log.warn("Order creation failed for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (InsufficientStockException e) {
            log.warn("Order creation failed due to stock for user {}: {}", userDetails.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating order for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while creating the order.", e);
        }
    }

    /**
     * Handles GET requests to retrieve details of a specific order placed by the authenticated user.
     * Ensures the requested order belongs to the authenticated user before returning details.
     *
     * @param userDetails The details of the authenticated user.
     * @param orderId     The ID of the order to retrieve, specified in the path variable.
     * @return A {@link ResponseEntity} containing the {@link OrderDTO} and HTTP status 200 (OK).
     * @throws ResponseStatusException with status 401 (Unauthorized) if the user is not authenticated.
     * @throws ResponseStatusException with status 404 (Not Found) if the order doesn't exist or doesn't belong to the user.
     * @throws ResponseStatusException with status 500 (Internal Server Error) for unexpected errors.
     */
    @Operation(summary = "Get Order by ID", description = "Retrieves details of a specific order placed by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved order details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Order not found or does not belong to the user", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the order to retrieve", required = true) @PathVariable Long orderId) {

        log.info("Received request to get order ID: {} for user: {}", orderId, userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);

        Order order = orderService.findOrderByIdAndUser(orderId, currentUser)
                .orElseThrow(() -> {
                    log.warn("Order ID: {} not found or not owned by user: {}", orderId, userDetails.getUsername());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });

        OrderDTO orderDTO = mapToOrderDTO(order);
        log.info("Returning details for order ID: {}", orderId);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * Handles GET requests to retrieve a list of all orders placed by the authenticated user.
     * Orders are typically returned in descending order of creation date.
     *
     * @param userDetails The details of the authenticated user.
     * @return A {@link ResponseEntity} containing a list of {@link OrderDTO} objects and HTTP status 200 (OK).
     * @throws ResponseStatusException with status 401 (Unauthorized) if the user is not authenticated.
     * @throws ResponseStatusException with status 500 (Internal Server Error) for unexpected errors.
     */
    @Operation(summary = "Get User's Order History", description = "Retrieves a list of all orders placed by the authenticated user, ordered by date descending.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved order history",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = OrderDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrdersForUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received request to get all orders for user: {}", userDetails.getUsername());
        User currentUser = getCurrentUser(userDetails);

        List<Order> orders = orderService.findAllOrdersByUser(currentUser);

        List<OrderDTO> orderDTOs = orders.stream()
                .map(this::mapToOrderDTO)
                .collect(Collectors.toList());

        log.info("Returning {} orders for user: {}", orderDTOs.size(), userDetails.getUsername());
        return ResponseEntity.ok(orderDTOs);
    }
}