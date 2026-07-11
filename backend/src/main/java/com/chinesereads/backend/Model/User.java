package com.chinesereads.backend.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private String name;
    private String password;
    private String language;

    // Si está bloqueado, no puede iniciar sesión (Spring Security lo rechaza).
    private boolean blocked = false;
    // Fecha de alta y última conexión, usadas por el panel de administración.
    private LocalDate registrationDate;
    private LocalDateTime lastAccess;

    // Contador mensual de creaciones de texto propio (OCR + pegado) para el límite
    // por usuario. Se reinicia cuando cambia el mes (ver usagePeriodStart).
    private int monthlyTextCount = 0;
    private LocalDate usagePeriodStart;

    // Contador diario de creaciones, usado sólo por el tope de "uso justo" de los
    // usuarios PREMIUM (que no tienen límite mensual ni pasan por el fusible global).
    // Se reinicia cuando cambia el día (ver usageDayStart). Null en cuentas antiguas.
    private int dailyTextCount = 0;
    private LocalDate usageDayStart;

    // Contadores mensuales de audio (TTS) del plan gratuito. El audio es de pago
    // (Google TTS) y sólo accesible tras registro. Se separan en dos cubos porque
    // cuestan muy distinto: la palabra suelta (barata, núcleo de la lectura) tiene un
    // cupo alto, y la frase/texto completo (caro) uno escaso — que es el gancho premium.
    // Premium y admin están exentos. Se reinician al cambiar el mes (audioUsagePeriodStart).
    private int monthlyWordAudioCount = 0;
    private int monthlyPhraseAudioCount = 0;
    private LocalDate audioUsagePeriodStart;

    // Contador mensual del chat IA contextual (de pago: llama a DeepSeek). Sólo tras
    // registro; el plan gratuito tiene un cupo escaso y premium/admin son ilimitados.
    // Se reinicia al cambiar el mes (ver chatUsagePeriodStart).
    private int monthlyChatCount = 0;
    private LocalDate chatUsagePeriodStart;

    // Racha de lectura (capa de hábito): días consecutivos con al menos una lectura.
    // Se actualiza al registrar una lectura (ver ActivityService); lastReadingDay
    // permite detectar si la racha sigue viva sin recorrer el historial.
    private int currentStreak = 0;
    private int bestStreak = 0;
    private LocalDate lastReadingDay;

    // Prueba de consentimiento (RGPD): momento en que el usuario aceptó los términos
    // de uso al registrarse. Null en cuentas anteriores a esta funcionalidad.
    private LocalDateTime termsAcceptedAt;

    // Suscripción PREMIUM caducable: el usuario disfruta del plan premium mientras
    // premiumUntil sea una fecha/hora futura. Null (o pasada) = plan gratuito. Lo fija
    // un administrador o la pasarela de pago (Stripe). Es la ÚNICA fuente de verdad del
    // estado premium (no un rol estático, que no "caducaría" solo).
    private LocalDateTime premiumUntil;

    // Identificadores de Stripe para reconciliar los webhooks con este usuario y para
    // abrir el portal de gestión de la suscripción. Null hasta el primer pago.
    private String stripeCustomerId;
    private String stripeSubscriptionId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collection> collections = new ArrayList<>();

    // Textos privados del usuario. Cascade para que al borrar el usuario se borren
    // también sus textos (y sus palabras) sin violar la clave foránea.
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserText> userTexts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;

    public User(){}

    public User(String email, String name, String password, String language, String... roles){
        this.email = email;
        this.name = name;
        this.password = password;
        this.language = language;
        if (roles == null) {
            this.setRoles(Collections.singletonList("USER"));
        } else {
            this.roles = List.of(roles);
        }
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getLanguage(){
        return language;
    }

    public List<Collection> getCollections(){
        return this.collections;
    }

    public List<String> getRoles() {
		return roles;
	}

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public void setCollections(List<Collection> collections){
        this.collections = collections;
    }

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(LocalDateTime lastAccess) {
        this.lastAccess = lastAccess;
    }

    public int getMonthlyTextCount() {
        return monthlyTextCount;
    }

    public void setMonthlyTextCount(int monthlyTextCount) {
        this.monthlyTextCount = monthlyTextCount;
    }

    public LocalDate getUsagePeriodStart() {
        return usagePeriodStart;
    }

    public void setUsagePeriodStart(LocalDate usagePeriodStart) {
        this.usagePeriodStart = usagePeriodStart;
    }

    public int getDailyTextCount() {
        return dailyTextCount;
    }

    public void setDailyTextCount(int dailyTextCount) {
        this.dailyTextCount = dailyTextCount;
    }

    public LocalDate getUsageDayStart() {
        return usageDayStart;
    }

    public void setUsageDayStart(LocalDate usageDayStart) {
        this.usageDayStart = usageDayStart;
    }

    public int getMonthlyWordAudioCount() {
        return monthlyWordAudioCount;
    }

    public void setMonthlyWordAudioCount(int monthlyWordAudioCount) {
        this.monthlyWordAudioCount = monthlyWordAudioCount;
    }

    public int getMonthlyPhraseAudioCount() {
        return monthlyPhraseAudioCount;
    }

    public void setMonthlyPhraseAudioCount(int monthlyPhraseAudioCount) {
        this.monthlyPhraseAudioCount = monthlyPhraseAudioCount;
    }

    public LocalDate getAudioUsagePeriodStart() {
        return audioUsagePeriodStart;
    }

    public void setAudioUsagePeriodStart(LocalDate audioUsagePeriodStart) {
        this.audioUsagePeriodStart = audioUsagePeriodStart;
    }

    public int getMonthlyChatCount() {
        return monthlyChatCount;
    }

    public void setMonthlyChatCount(int monthlyChatCount) {
        this.monthlyChatCount = monthlyChatCount;
    }

    public LocalDate getChatUsagePeriodStart() {
        return chatUsagePeriodStart;
    }

    public void setChatUsagePeriodStart(LocalDate chatUsagePeriodStart) {
        this.chatUsagePeriodStart = chatUsagePeriodStart;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public LocalDate getLastReadingDay() {
        return lastReadingDay;
    }

    public void setLastReadingDay(LocalDate lastReadingDay) {
        this.lastReadingDay = lastReadingDay;
    }

    public List<UserText> getUserTexts() {
        return userTexts;
    }

    public void setUserTexts(List<UserText> userTexts) {
        this.userTexts = userTexts;
    }

    public LocalDateTime getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
    }

    public LocalDateTime getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(LocalDateTime premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    /** True while the PREMIUM subscription is active (grant not yet expired). */
    public boolean isPremiumActive() {
        return premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now());
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }
}