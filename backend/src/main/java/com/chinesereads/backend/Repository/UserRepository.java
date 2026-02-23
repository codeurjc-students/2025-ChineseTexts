package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    
}