package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserRepository extends JpaRepository<User, String>, UserRepository {
}
