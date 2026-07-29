package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.dto.ReviewDTO;
import com.evcharging.evcharging.service.StationServiceApi;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StationController {

    private final StationServiceApi stationService;

    public StationController(StationServiceApi stationService) {
        this.stationService = stationService;
    }

    // Get all stations
    @GetMapping("/stations")
    public List<Map<String, Object>> getStations() {
        return stationService.getStations();
    }

    // Get chargers of a station
    @GetMapping("/stations/{id}/chargers")
    public List<Map<String, Object>> getChargers(@PathVariable int id) {
        return stationService.getChargers(id);
    }

    // Add review to station
    @PostMapping("/stations/{id}/review")
    public String addReview(@PathVariable int id, @RequestBody ReviewDTO review) {
        return stationService.addReview(id, review);
    }

    // Get reviews of station
    @GetMapping("/stations/{id}/reviews")
    public List<Map<String, Object>> getReviews(@PathVariable int id) {
        return stationService.getReviews(id);
    }
}