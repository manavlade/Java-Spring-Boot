package com.example.demo.dto;

import java.util.List;

public class SalaryChartResponseDTO {
    private final Integer totalEmployees;
    private final Double overallAverageSalary;
    private final Double minSalary;
    private final Double maxSalary;
    private final String highestDensityBucket;
    private final String largestRange;
    private final List<SalaryRangeDTO> ranges;
    private final Double averageAge;
    private final Integer minAge;
    private final Integer maxAge;

    public SalaryChartResponseDTO(Integer totalEmployees, Double overallAverageSalary,
            Double minSalary, Double maxSalary,
            String highestDensityBucket, String largestRange,
            List<SalaryRangeDTO> ranges, Double averageAge, Integer minAge, Integer maxAge) {
        this.totalEmployees = totalEmployees;
        this.overallAverageSalary = overallAverageSalary;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.highestDensityBucket = highestDensityBucket;
        this.largestRange = largestRange;
        this.ranges = ranges;
        this.averageAge = averageAge;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public Integer getTotalEmployees() {
        return totalEmployees;
    }

    public Double getOverallAverageSalary() {
        return overallAverageSalary;
    }

    public Double getMinSalary() {
        return minSalary;
    }

    public Double getMaxSalary() {
        return maxSalary;
    }

    public String getHighestDensityBucket() {
        return highestDensityBucket;
    }

    public String getLargestRange() {
        return largestRange;
    }

    public List<SalaryRangeDTO> getRanges() {
        return ranges;
    }

    public Double getAverageAge() {
        return averageAge;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }
}
