package com.example.demo.dto;

public class EmployeeSalAgeTO {
    private Integer age;
    private Double salary;

    public EmployeeSalAgeTO(Integer age, Double salary) {
        this.age = age;
        this.salary = salary;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }
    
}
