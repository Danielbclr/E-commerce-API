package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a single item within an Order.
 * Stores details of the product, including its price and name,
 * at the exact moment the order was placed to ensure historical accuracy
 * even if the original product details change later.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    /**
     * Unique identifier for the order item. Generated automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The {@link Order} this item belongs to. This is the owning side of the relationship.
     * Loaded lazily by default. It's mandatory for an OrderItem to be part of an Order.
     */
    @NotNull // An OrderItem must belong to an Order
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * The ID of the original {@link Product} this order item refers to.
     * Used for linking back to the product entity if needed, but the core details
     * (name, price) are duplicated below for historical accuracy.
     */
    @NotNull(message = "Product ID cannot be null for an order item")
    @Column(nullable = false)
    private Long productId;

    /**
     * The name of the product as it was when the order was placed.
     * Stored directly to avoid issues if the product name changes later.
     */
    @NotBlank(message = "Product name cannot be blank for an order item")
    @Column(nullable = false)
    private String productName;

    /**
     * The quantity of this specific product ordered. Must be at least 1.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    /**
     * The price of a single unit of the product at the time the order was placed.
     * Stored directly to ensure the correct price is used, even if the product price changes later.
     * Defined with precision and scale suitable for currency.
     */
    @NotNull(message = "Price per unit cannot be null")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    /**
     * The calculated subtotal for this line item (quantity * pricePerUnit).
     * Automatically calculated before persistence or update via {@link #calculateSubtotal()}.
     * Defined with precision and scale suitable for currency.
     */
    @NotNull(message = "Subtotal cannot be null")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Calculates the subtotal for this order item based on quantity and price per unit.
     * This method is automatically called by JPA before the entity is first persisted
     * or subsequently updated due to the {@link PrePersist} and {@link PreUpdate} annotations.
     * Sets subtotal to zero if price or quantity is invalid.
     */
    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        if (pricePerUnit != null && quantity > 0) {
            this.subtotal = pricePerUnit.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    /**
     * Constructs a new OrderItem, typically used when converting from a ShoppingCartItem
     * during order creation. It captures the necessary product details at the time of order.
     * Automatically calculates the subtotal upon creation.
     *
     * @param order    The {@link Order} this item belongs to.
     * @param product  The {@link Product} being ordered.
     * @param quantity The quantity of the product being ordered.
     */
    public OrderItem(Order order, Product product, int quantity) {
        this.order = order;
        this.productId = product.getId();
        this.productName = product.getName();
        this.quantity = quantity;
        this.pricePerUnit = product.getPrice();
        calculateSubtotal();
    }
}