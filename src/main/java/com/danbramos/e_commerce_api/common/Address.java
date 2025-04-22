package com.danbramos.e_commerce_api.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a reusable address component, embeddable in other entities.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @NotBlank(message = "Street address cannot be blank")
    @Column(name = "address_street")
    private String street;

    @NotBlank(message = "City cannot be blank")
    @Column(name = "address_city")
    private String city;

    @NotBlank(message = "State cannot be blank")
    @Column(name = "address_state")
    private String state;

    @NotBlank(message = "Postal code cannot be blank")
    @Column(name = "address_postal_code")
    private String postalCode;

    @NotBlank(message = "Country cannot be blank")
    @Column(name = "address_country")
    private String country;

    public Address(String street, String city, String state, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

}