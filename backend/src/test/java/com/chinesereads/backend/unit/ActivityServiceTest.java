package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.ReadingLog;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.ReadingLogRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.ActivityService;

/**
 * Tests the streak state machine and the stats snapshot: first reading, consecutive
 * days, broken streaks, same-day idempotency and the weekly chart shape.
 */
public class ActivityServiceTest {

    private ReadingLogRepository readingLogRepository;
    private UserRepository userRepository;
    private ActivityService activityService;

    @BeforeEach
    public void setUp() {
        readingLogRepository = mock(ReadingLogRepository.class);
        userRepository = mock(UserRepository.class);
        activityService = new ActivityService(readingLogRepository, userRepository);

        when(readingLogRepository.existsByUserAndDayAndTextKey(any(), any(), any())).thenReturn(false);
        when(readingLogRepository.findByUserAndDayGreaterThanEqual(any(), any())).thenReturn(List.of());
    }

    private User user() {
        return new User("u@u.com", "U", "pass", "en", "USER");
    }

    @Test
    @DisplayName("The first reading ever starts a streak of 1")
    public void testFirstReading() {
        User u = user();
        activityService.recordReading(u, "public:1");
        assertEquals(1, u.getCurrentStreak());
        assertEquals(1, u.getBestStreak());
        assertEquals(LocalDate.now(), u.getLastReadingDay());
        verify(readingLogRepository).save(any(ReadingLog.class));
    }

    @Test
    @DisplayName("Reading after yesterday's activity extends the streak")
    public void testConsecutiveDayExtends() {
        User u = user();
        u.setCurrentStreak(3);
        u.setBestStreak(5);
        u.setLastReadingDay(LocalDate.now().minusDays(1));

        activityService.recordReading(u, "public:2");

        assertEquals(4, u.getCurrentStreak());
        assertEquals(5, u.getBestStreak()); // best unchanged (4 < 5)
    }

    @Test
    @DisplayName("A gap resets the streak to 1")
    public void testGapResets() {
        User u = user();
        u.setCurrentStreak(9);
        u.setBestStreak(9);
        u.setLastReadingDay(LocalDate.now().minusDays(3));

        activityService.recordReading(u, "public:3");

        assertEquals(1, u.getCurrentStreak());
        assertEquals(9, u.getBestStreak()); // history preserved
    }

    @Test
    @DisplayName("A new record raises the best streak")
    public void testBestStreakRaised() {
        User u = user();
        u.setCurrentStreak(5);
        u.setBestStreak(5);
        u.setLastReadingDay(LocalDate.now().minusDays(1));

        activityService.recordReading(u, "own:4");

        assertEquals(6, u.getCurrentStreak());
        assertEquals(6, u.getBestStreak());
    }

    @Test
    @DisplayName("A second reading the same day logs the text but doesn't touch the streak")
    public void testSameDaySecondText() {
        User u = user();
        u.setCurrentStreak(2);
        u.setBestStreak(2);
        u.setLastReadingDay(LocalDate.now()); // already read today

        activityService.recordReading(u, "public:5");

        assertEquals(2, u.getCurrentStreak()); // unchanged
        verify(readingLogRepository).save(any(ReadingLog.class)); // but the text IS logged
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Re-reading the same text the same day is fully idempotent")
    public void testSameTextSameDayIdempotent() {
        User u = user();
        when(readingLogRepository.existsByUserAndDayAndTextKey(any(), any(), any())).thenReturn(true);

        activityService.recordReading(u, "public:6");

        verify(readingLogRepository, never()).save(any());
        assertEquals(0, u.getCurrentStreak());
    }

    @Test
    @DisplayName("Stats: a streak broken two days ago reads as 0, best is kept")
    public void testStatsBrokenStreak() {
        User u = user();
        u.setCurrentStreak(4);
        u.setBestStreak(7);
        u.setLastReadingDay(LocalDate.now().minusDays(2));

        var stats = activityService.getStats(u);

        assertEquals(0, stats.currentStreak());
        assertEquals(7, stats.bestStreak());
        assertFalse(stats.readToday());
    }

    @Test
    @DisplayName("Stats: yesterday's streak is still alive (there is time to keep it today)")
    public void testStatsAliveFromYesterday() {
        User u = user();
        u.setCurrentStreak(4);
        u.setBestStreak(4);
        u.setLastReadingDay(LocalDate.now().minusDays(1));

        var stats = activityService.getStats(u);

        assertEquals(4, stats.currentStreak());
        assertFalse(stats.readToday());
    }

    @Test
    @DisplayName("Stats: the weekly chart always has 7 days, oldest first")
    public void testStatsWeekShape() {
        User u = user();
        LocalDate today = LocalDate.now();
        when(readingLogRepository.findByUserAndDayGreaterThanEqual(any(), any()))
                .thenReturn(List.of(
                        new ReadingLog(u, today, "public:1"),
                        new ReadingLog(u, today, "public:2"),
                        new ReadingLog(u, today.minusDays(2), "own:3")));

        var stats = activityService.getStats(u);

        assertEquals(7, stats.week().size());
        assertEquals(today.minusDays(6), stats.week().get(0).day());
        assertEquals(2, stats.week().get(6).count());   // today
        assertEquals(1, stats.week().get(4).count());   // two days ago
        assertEquals(0, stats.week().get(0).count());
    }

    @Test
    @DisplayName("Stats: readToday is true after today's reading")
    public void testStatsReadToday() {
        User u = user();
        activityService.recordReading(u, "public:9");
        assertTrue(activityService.getStats(u).readToday());
    }
}
