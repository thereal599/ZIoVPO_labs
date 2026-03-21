package com.lab2.demo.repository;

import com.lab2.demo.model.LicenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LicenseTypeRepository extends JpaRepository<LicenseType, UUID> {
}