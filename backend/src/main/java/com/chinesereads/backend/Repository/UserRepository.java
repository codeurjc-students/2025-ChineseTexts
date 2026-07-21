package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    /** Candidates for the daily review-reminder email (consenting, not blocked). */
    List<User> findByEmailConsentTrueAndBlockedFalse();

    /** One-click unsubscribe: resolves the token embedded in reminder emails. */
    Optional<User> findByUnsubscribeToken(String unsubscribeToken);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    Optional<User> findByStripeSubscriptionId(String stripeSubscriptionId);

    /** Users who signed up through a ?ref=CODE link (influencer attribution). */
    List<User> findByReferralSourceIsNotNull();

    /** Signup cohort of a date range (inclusive), for the activation metrics. */
    List<User> findByRegistrationDateBetween(LocalDate from, LocalDate to);

    /** Users who redeemed a Stripe promotion code at checkout (influencer attribution). */
    List<User> findByStripePromotionCodeIdIsNotNull();

    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
            String email, String name, Pageable pageable);
}