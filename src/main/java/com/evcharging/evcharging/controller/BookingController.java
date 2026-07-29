package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.security.SecurityUtils;
import com.evcharging.evcharging.service.BookingServiceApi;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingServiceApi bookingServiceApi;

    public BookingController(BookingServiceApi bookingServiceApi){
        this.bookingServiceApi = bookingServiceApi;
    }

    // SecurityConfig only proves "some authenticated user" - this proves
    // it's the *right* one, so a logged-in customer can't read another
    // customer's booking history just by changing the id in the URL
    // (Module 9 IDOR fix; ADMIN bypasses via SecurityUtils).
    @GetMapping("/users/{id}/bookings")
    public List<Map<String,Object>> getUserBookings(@PathVariable int id, Authentication authentication){
        SecurityUtils.assertOwnResource(authentication, id);
        return bookingServiceApi.getUserBookings(id);
    }
}