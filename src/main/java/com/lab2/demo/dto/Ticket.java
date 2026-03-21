package com.lab2.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class Ticket {
    private LocalDate serverDate;
    private Long ticketLifetimeDays;
    private LocalDate licenseActivationDate;
    private LocalDate licenseExpirationDate;
    private UUID userId;
    private UUID deviceId;
    private Boolean blocked;
}