package com.chinesereads.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.HallOfFameEntry;

public interface HallOfFameEntryRepository extends JpaRepository<HallOfFameEntry, Long> {

    List<HallOfFameEntry> findAllByOrderByDisplayOrderAscIdAsc();

    boolean existsBySlug(String slug);
}
