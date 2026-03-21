package com.lab2.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketResponse {
    private Ticket ticket;
    private String signature;
}