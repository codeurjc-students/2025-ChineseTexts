package com.chinesereads.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.User;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByUser(User user);
}