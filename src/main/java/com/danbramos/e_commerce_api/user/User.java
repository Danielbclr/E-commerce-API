package com.danbramos.e_commerce_api.user;

import com.danbramos.e_commerce_api.role.Role;
import com.danbramos.e_commerce_api.shoppingcart.ShoppingCart;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinTable(
            name="users_roles",
            joinColumns={@JoinColumn(name="USER_ID", referencedColumnName="ID")},
            inverseJoinColumns={@JoinColumn(name="ROLE_ID", referencedColumnName="ID")})
    private List<Role> roles = new ArrayList<>();

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private ShoppingCart shoppingCart;

    public void setShoppingCart(ShoppingCart shoppingCart) {
        if (shoppingCart == null) {
            if (this.shoppingCart != null) {
                this.shoppingCart.setUser(null); // Break the link on the other side
            }
        } else {
            shoppingCart.setUser(this); // Set the link on the other side
        }
        this.shoppingCart = shoppingCart;
    }

}
