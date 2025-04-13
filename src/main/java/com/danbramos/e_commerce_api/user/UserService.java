package com.danbramos.e_commerce_api.user;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    void saveUser(UserRegistrationDTO userDto);

    User findUserByEmail(String email);

    List<UserDTO> findAllUsers();
}