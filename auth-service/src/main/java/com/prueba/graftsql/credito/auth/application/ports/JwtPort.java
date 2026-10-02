package com.prueba.graftsql.credito.auth.application.ports;

import com.prueba.graftsql.credito.auth.domain.User;

public interface JwtPort {
    String generateToken(User user);
    String generateRefreshToken(User user);

}
