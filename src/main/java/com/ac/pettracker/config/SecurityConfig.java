package com.ac.pettracker.config;

import com.ac.pettracker.security.SessionAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Spring Security configuration: centralized authorization rules and CSRF protection. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Provides the application-wide password encoder (BCrypt). */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures the security filter chain.
   *
   * <p>{@link SessionAuthFilter} bridges the custom session-based auth (set by {@code
   * AuthController}) into Spring Security's {@code SecurityContext} so that standard {@code
   * authenticated()} checks enforce access control in one place rather than in every controller.
   *
   * <ul>
   *   <li>Public routes: landing page, sign-up, auth endpoints, static assets, and error page.
   *   <li>All other routes require an authenticated session.
   *   <li>Unauthenticated requests to protected routes redirect to {@code /}.
   * </ul>
   *
   * @param http the {@link HttpSecurity} builder
   * @param sessionAuthFilter the filter that populates the {@code SecurityContext} from session
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, SessionAuthFilter sessionAuthFilter) throws Exception {
    http.addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/", "/signup", "/auth/**", "/css/**", "/images/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint((req, res, e) -> res.sendRedirect("/")))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
    return http.build();
  }
}
