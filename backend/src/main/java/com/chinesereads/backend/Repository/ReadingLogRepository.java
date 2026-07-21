package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chinesereads.backend.Model.ReadingLog;
import com.chinesereads.backend.Model.User;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    /** A (user, day) pair with reading activity, for the activation metrics. */
    interface UserDayView {
        Long getUserId();

        LocalDate getDay();
    }

    boolean existsByUserAndDayAndTextKey(User user, LocalDate day, String textKey);

    long countByUser(User user);

    /** Logs from `from` (inclusive) onwards, for the weekly activity chart. */
    List<ReadingLog> findByUserAndDayGreaterThanEqual(User user, LocalDate from);

    /** Distinct active days per user of a signup cohort (one query, no N+1). */
    @Query("SELECT DISTINCT r.user.id AS userId, r.day AS day FROM ReadingLog r WHERE r.user.id IN :userIds")
    List<UserDayView> findDistinctUserDays(@Param("userIds") Collection<Long> userIds);

    /** Removes a user's activity history; MUST run before deleting the user (FK). */
    void deleteByUser(User user);
}
