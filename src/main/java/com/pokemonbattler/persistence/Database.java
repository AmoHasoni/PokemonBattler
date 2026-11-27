package com.pokemonbattler.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/*
 Samlar anslutningarna till båda databaserna på ett ställe.
 PostgreSQL nås via en HikariCP-pool, MongoDB via en MongoClient.
 Båda skapas en gång och återanvänds under hela programmets körning.
 */
public final class Database {

    private static final String PG_URL = "jdbc:postgresql://localhost:5432/pokemonbattler";
    private static final String PG_USER = "trainer";
    // Lösenordet läses från miljövariabeln PG_PASSWORD och ska aldrig finnas i koden.
    private static final String PG_PASSWORD = requireEnv("PG_PASSWORD");

    // Ger upp efter 2 sekunder i stället för 30 om MongoDB inte är igång.
    private static final String MONGO_URL = "mongodb://localhost:27017/?serverSelectionTimeoutMS=2000";
    private static final String MONGO_DB = "pokemonbattler";

    private static final HikariDataSource dataSource;
    private static final MongoClient mongoClient;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(PG_URL);
        config.setUsername(PG_USER);
        config.setPassword(PG_PASSWORD);
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);

        mongoClient = MongoClients.create(MONGO_URL);
    }

    private Database() {
    }

    // Stoppar programmet med ett tydligt fel om miljövariabeln saknas.
    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Miljövariabeln " + name + " är inte satt.");
        }
        return value;
    }

    // Poolen som alla SQL-anrop lånar anslutningar från.
    public static DataSource dataSource() {
        return dataSource;
    }

    // Databasen i MongoDB där stridsloggarna sparas.
    public static MongoDatabase mongo() {
        return mongoClient.getDatabase(MONGO_DB);
    }

    // Stänger poolen och Mongo-klienten när programmet avslutas.
    public static void close() {
        dataSource.close();
        mongoClient.close();
    }
}