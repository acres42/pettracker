package com.ac.pettracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class AppErrorController {

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleMissingRequestParameter(Model model) {
    model.addAttribute("errorTitle", "Invalid search request");
    model.addAttribute("errorMessage", "Please provide both pet type and location.");
    return "error";
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgumentException(Model model) {
    model.addAttribute("errorTitle", "Invalid search request");
    model.addAttribute("errorMessage", "Please provide both pet type and location.");
    return "error";
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleRuntimeException(Model model) {
    model.addAttribute("errorTitle", "Something went wrong");
    model.addAttribute("errorMessage", "Please try again.");
    return "error";
  }
}
