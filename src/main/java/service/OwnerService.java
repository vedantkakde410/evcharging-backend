package com.evcharging.evcharging.service;

import com.evcharging.evcharging.dto.ChargerDTO;
import com.evcharging.evcharging.dto.StationDTO;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Service
public class OwnerService {

    // See StationServiceApi's constructor comment - same DataSource-injection
    // change (Module P0), no query/logic changes.
    private final DataSource dataSource;

    public OwnerService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String addStation(StationDTO station){

        try(Connection conn= dataSource.getConnection()){

            PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO stations(name,location,owner_id) VALUES(?,?)"
            );

            ps.setString(1,station.name);
            ps.setString(2,station.location);

            ps.executeUpdate();

            return "Station added successfully";

        }catch(Exception e){
            e.printStackTrace();
            return "Error: "+e.getMessage();
        }
    }

    public String addCharger(ChargerDTO charger){

        try(Connection conn=dataSource.getConnection()){

            PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO chargers(station_id,power,price_per_kwh,status) VALUES(?,?,?,?)"
            );

            ps.setInt(1,charger.stationId);
            ps.setDouble(2,charger.power);
            ps.setDouble(3,charger.pricePerKwh);
            ps.setString(4,"AVAILABLE");

            ps.executeUpdate();

            return "Charger added";

        }catch(Exception e){
            e.printStackTrace();
            return "Error: "+e.getMessage();
        }
    }

    public String updatePrice(int chargerId,double price){

        try(Connection conn=dataSource.getConnection()){

            PreparedStatement ps=conn.prepareStatement(
                    "UPDATE chargers SET price_per_kwh=? WHERE id=?"
            );

            ps.setDouble(1,price);
            ps.setInt(2,chargerId);

            ps.executeUpdate();

            return "Price updated";

        }catch(Exception e){
            e.printStackTrace();
            return "Error: "+e.getMessage();
        }
    }
}