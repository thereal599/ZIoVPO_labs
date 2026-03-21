package com.lab2.demo.service;

import com.lab2.demo.dto.*;
import com.lab2.demo.exception.ConflictException;
import com.lab2.demo.exception.ForbiddenException;
import com.lab2.demo.exception.NotFoundException;
import com.lab2.demo.model.*;
import com.lab2.demo.repository.DeviceLicenseRepository;
import com.lab2.demo.repository.DeviceRepository;
import com.lab2.demo.repository.LicenseHistoryRepository;
import com.lab2.demo.repository.LicenseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final ProductService productService;
    private final LicenseTypeService licenseTypeService;
    private final AppUserService appUserService;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;

    public LicenseService(LicenseRepository licenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          ProductService productService,
                          LicenseTypeService licenseTypeService,
                          AppUserService appUserService,
                          DeviceRepository deviceRepository,
                          DeviceLicenseRepository deviceLicenseRepository) {
        this.licenseRepository = licenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.productService = productService;
        this.licenseTypeService = licenseTypeService;
        this.appUserService = appUserService;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
    }

    @Transactional
    public License createLicense(CreateLicenseRequest request, UUID adminId) {
        Product product = productService.getProductOrFail(request.getProductId());
        LicenseType type = licenseTypeService.getTypeOrFail(request.getTypeId());
        AppUser owner = appUserService.getActiveUserOrFail(request.getOwnerId());
        AppUser admin = appUserService.getByIdOrFail(adminId);

        License license = createNewLicense(request, product, type, owner);
        License saved = licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(saved);
        history.setUser(admin);
        history.setStatus("CREATED");
        history.setChangeDate(LocalDateTime.now());
        history.setDescription("License created by admin: " + admin.getUsername());

        licenseHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, UUID userId) {
        AppUser user = appUserService.getActiveUserOrFail(userId);
        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new NotFoundException("License not found"));
        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new ForbiddenException("License is blocked");
        }
        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw new ForbiddenException("License owned by another user");
        }
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> createDevice(user, request));
        if (!device.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Device owned by another user");
        }
        boolean firstActivation = license.getUser() == null;

        if (firstActivation) {
            license.setUser(user);
            license.setFirstActivationDate(LocalDate.now());
            license.setEndingDate(calculateEndingDate(license.getFirstActivationDate(), license.getType()));
            licenseRepository.save(license);
            createDeviceLicenseIfAbsent(license, device);
            saveHistory(license, user, "ACTIVATED", "First activation");
            return buildTicketResponse(license, device);
        }
        boolean alreadyBoundToThisDevice = deviceLicenseRepository.existsByLicenseAndDevice(license, device);
        if (alreadyBoundToThisDevice) {
            saveHistory(license, user, "ACTIVATED", "Repeated activation on same device");
            return buildTicketResponse(license, device);
        }

        long currentCount = deviceLicenseRepository.countByLicense(license);
        if (currentCount >= license.getDeviceCount()) {
            throw new ConflictException("Device limit reached");
        }

        createDeviceLicenseIfAbsent(license, device);
        saveHistory(license, user, "ACTIVATED", "Activation on additional device");

        return buildTicketResponse(license, device);
    }

    private License createNewLicense(CreateLicenseRequest request,
                                     Product product,
                                     LicenseType type,
                                     AppUser owner) {
        License license = new License();
        license.setCode(generateCode());
        license.setUser(null);
        license.setProduct(product);
        license.setType(type);
        license.setFirstActivationDate(null);
        license.setEndingDate(null);
        license.setBlocked(false);
        license.setDeviceCount(request.getDeviceCount());
        license.setOwner(owner);
        license.setDescription(request.getDescription());
        return license;
    }

    private void createDeviceLicenseIfAbsent(License license, Device device) {
        if (deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
            return;
        }

        DeviceLicense deviceLicense = new DeviceLicense();
        deviceLicense.setLicense(license);
        deviceLicense.setDevice(device);
        deviceLicense.setActivationDate(LocalDate.now());

        deviceLicenseRepository.save(deviceLicense);
    }

    private LocalDate calculateEndingDate(LocalDate firstActivationDate, LicenseType type) {
        return firstActivationDate.plusDays(type.getDefaultDurationInDays());
    }

    private void saveHistory(License license, AppUser user, String status, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus(status);
        history.setChangeDate(LocalDateTime.now());
        history.setDescription(description);
        licenseHistoryRepository.save(history);
    }

    private Device createDevice(AppUser user, ActivateLicenseRequest request) {
        Device device = new Device();
        device.setUser(user);
        device.setName(request.getDeviceName());
        device.setMacAddress(request.getDeviceMac());
        return deviceRepository.save(device);
    }

    private String generateCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        } while (licenseRepository.existsByCode(code));
        return code;
    }

    private TicketResponse buildTicketResponse(License license, Device device) {
        Ticket ticket = Ticket.builder()
                .serverDate(LocalDate.now())
                .ticketLifetimeDays(calculateTicketLifetimeDays(license))
                .licenseActivationDate(license.getFirstActivationDate())
                .licenseExpirationDate(license.getEndingDate())
                .userId(license.getUser() != null ? license.getUser().getId() : null)
                .deviceId(device != null ? device.getId() : null)
                .blocked(license.getBlocked())
                .build();

        return TicketResponse.builder()
                .ticket(ticket)
                .signature("SIGNATURE_PLACEHOLDER")
                .build();
    }

    private long calculateTicketLifetimeDays(License license) {
        if (license.getEndingDate() == null) {
            return 0L;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), license.getEndingDate());
        return Math.max(days, 0L);
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, UUID userId) {
        AppUser user = appUserService.getActiveUserOrFail(userId);

        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new NotFoundException("License not found"));

        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new ForbiddenException("License is blocked");
        }

        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw new ForbiddenException("License owned by another user");
        }

        if (!isRenewable(license)) {
            throw new ConflictException("License is not renewable yet");
        }

        LocalDate baseDate = license.getEndingDate() != null
                ? license.getEndingDate()
                : LocalDate.now();

        license.setEndingDate(baseDate.plusDays(license.getType().getDefaultDurationInDays()));
        licenseRepository.save(license);

        saveHistory(license, user, "RENEWED", "License renewed");

        return buildTicketResponse(license, null);
    }

    private boolean isRenewable(License license) {
        if (license.getEndingDate() == null) {
            return true;
        }

        LocalDate today = LocalDate.now();
        LocalDate renewableFrom = today.plusDays(7);

        return !license.getEndingDate().isAfter(renewableFrom);
    }

    @Transactional
    public TicketResponse checkLicense(CheckLicenseRequest request, UUID userId) {
        AppUser user = appUserService.getActiveUserOrFail(userId);

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new NotFoundException("Device not found"));

        if (!device.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Device owned by another user");
        }

        List<License> licenses = licenseRepository.findActiveLicense(
                device.getId(),
                user.getId(),
                request.getProductId()
        );

        if (licenses.isEmpty()) {
            throw new NotFoundException("License not found");
        }

        License license = licenses.getFirst();
        return buildTicketResponse(license, device);
    }

}