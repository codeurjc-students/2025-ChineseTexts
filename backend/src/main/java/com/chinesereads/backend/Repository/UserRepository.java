package com.chinesereads.backend.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
            String email, String name, Pageable pageable);
}