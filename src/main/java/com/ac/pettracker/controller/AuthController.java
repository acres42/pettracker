package com.ac.pettracker.controller;

import com.ac.pettracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling user authentication: login, registration, and logout. */
@Controller
public class AuthController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Renders the home (landing) page. */
  @GetMapping("/")
  public String home() {
    return "index";
  }

  /** Renders the sign-up page. */
  @GetMapping("/signup")
  public String signup() {
    return "signup";
  }

  /**
   * Handles user login and establishes a session on success.
   *
   * @param email the user's email address
   * @param password the user's plain-text password
   * @param request the current HTTP request
   * @return a redirect to the search page on success, or {@code /?authError=login} on failure
   */
  @PostMapping("/auth/login")
  public String login(
      @RequestParam String email, @RequestParam String password, HttpServletRequest request) {
    return establishSessionAndRedirect(email, password, request);
  }

  /**
   * Handles user registration and, on success, establishes a session.
   *
   * @param email the new user's email address
   * @param password the new user's plain-text password
   * @param request the current HTTP request
   * @return a redirect to the search page on success, or {@code /?authError=register} on failure
   */
  @PostMapping("/auth/register")
  public String register(
      @RequestParam String email, @RequestParam String password, HttpServletRequest request) {
    boolean registered = authService.register(email, password);
    if (!registered) {
      return "redirect:/?authError=register";
    }
    return establishSessionAndRedirect(email, password, request);
  }

  /**
   * Invalidates the current session and redirects to the home page.
   *
   * @param session the current HTTP session
   * @return a redirect to {@code /}
   */
  @GetMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }

  private String establishSessionAndRedirect(
      String email, String password, HttpServletRequest request) {
    return authService
        .authenticate(email, password)
        .map(
            user -> {
              HttpSession existing = request.getSession(false);
              if (existing != null) {
                existing.invalidate();
              }
              HttpSession fresh = request.getSession(true);
              fresh.setAttribute("authUserId", user.getId());
              fresh.setAttribute("authUserEmail", user.getEmail());
              logger.info("AUTH_SUCCESS: user authenticated");
              return "redirect:/search";
            })
        .orElseGet(
            () -> {
              logger.warn("AUTH_FAILURE: invalid credentials");
              return "redirect:/?authError=login";
            });
  }
}
