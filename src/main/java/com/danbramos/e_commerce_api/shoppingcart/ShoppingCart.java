package com.danbramos.e_commerce_api.shoppingcart;

import com.danbramos.e_commerce_api.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShoppingCartItem> items = new ArrayList<>();

    public void addItem(ShoppingCartItem item) {
        items.add(item);
        item.setShoppingCart(this);
    }

    public void removeItem(ShoppingCartItem item) {
        items.remove(item);
        item.setShoppingCart(null);
    }

    public Optional<ShoppingCartItem> findItemByProductId(long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId() == productId)
                .findFirst();
    }
}
