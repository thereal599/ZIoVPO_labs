package com.lab2.demo.service;

import com.lab2.demo.model.LicenseType;
import com.lab2.demo.repository.LicenseTypeRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LicenseTypeService {

    private final LicenseTypeRepository licenseTypeRepository;

    public LicenseTypeService(LicenseTypeRepository licenseTypeRepository) {
        this.licenseTypeRepository = licenseTypeRepository;
    }

    public LicenseType getTypeOrFail(UUID typeId) {
        return licenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Type not found"));
    }
}