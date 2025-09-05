package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.User;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User updateUser(String id, User user);
    void deleteUser(String id);
}