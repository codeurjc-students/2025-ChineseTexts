package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.FounderItem;

public interface FounderItemRepository extends JpaRepository<FounderItem, Long> {
}
