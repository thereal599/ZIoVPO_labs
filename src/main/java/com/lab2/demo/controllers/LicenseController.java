package com.lab2.demo.controllers;

import com.lab2.demo.dto.*;
import com.lab2.demo.model.AppUser;
import com.lab2.demo.model.License;
import com.lab2.demo.service.AppUserService;
import com.lab2.demo.service.LicenseService;
import com.lab2.demo.dto.CheckLicenseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;
    private final AppUserService appUserService;

    public LicenseController(LicenseService licenseService, AppUserService appUserService) {
        this.licenseService = licenseService;
        this.appUserService = appUserService;
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(@Valid @RequestBody ActivateLicenseRequest request,
                                                          Authentication authentication) {
        String username = authentication.getName();
        AppUser user = appUserService.getByUsernameOrFail(username);
        TicketResponse response = licenseService.activateLicense(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renew")
    public ResponseEntity<TicketResponse> renewLicense(@Valid @RequestBody RenewLicenseRequest request,
                                                       Authentication authentication) {
        String username = authentication.getName();
        AppUser user = appUserService.getByUsernameOrFail(username);

        TicketResponse response = licenseService.renewLicense(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<TicketResponse> checkLicense(@Valid @RequestBody CheckLicenseRequest request,
                                                       Authentication authentication) {
        String username = authentication.getName();
        AppUser user = appUserService.getByUsernameOrFail(username);

        TicketResponse response = licenseService.checkLicense(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createLicense(@Valid @RequestBody CreateLicenseRequest request,
                                           Authentication authentication) {
        try {
            String adminUsername = authentication.getName();
            AppUser admin = appUserService.getByUsernameOrFail(adminUsername);
            License license = licenseService.createLicense(request, admin.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(LicenseResponse.from(license));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();

            if ("Product not found".equals(message)
                    || "Type not found".equals(message)
                    || "User not found".equals(message)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", message));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", message));
        }
    }
}