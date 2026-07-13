package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.UserRepository;

/**
 * Daily SRS review-reminder email (retention loop on top of the SM-2 feature).
 *
 * A user only gets the email when EVERY condition holds, so it stays useful and
 * never becomes spam:
 *   1. they opted in (User.emailConsent, GDPR) and are not blocked;
 *   2. they have not been reminded today (lastReviewReminderDay — max 1/day even
 *      if the job re-runs after a mid-day server restart);
 *   3. they have not read today (lastReadingDay — an active user needs no nudge);
 *   4. they actually have flashcards due (countDue &gt; 0 — nothing due, no email).
 *
 * Failures are per-user: one bad address never stops the batch, and a failed send
 * is NOT marked as sent, so it naturally retries the next day.
 */
@Service
public class ReviewReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReviewReminderService.class);

    private final UserRepository userRepository;

    private final FlashcardRepository flashcardRepository;

    private final EmailService emailService;

    // Kill switch independent of the Brevo credentials (see application.properties).
    @Value("${reminder.enabled:true}")
    private boolean enabled;

    // The zone defines both WHEN the job fires and what "today" means for the
    // due-count, so a card due "today" is due on the user's calendar, not UTC's.
    @Value("${reminder.zone:Europe/Madrid}")
    private String zone;

    public ReviewReminderService(UserRepository userRepository, FlashcardRepository flashcardRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.flashcardRepository = flashcardRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${reminder.cron:0 0 18 * * *}", zone = "${reminder.zone:Europe/Madrid}")
    public void sendDailyReviewReminders() {
        if (!enabled || !emailService.isConfigured()) {
            return;
        }
        int sent = remindUsersDue(LocalDate.now(ZoneId.of(zone)));
        log.info("Review reminder job finished: {} email(s) sent", sent);
    }

    /**
     * Runs one reminder pass for the given day. Separated from the scheduled
     * entry point so tests can drive it with a fixed date. Returns emails sent.
     */
    public int remindUsersDue(LocalDate today) {
        int sent = 0;
        for (User user : userRepository.findByEmailConsentTrueAndBlockedFalse()) {
            try {
                if (today.equals(user.getLastReviewReminderDay())) {
                    continue; // already reminded today
                }
                if (today.equals(user.getLastReadingDay())) {
                    continue; // already active today — a nudge would only annoy
                }
                long due = flashcardRepository.countDue(user, today);
                if (due <= 0) {
                    continue; // nothing to review — no email
                }
                if (user.getUnsubscribeToken() == null || user.getUnsubscribeToken().isBlank()) {
                    user.setUnsubscribeToken(UUID.randomUUID().toString());
                }
                emailService.sendReviewReminderEmail(user.getEmail(), user.getName(),
                        user.getLanguage(), due, user.getCurrentStreak(), user.getUnsubscribeToken());
                // Only mark AFTER a successful send: a failure above skips this,
                // so the user is retried on the next daily run.
                user.setLastReviewReminderDay(today);
                userRepository.save(user);
                sent++;
            } catch (Exception e) {
                log.warn("Review reminder failed for {} (continuing with the rest)", user.getEmail(), e);
            }
        }
        return sent;
    }
}
