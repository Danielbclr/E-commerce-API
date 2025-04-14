package com.danbramos.e_commerce_api.shoppingcart;

import com.danbramos.e_commerce_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingCartRepository  extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByUser(User user);
}
