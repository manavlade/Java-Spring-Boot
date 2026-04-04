package com.example.demo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.EmployeeSalAgeTO;
import com.example.demo.dto.SalaryBucketDTO;
import com.example.demo.dto.SalaryChartResponseDTO;
import com.example.demo.dto.SalaryRangeDTO;
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

    public SalaryChartResponseDTO getChartData() {

        List<EmployeeSalAgeTO> raw = employeeUploadRepository.findAgeAndSalary();

        List<Double> salaries = raw.stream()
                .map(EmployeeSalAgeTO::getSalary)
                .filter(s -> s != null)
                .toList();

        List<Integer> age = raw.stream()
                .map(EmployeeSalAgeTO::getAge)
                .filter(a -> a != null)
                .toList();

        int total = salaries.size();

        double overallAvg = salaries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double minSal = salaries.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxSal = salaries.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        double avgAge = age.stream().mapToInt(Integer::intValue).average().orElse(0);
        int minAge = age.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxAge = age.stream().mapToInt(Integer::intValue).max().orElse(0);

        double rangeMin = Math.floor(minSal / 10000) * 10000;
        double rangeMax = Math.ceil(maxSal / 10000) * 10000;
        double spread = rangeMax - rangeMin;

        double step = Math.round(spread / 3 / 10000) * 10000;
        if (step == 0)
            step = 10000;

        List<double[]> mainRanges = List.of(
                new double[] { rangeMin, rangeMin + step },
                new double[] { rangeMin + step, rangeMin + step * 2 },
                new double[] { rangeMin + step * 2, Double.MAX_VALUE });

        List<String> rangeLabels = List.of(
                fmt(rangeMin) + "-" + fmt(rangeMin + step),
                fmt(rangeMin + step) + "-" + fmt(rangeMin + step * 2),
                fmt(rangeMin + step * 2) + "-Max");

        List<SalaryRangeDTO> ranges = new ArrayList<>();

        String highestDensityBucket = "";
        int highestDensityCount = 0;
        String largestRangeLabel = "";
        int largestRangeCount = 0;

        for (int i = 0; i < mainRanges.size(); i++) {

            double lo = mainRanges.get(i)[0];
            double hi = mainRanges.get(i)[1];
            String label = rangeLabels.get(i);

            List<EmployeeSalAgeTO> rangeData = raw.stream()
                    .filter(e -> e.getSalary() != null && e.getSalary() >= lo && e.getSalary() < hi)
                    .toList();

            if (rangeData.isEmpty())
                continue;

            List<Double> rangeSals = rangeData.stream()
                    .map(EmployeeSalAgeTO::getSalary)
                    .toList();

            double rangeAvg = rangeSals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double pctOfTotal = Math.round((rangeSals.size() * 1000.0 / total)) / 10.0;

            double rangeMaxLocal = hi == Double.MAX_VALUE
                    ? rangeSals.stream().max(Double::compare).orElse(lo)
                    : hi;

            double spreadLocal = rangeMaxLocal - lo;

            int targetBuckets = Math.min(6, Math.max(3, rangeSals.size() / 6));
            double rawStep = spreadLocal / targetBuckets;

            double[] niceSteps = { 1000, 2000, 2500, 5000, 10000, 15000, 20000, 25000, 50000 };

            double bucketStep = Arrays.stream(niceSteps)
                    .filter(s -> s >= rawStep)
                    .findFirst()
                    .orElse(rawStep);

            List<SalaryBucketDTO> buckets = new ArrayList<>();
            double cur = lo;

            while (cur < rangeMaxLocal) {

                double bucketLo = cur;
                double bucketHi = cur + bucketStep;

                List<EmployeeSalAgeTO> inBucket = rangeData.stream()
                        .filter(e -> e.getSalary() >= bucketLo && e.getSalary() < bucketHi)
                        .toList();

                if (!inBucket.isEmpty()) {

                    double bucketPct = Math.round((inBucket.size() * 1000.0 / rangeSals.size())) / 10.0;
                    String bucketLabel = fmt(bucketLo);

                    double bucketAvgAge = inBucket.stream()
                            .map(EmployeeSalAgeTO::getAge)
                            .filter(a -> a != null)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0);

                    buckets.add(new SalaryBucketDTO(
                            bucketLabel,
                            bucketLo,
                            bucketHi,
                            inBucket.size(),
                            bucketPct,
                            label,
                            bucketAvgAge));

                    if (inBucket.size() > highestDensityCount) {
                        highestDensityCount = inBucket.size();
                        highestDensityBucket = bucketLabel;
                    }
                }

                cur += bucketStep;
            }

            if (rangeSals.size() > largestRangeCount) {
                largestRangeCount = rangeSals.size();
                largestRangeLabel = label;
            }

            ranges.add(new SalaryRangeDTO(
                    label,
                    rangeSals.size(),
                    rangeAvg,
                    pctOfTotal,
                    buckets));
        }

        return new SalaryChartResponseDTO(
                total,
                overallAvg,
                minSal,
                maxSal,
                highestDensityBucket,
                largestRangeLabel,
                ranges,
                avgAge,
                minAge,
                maxAge);
    }

    private String fmt(double val) {
        return val >= 1000 ? Math.round(val / 1000) + "k" : String.valueOf((int) val);
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
