package com.lab2.demo.repository;

import com.lab2.demo.model.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, UUID> {
}