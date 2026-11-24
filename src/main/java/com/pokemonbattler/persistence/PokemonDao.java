package com.pokemonbattler.persistence;

import java.sql.*;

public class PokemonDao {

    // Hämtar id för en typ, eller skapar typen om den inte finns.
    public int findOrCreateType(String typeName) throws SQLException {
        String select = "SELECT type_id FROM type WHERE name = ?";
        String insert = "INSERT INTO type(name) VALUES (?) RETURNING type_id";

        try (Connection connect = Database.dataSource().getConnection()) {      // lånar från poolen
            try (PreparedStatement ps = connect.prepareStatement(select)) {
                ps.setString(1, typeName);                                    // värdet skickas separat
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("type_id");
                    }
                }
            }
            try (PreparedStatement ps = connect.prepareStatement(insert)) {
                ps.setString(1, typeName);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt("type_id");                              // id:t från RETURNING
                }
            }
        }                                                                     // anslutningen lämnas tillbaka
    }
}
