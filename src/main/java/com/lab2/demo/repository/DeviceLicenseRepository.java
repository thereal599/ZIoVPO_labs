package com.lab2.demo.repository;

import com.lab2.demo.model.Device;
import com.lab2.demo.model.DeviceLicense;
import com.lab2.demo.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {
    long countByLicense(License license);
    boolean existsByLicenseAndDevice(License license, Device device);
}