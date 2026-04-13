package com.eaglebank.cbs.core_engine.repository;

import com.eaglebank.cbs.core_engine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

	Optional<User> findByUsername(String username);
}
