package com.pokemonbattler.persistence;

import org.bson.Document;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectionTest {
    public static void main(String[] args) throws Exception {
        try (Connection conn = Database.dataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version()")) {
            rs.next();
            System.out.println("Postgres OK: " + rs.getString(1));
        }

        Document ping = Database.mongo().runCommand(new Document("ping", 1));
        System.out.println("Mongo OK: " + ping.toJson());

        Database.close();
    }
}