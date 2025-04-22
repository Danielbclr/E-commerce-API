package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.payment.PaymentDetails;
import com.danbramos.e_commerce_api.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer order placed in the e-commerce system.
 * Contains details about the user, items ordered, shipping and billing information,
 * payment status, and overall order status.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor // Required by JPA
public class Order {

    /**
     * Unique identifier for the order. Generated automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who placed the order. This is a mandatory relationship.
     * Loaded lazily by default to optimize performance.
     */
    @NotNull(message = "Order must be associated with a user")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) // Foreign key column in the 'orders' table
    private User user;

    /**
     * List of items included in this order.
     * Managed via cascade operations (ALL) and orphan removal is enabled,
     * meaning OrderItems are deleted if removed from this list.
     * Loaded eagerly for simplicity in this example, consider LAZY for performance with many items.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * The total calculated monetary value of the order.
     * Precision and scale are defined for currency representation.
     * Automatically calculated before persistence or update.
     */
    @NotNull(message = "Total amount cannot be null")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * The exact date and time when the order was created in the system.
     * Automatically set upon creation and cannot be updated.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate;

    /**
     * The address where the order should be shipped. Stored as an embedded object.
     * Column names are overridden to avoid conflicts and provide clarity.
     * Validation is enabled for the embedded address fields.
     */
    @NotNull(message = "Shipping address cannot be null")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street", nullable = false)),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city", nullable = false)),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state", nullable = false)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", nullable = false)),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country", nullable = false))
    })
    @Valid
    private Address shippingAddress;

    /**
     * Details related to the payment for this order, including method, status, and billing address.
     * Stored as an embedded object.
     * Validation is enabled for the embedded payment details (including its own embedded billing address).
     */
    @NotNull(message = "Payment details cannot be null")
    @Embedded
    @Valid
    private PaymentDetails paymentDetails;

    /**
     * The current status of the order (e.g., PENDING_PAYMENT, PROCESSING, SHIPPED).
     * Defaults to PENDING_PAYMENT upon creation.
     */
    @NotNull(message = "Order status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

    /**
     * Adds an {@link OrderItem} to this order and establishes the bidirectional relationship.
     * Initializes the list if it's null (though it's initialized by default).
     *
     * @param item The OrderItem to add. If null, the method does nothing.
     */
    public void addOrderItem(OrderItem item) {
        if (item != null) {
            if (this.orderItems == null) {
                this.orderItems = new ArrayList<>();
            }
            this.orderItems.add(item);
            item.setOrder(this);
        }
    }

    /**
     * Calculates the total amount for the order by summing the subtotals of all its items.
     * If there are no items or the list is null, the total amount is set to zero.
     * Filters out any potentially null subtotals before summing.
     */
    public void calculateTotalAmount() {
        if (this.orderItems == null || this.orderItems.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            return;
        }
        this.totalAmount = this.orderItems.stream()
                .map(OrderItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * JPA lifecycle callback method. Ensures the total amount is calculated
     * just before the order entity is first persisted or subsequently updated.
     */
    @PrePersist
    @PreUpdate
    private void ensureTotalAmountCalculated() {
        calculateTotalAmount();
    }
}