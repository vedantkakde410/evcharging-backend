package com.evcharging.evcharging.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // Simple test API to check if server is running
    @GetMapping("/health")
    public String health() {
        return "EV Charging API is running!";
    }

}