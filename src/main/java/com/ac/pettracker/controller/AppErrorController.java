package com.ac.pettracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Global exception handler that maps common exceptions to error page responses. */
@ControllerAdvice
public class AppErrorController {

  /**
   * Handles missing required request parameters by returning a 400 error page.
   *
   * @param model the Spring MVC model
   * @return the {@code error} view name
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleMissingRequestParameter(Model model) {
    model.addAttribute("errorTitle", "Invalid search request");
    model.addAttribute("errorMessage", "Please provide both pet type and location.");
    return "error";
  }

  /**
   * Handles invalid argument exceptions (e.g., blank search parameters) with a 400 error page.
   *
   * @param model the Spring MVC model
   * @return the {@code error} view name
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgumentException(Model model) {
    model.addAttribute("errorTitle", "Invalid search request");
    model.addAttribute("errorMessage", "Please provide both pet type and location.");
    return "error";
  }

  /**
   * Handles unexpected runtime exceptions with a 500 error page.
   *
   * @param model the Spring MVC model
   * @return the {@code error} view name
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleRuntimeException(Model model) {
    model.addAttribute("errorTitle", "Something went wrong");
    model.addAttribute("errorMessage", "Please try again.");
    return "error";
  }
}
