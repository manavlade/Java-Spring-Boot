package com.example.demo.dto;

public class SalaryBucketDTO {
    private final String bucketLabel;
    private final  Double bucketStart;
    private final Double bucketEnd;
    private final Integer employeeCount;
    private final Double percentage;
    private final String mainRange;
    private final Double averageAge;

    public SalaryBucketDTO(String bucketLabel, Double bucketStart, Double bucketEnd,
            Integer employeeCount, Double percentage, String mainRange, Double averageAge) {
        this.bucketLabel = bucketLabel;
        this.bucketStart = bucketStart;
        this.bucketEnd = bucketEnd;
        this.employeeCount = employeeCount;
        this.percentage = percentage;
        this.mainRange = mainRange;
        this.averageAge = averageAge;
    }

    public String getBucketLabel() {
        return bucketLabel;
    }

    public Double getBucketStart() {
        return bucketStart;
    }

    public Double getBucketEnd() {
        return bucketEnd;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public String getMainRange() {
        return mainRange;
    }
    
    public Double getAverageAge(){
        return averageAge;
    }
}
