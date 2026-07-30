package com.ecomerce.ecomerce_web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // 404 error
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse>handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request
    ){
        return buildResponse(HttpStatus.NOT_FOUND,ex.getMessage(),request);
    }
    // 409 error
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse>handleDuplicate(
            DuplicateResourceException ex , HttpServletRequest request
    ){
        return buildResponse(HttpStatus.CONFLICT,ex.getMessage(),request);
    }
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse>handleInsufficientStock(
            InsufficientStockException ex , HttpServletRequest request
    ){
        return buildResponse(HttpStatus.CONFLICT,ex.getMessage(),request);
    }
    // 400 error
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse>handleInvalidRequest(
            InvalidRequestException ex,HttpServletRequest request
    ){
        return buildResponse(HttpStatus.BAD_REQUEST,ex.getMessage(),request);
    }
    // 400 with field errors (bean validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>handleValidation(
            MethodArgumentNotValidException ex,HttpServletRequest request
    ){
        Map<String,String> fieldErrors = new HashMap<>();
        for(FieldError fe : ex.getBindingResult().getFieldErrors()){
            fieldErrors.put(fe.getField(),fe.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI()
        );
        error.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

    }
    // 403 error
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse>handleUnauthorizedAction(
            UnauthorizedActionException ex,HttpServletRequest request
    ){
        return buildResponse(HttpStatus.FORBIDDEN,ex.getMessage(),request);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse>handleAccessDenied(AccessDeniedException ex,HttpServletRequest request){
        return buildResponse(HttpStatus.FORBIDDEN,"You do not have permission to perform this action",request);
    }
    // 401 error
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse>handleAuthentication(Exception ex , HttpServletRequest request){
        return buildResponse(HttpStatus.UNAUTHORIZED,"Invalid credentials or authentication failed",request);

    }
    // 500 error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>handleGeneric(Exception ex, HttpServletRequest request){
        logger.error("Unhandled exception occurred at [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",request);
    }


    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()

        );
        return ResponseEntity.status(status).body(error);
    }

}
