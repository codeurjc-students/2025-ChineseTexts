package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // termsAccepted is a client->server signup input only; the entity stores the proof
    // as termsAcceptedAt, so there is nothing to map back onto the DTO.
    // newPassword is likewise an input-only field (password change): it must never be
    // populated on responses.
    @Mapping(target = "termsAccepted", ignore = true)
    @Mapping(target = "newPassword", ignore = true)
    UserDTO toDTO(User user);

    List<UserDTO> toDTO(List<User> user);

    // Every ignore below is a SERVER-MANAGED field that a client request must never be
    // able to set: admin state, audit timestamps, usage counters, streaks, SRS reminder
    // state, consent proofs and billing identifiers. They were always left unmapped on
    // purpose (UserDTO simply does not carry them); declaring them keeps that security
    // boundary explicit — and any NEW User field will still raise an unmapped-target
    // warning, forcing a conscious decision.
    @Mapping(target = "blocked", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "lastAccess", ignore = true)
    @Mapping(target = "monthlyTextCount", ignore = true)
    @Mapping(target = "usagePeriodStart", ignore = true)
    @Mapping(target = "dailyTextCount", ignore = true)
    @Mapping(target = "usageDayStart", ignore = true)
    @Mapping(target = "monthlyWordAudioCount", ignore = true)
    @Mapping(target = "monthlyPhraseAudioCount", ignore = true)
    @Mapping(target = "audioUsagePeriodStart", ignore = true)
    @Mapping(target = "monthlyChatCount", ignore = true)
    @Mapping(target = "chatUsagePeriodStart", ignore = true)
    @Mapping(target = "currentStreak", ignore = true)
    @Mapping(target = "bestStreak", ignore = true)
    @Mapping(target = "lastReadingDay", ignore = true)
    @Mapping(target = "userTexts", ignore = true)
    @Mapping(target = "termsAcceptedAt", ignore = true)
    @Mapping(target = "emailConsentAt", ignore = true)
    @Mapping(target = "lastReviewReminderDay", ignore = true)
    @Mapping(target = "unsubscribeToken", ignore = true)
    @Mapping(target = "stripeCustomerId", ignore = true)
    @Mapping(target = "stripeSubscriptionId", ignore = true)
    User toDomain(UserDTO userWithPasswordDTO);

    List<User> toDomain(List<UserDTO> users);

    // Element mapping for the nested UserDTO.collections -> User.collections list,
    // declared here (not via uses=CollectionMapper) so the generated impl stays
    // dependency-free and unit tests can keep instantiating it with plain `new`.
    // Same ignores as CollectionMapper.toDomain: both are backrefs/children the
    // persistence layer manages.
    @Mapping(target = "flashcards", ignore = true)
    @Mapping(target = "user", ignore = true)
    com.chinesereads.backend.Model.Collection collectionToDomain(CollectionDTO collectionDTO);
}
