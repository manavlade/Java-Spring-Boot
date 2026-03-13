package com.example.demo.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.models.EmployeeUpload;

@Repository 
public interface  EmployeeUploadRepository extends JpaRepository<EmployeeUpload, Long> {

    Optional<EmployeeUpload> findByEmail(String emai);

    @Query("SELECT e.email FROM EmployeeUpload e")
    Set<String> findAllEmails();
}
