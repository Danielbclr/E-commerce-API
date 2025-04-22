package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.exception.EmptyCartException;
import com.danbramos.e_commerce_api.exception.InsufficientStockException;
import com.danbramos.e_commerce_api.payment.MockPaymentService;
import com.danbramos.e_commerce_api.payment.PaymentDetails;
import com.danbramos.e_commerce_api.payment.PaymentMethod;
import com.danbramos.e_commerce_api.payment.PaymentStatus;
import com.danbramos.e_commerce_api.payment.event.PaymentSuccessEvent;
import com.danbramos.e_commerce_api.product.Product;
import com.danbramos.e_commerce_api.product.ProductService;
import com.danbramos.e_commerce_api.shoppingcart.ShoppingCart;
import com.danbramos.e_commerce_api.shoppingcart.ShoppingCartItem;
import com.danbramos.e_commerce_api.shoppingcart.ShoppingCartService;
import com.danbramos.e_commerce_api.user.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final ShoppingCartService shoppingCartService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final MockPaymentService mockPaymentService;

    /**
     * Creates an order from the user's shopping cart, performs stock checks,
     * saves the order with PENDING_PAYMENT status, clears the cart,
     * and initiates asynchronous payment processing.
     *
     * @param user            The user placing the order.
     * @param shippingAddress The address to ship the order to.
     * @param paymentMethod   The chosen payment method.
     * @param billingAddress  The billing address for the payment (can be same as shipping).
     * @return The newly created Order entity.
     * @throws EmptyCartException         if the user's cart is empty.
     * @throws InsufficientStockException if any item in the cart has insufficient stock.
     */
    @Override
    @Transactional
    public Order createOrder(User user, Address shippingAddress, PaymentMethod paymentMethod, Address billingAddress)
            throws EmptyCartException, InsufficientStockException {

        log.info("Attempting to create order for user ID: {}", user.getId());
        ShoppingCart cart = shoppingCartService.getCartByUser(user);

        if (cart == null || cart.getItems().isEmpty()) {
            log.warn("Order creation failed for user ID: {}. Cart is empty.", user.getId());
            throw new EmptyCartException("Cannot create order from an empty shopping cart.");
        }
        log.debug("Cart ID: {} retrieved with {} items.", cart.getId(), cart.getItems().size());

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        PaymentDetails initialPaymentDetails = new PaymentDetails(paymentMethod, billingAddress);
        order.setPaymentDetails(initialPaymentDetails);

        log.debug("Processing cart items for order creation...");
        for (ShoppingCartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            log.debug("Checking stock for Product ID: {}, Quantity: {}", product.getId(), quantity);

            productService.verifyStockAvailability(product, quantity);

            OrderItem orderItem = new OrderItem(order, product, quantity);
            order.addOrderItem(orderItem);
            log.debug("Added OrderItem for Product ID: {}", product.getId());
        }

        order.calculateTotalAmount();
        log.info("Calculated total amount for order: {}", order.getTotalAmount());

        Order savedOrder = orderRepository.save(order);
        log.info("Order ID: {} saved successfully with status: {}", savedOrder.getId(), savedOrder.getOrderStatus());

        shoppingCartService.clearCart(user);
        log.info("Shopping cart cleared for user ID: {}", user.getId());

        mockPaymentService.processPayment(savedOrder);
        log.info("Asynchronous payment processing initiated for Order ID: {}", savedOrder.getId());

        user.addOrder(savedOrder);
        return savedOrder;
    }

    /**
     * Handles the callback for a successful payment simulation.
     * Updates the order status to PROCESSING and payment details to COMPLETED.
     *
     * @param event The event containing details of the successful payment.
     */
    @Override
    @EventListener
    @Transactional
    @Async
    public void handleOnSuccessfulPayment(PaymentSuccessEvent event) {
        Long orderId = event.getOrderId();
        String transactionId = event.getTransactionId();

        log.info("Handling successful payment callback for Order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Payment success callback failed: Order not found with ID: {}", orderId);
                    return new EntityNotFoundException("Order not found with ID: " + orderId);
                });

        if (order.getPaymentDetails().getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.warn("Order ID: {} payment is already marked as COMPLETED. Ignoring duplicate success callback.", orderId);
            return;
        }

        order.getPaymentDetails().markAsCompleted(transactionId);
        log.debug("Order ID: {} - PaymentDetails marked as COMPLETED with Txn ID: {}", orderId, transactionId);

        order.setOrderStatus(OrderStatus.PROCESSING);
        log.debug("Order ID: {} - Status updated to PROCESSING.", orderId);

        orderRepository.save(order);
        log.info("Order ID: {} successfully updated after successful payment.", orderId);

    }

    /**
     * Finds a specific order by its ID, ensuring it belongs to the specified user.
     *
     * @param orderId The ID of the order to find.
     * @param user    The user who must own the order.
     * @return An Optional containing the Order if found and owned by the user.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findOrderByIdAndUser(Long orderId, User user) {
        log.debug("Finding order by ID: {} for User ID: {}", orderId, user.getId());
        return orderRepository.findByIdAndUser(orderId, user);
    }

    /**
     * Finds all orders placed by a specific user, ordered by date descending.
     *
     * @param user The user whose orders are to be retrieved.
     * @return A list of the user's orders.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllOrdersByUser(User user) {
        log.debug("Finding all orders for User ID: {}", user.getId());
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }
}