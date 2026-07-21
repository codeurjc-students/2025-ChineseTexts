package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.ReadingLogRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.dto.ActivationMetricsDTO;

/**
 * Computes the campaign activation funnel over a signup cohort, entirely from the
 * local database (no Stripe calls): did the new users read on day 1, save words,
 * come back within a week (D7 retention) and hold premium today.
 *
 * <p>D7 is only judged on users whose day-7 window has fully elapsed
 * ({@code measurableD7}); counting fresher signups as "not retained" would drag the
 * rate down artificially while a campaign is still running.
 */
@Service
public class ActivationMetricsService {

    private final UserRepository userRepository;
    private final ReadingLogRepository readingLogRepository;
    private final FlashcardRepository flashcardRepository;

    public ActivationMetricsService(UserRepository userRepository,
            ReadingLogRepository readingLogRepository, FlashcardRepository flashcardRepository) {
        this.userRepository = userRepository;
        this.readingLogRepository = readingLogRepository;
        this.flashcardRepository = flashcardRepository;
    }

    public ActivationMetricsDTO metrics(LocalDate from, LocalDate to, LocalDate today) {
        List<User> cohort = userRepository.findByRegistrationDateBetween(from, to);
        if (cohort.isEmpty()) {
            return new ActivationMetricsDTO(from, to, 0, 0, 0, 0, 0, 0, List.of());
        }

        List<Long> ids = cohort.stream().map(User::getId).toList();
        Map<Long, Set<LocalDate>> readingDays = new HashMap<>();
        for (ReadingLogRepository.UserDayView view : readingLogRepository.findDistinctUserDays(ids)) {
            readingDays.computeIfAbsent(view.getUserId(), k -> new HashSet<>()).add(view.getDay());
        }
        Set<Long> savedWordIds = flashcardRepository.findUserIdsWithAnyCard(ids);

        long activatedDay1 = 0;
        long savedWord = 0;
        long measurableD7 = 0;
        long retainedD7 = 0;
        long activePremium = 0;
        // Signups per ?ref source; the empty key groups organic/direct signups.
        Map<String, Long> bySource = new HashMap<>();

        for (User user : cohort) {
            LocalDate registered = user.getRegistrationDate();
            Set<LocalDate> days = readingDays.getOrDefault(user.getId(), Set.of());

            if (days.contains(registered)) {
                activatedDay1++;
            }
            if (savedWordIds.contains(user.getId())) {
                savedWord++;
            }
            if (user.isPremiumActive()) {
                activePremium++;
            }
            if (!today.isBefore(registered.plusDays(7))) {
                measurableD7++;
                boolean cameBack = days.stream().anyMatch(
                        day -> day.isAfter(registered) && !day.isAfter(registered.plusDays(7)));
                if (cameBack) {
                    retainedD7++;
                }
            }

            String source = user.getReferralSource();
            bySource.merge(source == null || source.isBlank() ? "" : source.trim(), 1L, Long::sum);
        }

        List<ActivationMetricsDTO.SourceRow> sourceRows = bySource.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ActivationMetricsDTO.SourceRow(
                        entry.getKey().isEmpty() ? null : entry.getKey(), entry.getValue()))
                .toList();

        return new ActivationMetricsDTO(from, to, cohort.size(), activatedDay1, savedWord,
                measurableD7, retainedD7, activePremium, sourceRows);
    }
}
