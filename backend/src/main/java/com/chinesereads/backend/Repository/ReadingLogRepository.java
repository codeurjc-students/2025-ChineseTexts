package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.ReadingLog;
import com.chinesereads.backend.Model.User;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    boolean existsByUserAndDayAndTextKey(User user, LocalDate day, String textKey);

    long countByUser(User user);

    /** Logs from `from` (inclusive) onwards, for the weekly activity chart. */
    List<ReadingLog> findByUserAndDayGreaterThanEqual(User user, LocalDate from);

    /** Removes a user's activity history; MUST run before deleting the user (FK). */
    void deleteByUser(User user);
}
