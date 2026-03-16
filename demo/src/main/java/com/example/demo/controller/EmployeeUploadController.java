package com.example.demo.controller;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Exception.ExcelValidationException;
import com.example.demo.Exception.RowValidationException;
import com.example.demo.service.EmployeeUploadService;

@RestController
@RequestMapping("/api/employee")
public class EmployeeUploadController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeUploadController.class);

    private final EmployeeUploadService employeeUploadService;

    public EmployeeUploadController(EmployeeUploadService employeeUploadService) {
        this.employeeUploadService = employeeUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadEmployee(@RequestParam("file") MultipartFile file) {
        try {
           Map<String, Object> response = employeeUploadService.uploadEmployee(file);
           return ResponseEntity.ok(response);
        } catch (RowValidationException e) {        
            throw e;                                   
        } catch (ExcelValidationException e) {
            logger.warn("File validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}