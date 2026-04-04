package com.example.Tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.Tournament.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
}