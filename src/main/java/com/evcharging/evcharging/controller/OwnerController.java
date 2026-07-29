package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.dto.ChargerDTO;
import com.evcharging.evcharging.dto.StationDTO;
import com.evcharging.evcharging.service.OwnerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService){
        this.ownerService = ownerService;
    }

    @PostMapping("/stations")
    public String addStation(@RequestBody StationDTO station){
        return ownerService.addStation(station);
    }

    @PostMapping("/chargers")
    public String addCharger(@RequestBody ChargerDTO charger){
        return ownerService.addCharger(charger);
    }

    @PutMapping("/chargers/{id}/price")
    public String updatePrice(@PathVariable int id,@RequestBody ChargerDTO charger){
        return ownerService.updatePrice(id,charger.pricePerKwh);
    }
}