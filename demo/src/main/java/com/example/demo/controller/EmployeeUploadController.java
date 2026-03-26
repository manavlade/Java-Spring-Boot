package com.example.demo.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Exception.ExcelValidationException;
import com.example.demo.Exception.RowValidationException;
import com.example.demo.dto.SalaryChartResponseDTO;
import com.example.demo.parser.ExcelParser;
import com.example.demo.service.EmployeeUploadService;

@CrossOrigin(origins = "http://localhost:4200")
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
                    "error {}", e.getMessage()));
        } catch (Exception e) {
            logger.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error {}", e.getMessage()));
        }
    }

    @PostMapping("/upload/report")
    public ResponseEntity<?> uploadAndDownloadReport(@RequestParam("file") MultipartFile file) {
        try {
            // calls generateReport() — NEVER throws RowValidationException
            // row errors go into the Excel file as FAIL rows, not as HTTP errors
            byte[] reportBytes = ExcelParser.generateReport(file);

            // build report filename from original filename
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("(?i)\\.(xlsx|xls)$", "")
                    : "report";
            String reportFilename = originalName + "_report.xlsx";

            // return as file download
            // Content-Disposition: attachment tells browser to download not display
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + reportFilename + "\"")
                    .body(reportBytes);

        } catch (ExcelValidationException e) {
            // file level or structure level error — return JSON error
            logger.warn("Report generation failed — file validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", e.getMessage()));

        } catch (Exception e) {
            logger.error("Report generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Something went wrong. Please try again."));
        }
    }

    @GetMapping("/ChartData")
    public ResponseEntity<SalaryChartResponseDTO> getChartData() {
        return ResponseEntity.ok(employeeUploadService.getChartData());
    }
}