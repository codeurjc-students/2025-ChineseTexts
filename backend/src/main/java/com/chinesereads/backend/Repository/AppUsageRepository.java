package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.AppUsage;

public interface AppUsageRepository extends JpaRepository<AppUsage, Long> {

    Optional<AppUsage> findByDay(LocalDate day);
}
