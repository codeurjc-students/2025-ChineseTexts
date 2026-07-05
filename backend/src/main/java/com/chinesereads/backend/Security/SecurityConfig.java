package com.chinesereads.backend.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.chinesereads.backend.Security.jwt.JwtRequestFilter;
import com.chinesereads.backend.Security.jwt.UnauthorizedHandlerJwt;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    RepositoryUserDetailsService userDetailsService;

    @Autowired
    private UnauthorizedHandlerJwt unauthorizedHandlerJwt;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
            .securityMatcher("/api/**")
            .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt));

        http
            .authorizeHttpRequests(authorize -> authorize
                // PUBLIC
                .requestMatchers(HttpMethod.GET, "/api/words/textWords").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/texts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/founder/**").permitAll()
                // USER
                .requestMatchers(HttpMethod.GET, "/api/flashcards/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/flashcards/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/collections/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/collections/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/collections/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/collections/**").hasAnyRole("USER", "ADMIN")
                // ADMIN
                .requestMatchers(HttpMethod.POST, "/api/texts/validate").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/texts").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/texts/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/words").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/words/dictionary").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/words/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/words/**").hasAnyRole("ADMIN")
                // USER self-service (must precede the ADMIN wildcards below — first match wins)
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/me/check-password").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/me/password").hasAnyRole("USER", "ADMIN")
                // ADMIN user management (list, detail, edit, block, delete by id)
                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/*").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/ai/**").hasAnyRole("ADMIN")
                // Perfil del creador: escritura sólo ADMIN (el GET ya es público arriba)
                .requestMatchers(HttpMethod.POST, "/api/founder/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/founder/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/founder/**").hasAnyRole("ADMIN")
                .anyRequest().permitAll()
            );

        http.formLogin(formLogin -> formLogin.disable());
        http.csrf(csrf -> csrf.disable());
        http.httpBasic(httpBasic -> httpBasic.disable());
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}