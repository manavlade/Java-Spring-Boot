package com.example.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.EmployeeSalAgeTO;
import com.example.demo.models.EmployeeUpload;
import com.example.demo.parser.ExcelParser;
import com.example.demo.repository.EmployeeUploadRepository;

@Service
public class EmployeeUploadService {

    private final EmployeeUploadRepository employeeUploadRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmployeeUploadService.class);

    public EmployeeUploadService(EmployeeUploadRepository employeeUploadRepository) {
        this.employeeUploadRepository = employeeUploadRepository;
    }

    public List<EmployeeSalAgeTO> getChartData(){
        return employeeUploadRepository.findAgeAndSalary();
     }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadEmployee(MultipartFile file) {

        Map<String, Object> parsed = ExcelParser.excelParser(file);
        List<EmployeeUpload> parsedEmployees = (List<EmployeeUpload>) parsed.get("employees");
        List<String> warnings = (List<String>) parsed.get("warnings");

        Map<String, EmployeeUpload> existingEmployeeMap = employeeUploadRepository
                .findAll()
                .stream()
                .collect(Collectors.toMap(
                        EmployeeUpload::getEmail,
                        emp -> emp,
                        (existing, duplicate) -> existing));

        logger.info("Found {} existing employees in DB", existingEmployeeMap.size());

        List<EmployeeUpload> toInsert = new ArrayList<>();
        List<EmployeeUpload> toUpdate = new ArrayList<>();

        for (EmployeeUpload parsedEmp : parsedEmployees) {
            EmployeeUpload existing = existingEmployeeMap.get(parsedEmp.getEmail());

            if (existing != null) {
                existing.setName(parsedEmp.getName());
                existing.setPassword(parsedEmp.getPassword());
                existing.setAge(parsedEmp.getAge());
                existing.setSalary(parsedEmp.getSalary());
                toUpdate.add(existing);
                logger.info("Queued for UPDATE → email: {}", parsedEmp.getEmail());
            } else {
                toInsert.add(parsedEmp);
                logger.info("Queued for INSERT → email: {}", parsedEmp.getEmail());
            }
        }

        List<EmployeeUpload> inserted = employeeUploadRepository.saveAll(toInsert);
        List<EmployeeUpload> updated = employeeUploadRepository.saveAll(toUpdate);

        logger.info("Upload complete → inserted: {}, updated: {}", inserted.size(), updated.size());

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Upload successful");
        response.put("totalInserted", inserted.size());
        response.put("totalUpdated", updated.size());
        response.put("totalProcessed", inserted.size() + updated.size());

        // Only add warnings if there are any
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }

        return response;
    }
}
