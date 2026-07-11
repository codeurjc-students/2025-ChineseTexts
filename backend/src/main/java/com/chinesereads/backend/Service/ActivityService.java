package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.ReadingLog;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.ReadingLogRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.dto.StatsDTO;

/**
 * The habit layer: records reading activity and derives the user's streak and progress
 * statistics. A "reading" is opening a text in either reader; it is idempotent per
 * user + text + day (see {@link ReadingLog}), so refreshing never inflates the stats.
 *
 * <p>The streak lives denormalized on {@link User} (currentStreak / bestStreak /
 * lastReadingDay) and is updated in O(1) on each first reading of the day — no history
 * scan. Psychology: the streak leverages loss aversion (don't break the chain) and the
 * weekly chart provides a visible goal gradient, the two cheapest retention levers.
 */
@Service
public class ActivityService {

    @Autowired
    private ReadingLogRepository readingLogRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Records that the user read {@code textKey} today and advances the streak when
     * this is the first reading of the day. Safe to call on every text open.
     */
    @Transactional
    public void recordReading(User user, String textKey) {
        LocalDate today = LocalDate.now();
        if (readingLogRepository.existsByUserAndDayAndTextKey(user, today, textKey)) {
            return; // same text already logged today
        }
        try {
            readingLogRepository.save(new ReadingLog(user, today, textKey));
        } catch (DataIntegrityViolationException e) {
            return; // concurrent duplicate (two tabs) — the unique constraint wins
        }

        // First activity of the day extends (or restarts) the streak; later readings
        // the same day change nothing.
        if (today.equals(user.getLastReadingDay())) {
            return;
        }
        if (today.minusDays(1).equals(user.getLastReadingDay())) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }
        user.setBestStreak(Math.max(user.getBestStreak(), user.getCurrentStreak()));
        user.setLastReadingDay(today);
        userRepository.save(user);
    }

    /** The user's progress snapshot (streak, totals and the last-7-days chart). */
    @Transactional(readOnly = true)
    public StatsDTO getStats(User user) {
        LocalDate today = LocalDate.now();

        // A streak is alive if the user read today or yesterday (still time to keep it).
        boolean readToday = today.equals(user.getLastReadingDay());
        boolean alive = readToday || today.minusDays(1).equals(user.getLastReadingDay());
        int currentStreak = alive ? user.getCurrentStreak() : 0;

        long textsRead = readingLogRepository.countByUser(user);
        int wordsSaved = user.getCollections() == null ? 0
                : user.getCollections().stream()
                        .mapToInt(c -> c.getFlashcards() != null ? c.getFlashcards().size() : 0)
                        .sum();

        LocalDate weekStart = today.minusDays(6);
        Map<LocalDate, Long> perDay = readingLogRepository
                .findByUserAndDayGreaterThanEqual(user, weekStart).stream()
                .collect(Collectors.groupingBy(ReadingLog::getDay, Collectors.counting()));
        List<StatsDTO.DayCountDTO> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            week.add(new StatsDTO.DayCountDTO(day, perDay.getOrDefault(day, 0L).intValue()));
        }

        return new StatsDTO(currentStreak, user.getBestStreak(), readToday, textsRead, wordsSaved, week);
    }
}
