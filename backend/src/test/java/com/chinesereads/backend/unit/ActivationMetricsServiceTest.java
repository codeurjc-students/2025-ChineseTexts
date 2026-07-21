package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.ReadingLogRepository;
import com.chinesereads.backend.Repository.ReadingLogRepository.UserDayView;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.ActivationMetricsService;
import com.chinesereads.backend.dto.ActivationMetricsDTO;

/**
 * Campaign activation funnel over a signup cohort: day-1 activation, saved words,
 * D7 retention (only over users whose day-7 window has elapsed) and the per-source
 * signup breakdown. All repository I/O is mocked — only the funnel logic runs here.
 */
public class ActivationMetricsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 31);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    private UserRepository userRepository;
    private ReadingLogRepository readingLogRepository;
    private FlashcardRepository flashcardRepository;
    private ActivationMetricsService service;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        readingLogRepository = mock(ReadingLogRepository.class);
        flashcardRepository = mock(FlashcardRepository.class);
        service = new ActivationMetricsService(userRepository, readingLogRepository,
                flashcardRepository);
    }

    private User user(long id, LocalDate registered, String referralSource) {
        User user = new User(id + "@t.com", "U" + id, "pass", "en", "USER");
        user.setId(id);
        user.setRegistrationDate(registered);
        user.setReferralSource(referralSource);
        return user;
    }

    private UserDayView day(long userId, LocalDate day) {
        return new UserDayView() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public LocalDate getDay() {
                return day;
            }
        };
    }

    @Test
    @DisplayName("An empty cohort short-circuits: zero metrics, no activity queries")
    public void emptyCohortShortCircuits() {
        when(userRepository.findByRegistrationDateBetween(FROM, TO)).thenReturn(List.of());

        ActivationMetricsDTO metrics = service.metrics(FROM, TO, TODAY);

        assertEquals(0, metrics.signups());
        assertEquals(List.of(), metrics.bySource());
        verify(readingLogRepository, never()).findDistinctUserDays(any());
        verify(flashcardRepository, never()).findUserIdsWithAnyCard(any());
    }

    @Test
    @DisplayName("Day-1 activation counts a reading log on the registration day itself")
    public void day1ActivationNeedsALogOnTheSignupDay() {
        LocalDate registered = LocalDate.of(2026, 7, 10);
        when(userRepository.findByRegistrationDateBetween(FROM, TO))
                .thenReturn(List.of(user(1, registered, null), user(2, registered, null)));
        // User 1 read on signup day; user 2 only the day after.
        when(readingLogRepository.findDistinctUserDays(any())).thenReturn(List.of(
                day(1, registered), day(2, registered.plusDays(1))));
        when(flashcardRepository.findUserIdsWithAnyCard(any())).thenReturn(Set.of(2L));

        ActivationMetricsDTO metrics = service.metrics(FROM, TO, TODAY);

        assertEquals(2, metrics.signups());
        assertEquals(1, metrics.activatedDay1());
        assertEquals(1, metrics.savedWord());
    }

    @Test
    @DisplayName("D7 only judges users whose day-7 window has elapsed; day-1 reads don't retain")
    public void d7CountsOnlyMeasurableUsersAndReturnVisits() {
        LocalDate old = TODAY.minusDays(10);
        // Retained: registered 10 days ago, came back on day 3.
        // Not retained: registered 10 days ago, only ever read on signup day.
        // Not measurable: registered 3 days ago (window still open), even having read.
        when(userRepository.findByRegistrationDateBetween(FROM, TO)).thenReturn(List.of(
                user(1, old, null), user(2, old, null), user(3, TODAY.minusDays(3), null)));
        when(readingLogRepository.findDistinctUserDays(any())).thenReturn(List.of(
                day(1, old), day(1, old.plusDays(3)),
                day(2, old),
                day(3, TODAY.minusDays(2))));
        when(flashcardRepository.findUserIdsWithAnyCard(any())).thenReturn(Set.of());

        ActivationMetricsDTO metrics = service.metrics(FROM, TO, TODAY);

        assertEquals(2, metrics.measurableD7());
        assertEquals(1, metrics.retainedD7());
    }

    @Test
    @DisplayName("Signups group by ?ref source, sorted by volume, with organic as the null row")
    public void sourceBreakdownGroupsAndSorts() {
        LocalDate registered = LocalDate.of(2026, 7, 10);
        when(userRepository.findByRegistrationDateBetween(FROM, TO)).thenReturn(List.of(
                user(1, registered, "MARIA40"), user(2, registered, "MARIA40"),
                user(6, registered, "MARIA40"),
                user(3, registered, "LI20"), user(4, registered, null),
                user(5, registered, "   ")));
        when(readingLogRepository.findDistinctUserDays(any())).thenReturn(List.of());
        when(flashcardRepository.findUserIdsWithAnyCard(any())).thenReturn(Set.of());

        List<ActivationMetricsDTO.SourceRow> rows = service.metrics(FROM, TO, TODAY).bySource();

        assertEquals(3, rows.size());
        assertEquals("MARIA40", rows.get(0).source());
        assertEquals(3, rows.get(0).signups());
        // Blank sources count as organic (null), together with users with no ?ref at all.
        assertEquals(2, rows.stream().filter(r -> r.source() == null)
                .mapToLong(ActivationMetricsDTO.SourceRow::signups).sum());
    }

    @Test
    @DisplayName("Active premium counts unexpired grants only")
    public void activePremiumCountsUnexpiredGrants() {
        LocalDate registered = LocalDate.of(2026, 7, 10);
        User premium = user(1, registered, null);
        premium.setPremiumUntil(LocalDateTime.now().plusDays(30));
        User expired = user(2, registered, null);
        expired.setPremiumUntil(LocalDateTime.now().minusDays(1));
        when(userRepository.findByRegistrationDateBetween(FROM, TO))
                .thenReturn(List.of(premium, expired, user(3, registered, null)));
        when(readingLogRepository.findDistinctUserDays(any())).thenReturn(List.of());
        when(flashcardRepository.findUserIdsWithAnyCard(any())).thenReturn(Set.of());

        assertEquals(1, service.metrics(FROM, TO, TODAY).activePremium());
    }
}
