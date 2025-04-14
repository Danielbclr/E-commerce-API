package com.danbramos.e_commerce_api.shoppingcart;

import com.danbramos.e_commerce_api.exception.InsufficientStockException;
import com.danbramos.e_commerce_api.product.Product;
import com.danbramos.e_commerce_api.product.ProductService;
import com.danbramos.e_commerce_api.user.User;
import com.danbramos.e_commerce_api.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service implementation for managing shopping cart operations.
 * Handles logic for retrieving carts, adding, updating, removing items, and clearing carts.
 * Includes stock verification logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final UserService userService;
    private final ProductService productService;
    private final ShoppingCartRepository cartRepository;
    private final ShoppingCartItemRepository cartItemRepository;

    /**
     * Retrieves the shopping cart associated with the given user.
     * Assumes a cart is always created upon user registration.
     *
     * @param user The user whose cart is to be retrieved. Must not be null and must have an ID.
     * @return The user's {@link ShoppingCart}.
     * @throws IllegalArgumentException if the user is null.
     * @throws IllegalStateException    if the cart is unexpectedly not found for the user (indicates data inconsistency).
     */
    @Override
    @Transactional(readOnly = true)
    public ShoppingCart getCartByUser(User user) {
        if (user == null || user.getId() == 0) {
            log.error("Attempted to get cart for a null user or user with invalid ID.");
            throw new IllegalArgumentException("User cannot be null and must have a valid ID.");
        }
        log.info("Fetching cart for user with ID: {}", user.getId());
        Optional<ShoppingCart> cartOptional = cartRepository.findByUser(user);
        ShoppingCart cart = cartOptional.orElseThrow(() -> {
            log.error("CRITICAL: ShoppingCart not found for user ID: {}, data inconsistency suspected.", user.getId());
            return new IllegalStateException("Shopping cart not found for user: " + user.getEmail());
        });

        log.info("Successfully fetched cart ID: {} for user ID: {}", cart.getId(), user.getId());
        return cart;
    }

    /**
     * Adds a specified quantity of a product to the user's shopping cart.
     * If the product is already in the cart, updates the quantity. Otherwise, creates a new cart item.
     * Performs stock validation before adding or updating.
     *
     * @param user        The user whose cart is being modified.
     * @param productId   The ID of the product to add.
     * @param quantity    The quantity to add (must be positive).
     * @throws IllegalArgumentException   if the quantity is not positive.
     * @throws EntityNotFoundException    if the product with the given ID doesn't exist.
     * @throws InsufficientStockException if the requested quantity exceeds available stock.
     * @throws Exception                  Potentially other runtime exceptions during processing.
     */
    @Override
    @Transactional
    public void addItemToCart(User user, long productId, int quantity) throws InsufficientStockException, EntityNotFoundException {
        if (quantity <= 0) {
            log.warn("Attempted to add non-positive quantity ({}) for product ID: {} to cart for user ID: {}", quantity, productId, user.getId());
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        ShoppingCart cart = getCartByUser(user);
        log.info("Adding item to cart ID: {} for user ID: {}", cart.getId(), user.getId());

        Product product = productService.findProductById(productId)
                .orElseThrow(() -> {
                    log.error("Product ID: {} not found. Cannot add to cart.", productId);
                    return new EntityNotFoundException("Product not found with ID: " + productId);
                });

        int currentStock = product.getStockQuantity();
        log.debug("Checking stock for Product ID: {}. Available: {}", productId, currentStock);
        Optional<ShoppingCartItem> existingItemOptional = cart.findItemByProductId(productId);
        ShoppingCartItem cartItem;

        if (existingItemOptional.isPresent()) {
            cartItem = existingItemOptional.get();
            int currentQuantityInCart = cartItem.getQuantity();
            int requestedTotalQuantity = currentQuantityInCart + quantity;

            if (requestedTotalQuantity > currentStock) {
                log.warn("Insufficient stock for Product ID: {}. Requested total: {}, Available: {}", productId, requestedTotalQuantity, currentStock);
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Available: " + currentStock + ", Requested total: " + requestedTotalQuantity);
            }

            log.debug("Product ID: {} already in cart ID: {}. Updating quantity.", productId, cart.getId());
            cartItem.setQuantity(requestedTotalQuantity);
        } else {
            if (quantity > currentStock) {
                log.warn("Insufficient stock for Product ID: {}. Requested: {}, Available: {}", productId, quantity, currentStock);
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Available: " + currentStock + ", Requested: " + quantity);
            }
            log.debug("Product ID: {} not found in cart ID: {}. Creating new item.", productId, cart.getId());
            cartItem = new ShoppingCartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setShoppingCart(cart);
            cart.addItem(cartItem);
        }
        cartItemRepository.save(cartItem);
        log.info("Successfully added/updated product ID: {} with quantity: {} to cart ID: {}", productId, quantity, cart.getId());
    }

    /**
     * Updates the quantity of a specific item in the user's shopping cart.
     * Performs stock validation before updating.
     *
     * @param user       The user whose cart item is being updated.
     * @param cartItemId The ID of the {@link ShoppingCartItem} to update.
     * @param quantity   The new quantity (must be positive).
     * @throws IllegalArgumentException   if the quantity is not positive or the item does not belong to the user's cart.
     * @throws EntityNotFoundException    if the cart item with the given ID is not found.
     * @throws InsufficientStockException if the requested quantity exceeds available stock.
     */
    @Override
    @Transactional
    public void updateItemQuantity(User user, long cartItemId, int quantity) throws InsufficientStockException, EntityNotFoundException {
        if (quantity <= 0) {
            log.warn("Attempted to update item ID: {} with non-positive quantity ({}) for user ID: {}", cartItemId, quantity, user.getId());
            throw new IllegalArgumentException("Quantity must be positive. To remove an item, use the remove endpoint.");
        }

        ShoppingCart userCart = getCartByUser(user);
        log.info("Updating item ID: {} quantity to {} in cart ID: {} for user ID: {}", cartItemId, quantity, userCart.getId(), user.getId());

        ShoppingCartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.error("ShoppingCartItem not found with ID: {}", cartItemId);
                    return new EntityNotFoundException("Cart item not found with ID: " + cartItemId);
                });

        if (!cartItem.getShoppingCart().getId().equals(userCart.getId())) {
            log.error("Security violation: User ID: {} attempted to update cart item ID: {} which belongs to cart ID: {}",
                    user.getId(), cartItemId, cartItem.getShoppingCart().getId());
            throw new IllegalArgumentException("Cart item not found in the current user's cart.");
        }

        Product product = cartItem.getProduct();
        int currentStock = product.getStockQuantity();
        log.debug("Checking stock for Product ID: {}. Available: {}", product.getId(), currentStock);

        if (quantity > currentStock) {
            log.warn("Insufficient stock for Product ID: {}. Requested: {}, Available: {}", product.getId(), quantity, currentStock);
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Available: " + currentStock + ", Requested: " + quantity);
        }

        cartItem.setQuantity(quantity);

        cartItemRepository.save(cartItem);
        log.info("Successfully updated quantity for item ID: {} to {} in cart ID: {}", cartItemId, quantity, userCart.getId());
    }

    /**
     * Removes a specific item from the user's shopping cart based on the item's ID.
     *
     * @param user       The user whose cart item is being removed.
     * @param cartItemId The ID of the {@link ShoppingCartItem} to remove.
     * @throws EntityNotFoundException  if the cart item with the given ID is not found.
     * @throws IllegalArgumentException if the item does not belong to the user's cart.
     */
    @Override
    @Transactional
    public void removeItemFromCart(User user, long cartItemId) {
        ShoppingCart userCart = getCartByUser(user);
        log.info("Removing item ID: {} from cart ID: {} for user ID: {}", cartItemId, userCart.getId(), user.getId());

        ShoppingCartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.error("ShoppingCartItem not found with ID: {} during removal attempt.", cartItemId);
                    return new EntityNotFoundException("Cart item not found with ID: " + cartItemId);
                });

        if (!cartItem.getShoppingCart().getId().equals(userCart.getId())) {
            log.error("Security violation: User ID: {} attempted to remove cart item ID: {} which belongs to cart ID: {}",
                    user.getId(), cartItemId, cartItem.getShoppingCart().getId());
            throw new IllegalArgumentException("Cart item not found in the current user's cart.");
        }

        cartItemRepository.delete(cartItem);

        log.info("Successfully removed item ID: {} from cart ID: {}", cartItemId, userCart.getId());
    }

    /**
     * Removes all items from the user's shopping cart.
     * Relies on {@code orphanRemoval=true} being set on the {@code ShoppingCart.items} collection mapping
     * for the actual deletion of {@link ShoppingCartItem} entities.
     *
     * @param user The user whose cart is to be cleared.
     */
    @Override
    @Transactional
    public void clearCart(User user) {
        ShoppingCart cart = getCartByUser(user);
        log.info("Clearing all items from cart ID: {} for user ID: {}", cart.getId(), user.getId());

        if (!cart.getItems().isEmpty()) {
            cart.getItems().clear();
            log.info("Cart items collection cleared for cart ID: {}. Orphan removal will delete items.", cart.getId());
        } else {
            log.info("Cart ID: {} was already empty.", cart.getId());
        }
    }
}