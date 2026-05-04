package com.ac.pettracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that bridges the application's session-based authentication with Spring Security's
 * {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>On every request the filter checks whether the session contains an {@code authUserId}
 * attribute (set by {@link com.ac.pettracker.controller.AuthController} on successful login). If
 * present, it constructs an {@link UsernamePasswordAuthenticationToken} and places it in the {@code
 * SecurityContextHolder}, satisfying Spring Security's {@code authenticated()} checks for protected
 * routes.
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session != null && session.getAttribute("authUserId") != null) {
      String email = (String) session.getAttribute("authUserEmail");
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(
              email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
  }
}
