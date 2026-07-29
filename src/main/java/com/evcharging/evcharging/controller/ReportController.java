package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.security.SecurityUtils;
import com.evcharging.evcharging.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/owner/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    // SecurityConfig's hasAnyRole("OWNER","ADMIN") only proves the caller is
    // *an* owner - this proves it's *their own* numbers being requested, not
    // another owner's (Module 9 IDOR fix; ADMIN bypasses via SecurityUtils).
    @GetMapping("/revenue/{ownerId}")
    public Map<String,Object> getRevenue(@PathVariable int ownerId, Authentication authentication){
        SecurityUtils.assertOwnResource(authentication, ownerId);
        return reportService.getRevenue(ownerId);
    }


    @GetMapping("/energy/{ownerId}")
    public Map<String,Object> getEnergy(@PathVariable int ownerId, Authentication authentication){
        SecurityUtils.assertOwnResource(authentication, ownerId);
        return reportService.getEnergy(ownerId);
    }


    @GetMapping("/bookings/{ownerId}")
    public List<Map<String,Object>> getBookings(@PathVariable int ownerId, Authentication authentication){
        SecurityUtils.assertOwnResource(authentication, ownerId);
        return reportService.getAllBookings(ownerId);
    }

}