package com.danbramos.e_commerce_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Import Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Import BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder

@SpringBootApplication
public class ECommerceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ECommerceApiApplication.class, args);
	}

	@Bean // Define the PasswordEncoder bean here
	public PasswordEncoder passwordEncoder() {
		// Use BCrypt for strong password hashing
		return new BCryptPasswordEncoder();
	}
}
    