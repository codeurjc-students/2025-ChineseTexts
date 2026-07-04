package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.FounderProfile;

public interface FounderProfileRepository extends JpaRepository<FounderProfile, Long> {
}
