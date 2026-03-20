package com.example.demo.Exception;

import java.util.Map;  

import org.slf4j.Logger;    
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        logger.warn("File upload rejected — size exceeded");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error {}", "File size exceeds the maximum allowed size of 5MB." 
        ));
    }

    @ExceptionHandler(ExcelValidationException.class)
    public ResponseEntity<?> handleExcelValidation(ExcelValidationException e) {
        logger.warn("Excel validation failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", e.getMessage()
        ));
    }

    @ExceptionHandler(RowValidationException.class)
    public ResponseEntity<?> handleRowValidation(RowValidationException e) {
        logger.warn("Row validation failed with {} error(s)", e.getErrors().size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "totalErrors", e.getErrors().size(),
            "errors", e.getErrors()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "Something went wrong. Please try again."
        ));
    }
}