package com.evcharging.evcharging.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class BookingServiceApi {

    // See StationServiceApi's constructor comment - same DataSource-injection
    // change (Module P0), no query/logic changes.
    private final DataSource dataSource;

    public BookingServiceApi(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Map<String,Object>> getUserBookings(int userId){

        List<Map<String,Object>> bookings = new ArrayList<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.id, s.name AS station, b.energy_used, b.charging_time, b.cost " +
                            "FROM bookings b " +
                            "JOIN chargers c ON b.charger_id=c.id " +
                            "JOIN stations s ON c.station_id=s.id " +
                            "WHERE b.user_id=?"
            );

            ps.setInt(1,userId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> booking = new HashMap<>();

                booking.put("bookingId",rs.getInt("id"));
                booking.put("station",rs.getString("station"));
                booking.put("energyUsed",rs.getDouble("energy_used"));
                booking.put("chargingTime",rs.getDouble("charging_time"));
                booking.put("cost",rs.getDouble("cost"));

                bookings.add(booking);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return bookings;
    }
}