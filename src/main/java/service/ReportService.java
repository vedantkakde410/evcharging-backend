package com.evcharging.evcharging.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class ReportService {

    // See StationServiceApi's constructor comment - same DataSource-injection
    // change (Module P0), no query/logic changes.
    private final DataSource dataSource;

    public ReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // TOTAL REVENUE (OWNER BASED)
    public Map<String,Object> getRevenue(int ownerId){

        Map<String,Object> result = new HashMap<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT IFNULL(SUM(b.cost),0) AS revenue " +
                            "FROM bookings b " +
                            "JOIN chargers c ON b.charger_id=c.id " +
                            "JOIN stations s ON c.station_id=s.id " +
                            "WHERE s.owner_id=?"
            );

            ps.setInt(1,ownerId);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                result.put("totalRevenue",rs.getDouble("revenue"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return result;
    }


    // TOTAL ENERGY DELIVERED (OWNER BASED)
    public Map<String,Object> getEnergy(int ownerId){

        Map<String,Object> result = new HashMap<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT IFNULL(SUM(b.energy_used),0) AS energy " +
                            "FROM bookings b " +
                            "JOIN chargers c ON b.charger_id=c.id " +
                            "JOIN stations s ON c.station_id=s.id " +
                            "WHERE s.owner_id=?"
            );

            ps.setInt(1,ownerId);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                result.put("totalEnergyDelivered",rs.getDouble("energy"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return result;
    }


    // ALL BOOKINGS (OWNER BASED)
    public List<Map<String,Object>> getAllBookings(int ownerId){

        List<Map<String,Object>> bookings = new ArrayList<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.id, u.name AS user, s.name AS station, b.energy_used, b.cost " +
                            "FROM bookings b " +
                            "JOIN users u ON b.user_id=u.id " +
                            "JOIN chargers c ON b.charger_id=c.id " +
                            "JOIN stations s ON c.station_id=s.id " +
                            "WHERE s.owner_id=?"
            );

            ps.setInt(1,ownerId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> booking = new HashMap<>();

                booking.put("bookingId",rs.getInt("id"));
                booking.put("user",rs.getString("user"));
                booking.put("station",rs.getString("station"));
                booking.put("energyUsed",rs.getDouble("energy_used"));
                booking.put("cost",rs.getDouble("cost"));

                bookings.add(booking);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return bookings;
    }
}