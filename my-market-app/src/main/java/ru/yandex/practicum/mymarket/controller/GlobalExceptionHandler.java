package ru.yandex.practicum.mymarket.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, Model model) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        model.addAttribute("error", e.getMessage());
        model.addAttribute("errorType", "Ошибка валидации");
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException e, Model model) {
        log.warn("Ошибка состояния: {}", e.getMessage());
        model.addAttribute("error", e.getMessage());
        model.addAttribute("errorType", "Ошибка состояния");
        return "error";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolationException(ConstraintViolationException e, Model model) {
        log.warn("Ошибка валидации параметров: {}", e.getMessage());
        model.addAttribute("error",  e.getMessage());
        model.addAttribute("errorType", "Ошибка валидации");
        return "error";
    }

    @ExceptionHandler(BindException.class)
    public String handleBindException(BindException e, Model model) {
        log.warn("Ошибка привязки параметров: {}", e.getMessage());
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Ошибка валидации параметров");

        model.addAttribute("error", errorMessage);
        model.addAttribute("errorType", "Ошибка валидации");
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, Model model) {
        log.warn("Ошибка типа параметра: {}", e.getMessage());
        model.addAttribute("error", "Неверный формат параметра: " + e.getName());
        model.addAttribute("errorType", "Ошибка валидации");
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, Model model) {
        log.error("Ошибка приложения", e);
        model.addAttribute("error", "Внутренняя ошибка приложения: " + e.getMessage());
        model.addAttribute("errorType", "Ошибка приложения");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Необработанная ошибка", e);
        model.addAttribute("error", "Произошла непредвиденная ошибка");
        model.addAttribute("errorType", "Системная ошибка");
        return "error";
    }

    // Специальная обработка для favicon.ico - просто игнорируем
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(NoResourceFoundException e, Model model) {
        // Проверяем, является ли запрос на favicon.ico
        if (e.getMessage() != null && e.getMessage().contains("favicon.ico")) {
            log.debug("Запрос favicon.ico проигнорирован");
            return null; // Возвращаем null, чтобы не показывать страницу ошибки
        }

        log.warn("Ресурс не найден: {}", e.getMessage());
        model.addAttribute("error", "Запрашиваемый ресурс не найден");
        model.addAttribute("errorType", "404 Not Found");
        return "error";
    }
}