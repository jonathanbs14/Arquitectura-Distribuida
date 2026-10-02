package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.domain.User;
import java.util.Optional;
public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    void delete(User user);
}
