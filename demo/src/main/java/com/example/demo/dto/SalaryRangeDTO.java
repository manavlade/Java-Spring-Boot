package com.example.demo.dto;

import java.util.List;

public class SalaryRangeDTO {
    private String rangeLabel;
    private Integer totalEmployee;
    private Double averageSalary;
    private Double percentageOfTotal; // like total = 100 so how mny % lie in this range
    private List<SalaryBucketDTO> buckets;

     public SalaryRangeDTO(String rangeLabel, Integer totalEmployee, Double averageSalary,
                         Double percentageOfTotal, List<SalaryBucketDTO> buckets) {
        this.rangeLabel = rangeLabel;
        this.totalEmployee = totalEmployee;
        this.averageSalary = averageSalary;
        this.percentageOfTotal = percentageOfTotal;
        this.buckets = buckets;
    }

    public String getRangeLabel() { return rangeLabel; }
    public Integer getTotalEmployees() { return totalEmployee; }
    public Double getAverageSalary() { return averageSalary; }
    public Double getPercentageOfTotal() { return percentageOfTotal; }
    public List<SalaryBucketDTO> getBuckets() { return buckets; }

}
