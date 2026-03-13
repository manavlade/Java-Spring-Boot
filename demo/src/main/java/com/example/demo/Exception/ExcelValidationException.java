package com.example.demo.Exception;

public class ExcelValidationException extends RuntimeException {
    public ExcelValidationException(String message){
        super(message);
    }
}
