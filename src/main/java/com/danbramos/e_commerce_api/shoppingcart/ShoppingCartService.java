package com.danbramos.e_commerce_api.shoppingcart;

import com.danbramos.e_commerce_api.user.User;

public interface ShoppingCartService {

    ShoppingCart getCartByUser(User user);
    void addItemToCart(User user, long productId, int quantity) throws Exception;
    void updateItemQuantity(User user, long cartItemId, int quantity);
    void removeItemFromCart(User user, long cartItemId);
    void clearCart(User user);
}
