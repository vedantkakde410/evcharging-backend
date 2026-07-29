package com.evcharging.evcharging.service;

import com.evcharging.evcharging.dto.ReviewDTO;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class StationServiceApi {

    // Was DriverManager.getConnection(hardcoded URL/USER/PASSWORD) - now
    // uses the shared, pooled DataSource Spring already configures from
    // spring.datasource.* (application.properties, env-var driven as of
    // Module P0) for JPA. Same connection-per-call pattern, just sourced
    // from the pool instead of opening a raw socket every time - also
    // fixes IMPROVEMENT_REPORT.md #18 (no connection pool) as a side
    // effect of removing the hardcoded credentials.
    private final DataSource dataSource;

    public StationServiceApi(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // GET ALL STATIONS (WITH RATING + CHARGER COUNT)
    public List<Map<String,Object>> getStations(){

        List<Map<String,Object>> stations = new ArrayList<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT s.id, s.name, s.location, " +
                            "IFNULL(AVG(r.rating),0) AS rating, " +
                            "COUNT(c.id) AS totalChargers, " +
                            "IFNULL(SUM(CASE WHEN c.status='AVAILABLE' THEN 1 ELSE 0 END),0) AS availableChargers " +
                            "FROM stations s " +
                            "LEFT JOIN chargers c ON s.id=c.station_id " +
                            "LEFT JOIN reviews r ON s.id=r.station_id " +
                            "GROUP BY s.id"
            );

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> s = new HashMap<>();

                s.put("id",rs.getInt("id"));
                s.put("name",rs.getString("name"));
                s.put("location",rs.getString("location"));
                s.put("rating",rs.getDouble("rating"));
                s.put("totalChargers",rs.getInt("totalChargers"));
                s.put("availableChargers",rs.getInt("availableChargers"));

                stations.add(s);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return stations;
    }


    // GET CHARGERS OF A STATION
    public List<Map<String,Object>> getChargers(int stationId){

        List<Map<String,Object>> chargers = new ArrayList<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id,power,price_per_kwh,status FROM chargers WHERE station_id=?"
            );

            ps.setInt(1,stationId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> c = new HashMap<>();

                c.put("id",rs.getInt("id"));
                c.put("power",rs.getDouble("power"));
                c.put("pricePerKwh",rs.getDouble("price_per_kwh"));
                c.put("status",rs.getString("status"));

                chargers.add(c);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return chargers;
    }


    // ADD REVIEW
    public String addReview(int stationId, ReviewDTO review){

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reviews(user_id,station_id,rating,comment) VALUES(?,?,?,?)"
            );

            ps.setInt(1,review.userId);
            ps.setInt(2,stationId);
            ps.setInt(3,review.rating);
            ps.setString(4,review.comment);

            ps.executeUpdate();

            return "Review added successfully";

        }catch(Exception e){
            e.printStackTrace();
            return "Error: "+e.getMessage();
        }
    }


    // GET REVIEWS
    public List<Map<String,Object>> getReviews(int stationId){

        List<Map<String,Object>> reviews = new ArrayList<>();

        try(Connection conn = dataSource.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id,rating,comment FROM reviews WHERE station_id=?"
            );

            ps.setInt(1,stationId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> r = new HashMap<>();

                r.put("userId",rs.getInt("user_id"));
                r.put("rating",rs.getInt("rating"));
                r.put("comment",rs.getString("comment"));

                reviews.add(r);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return reviews;
    }
}