package com.pokemonbattler.persistence;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;

import java.sql.*;
import java.util.List;

public class PokemonDao {

    /*
     Sparar en ny Pokémon och alla dess attacker i EN transaktion.
     Antingen sparas Pokémon, attackerna och kopplingarna i pokemon_attack,
     eller ingenting alls. Returnerar den nya Pokémonens id.
     */
    public int savePokemon(int trainerId, Pokemon pokemon) throws SQLException {
        String insertPokemon = """
                INSERT INTO pokemon
                    (trainer_id, type_id, name, max_hp, current_hp, speed)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING pokemon_id""";
        String upsertAttack = """
                INSERT INTO attack (type_id, name, base_damage, accuracy)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (name) DO UPDATE
                    SET type_id = EXCLUDED.type_id,
                        base_damage = EXCLUDED.base_damage,
                        accuracy = EXCLUDED.accuracy
                RETURNING attack_id""";
        String insertLink = """
                INSERT INTO pokemon_attack (pokemon_id, attack_id, slot)
                VALUES (?, ?, ?)""";

        try (Connection conn = Database.dataSource().getConnection()) {
            conn.setAutoCommit(false);          // transaktionen börjar
            try {
                int pokemonId;
                try (PreparedStatement ps = conn.prepareStatement(insertPokemon)) {
                    ps.setInt(1, trainerId);
                    ps.setInt(2, typeId(conn, pokemon.getType().name()));
                    ps.setString(3, pokemon.getName());
                    ps.setInt(4, pokemon.getMaxHp());
                    ps.setInt(5, pokemon.getCurrentHp());
                    ps.setInt(6, pokemon.getSpeed());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        pokemonId = rs.getInt("pokemon_id");
                    }
                }

                List<Attack> attacks = pokemon.getAttacks();
                for (int i = 0; i < attacks.size(); i++) {
                    Attack attack = attacks.get(i);
                    int attackId;
                    try (PreparedStatement ps = conn.prepareStatement(upsertAttack)) {
                        ps.setInt(1, typeId(conn, attack.getType().name()));
                        ps.setString(2, attack.getName());
                        ps.setInt(3, attack.getBaseDamage());
                        ps.setInt(4, attack.getAccuracy());
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            attackId = rs.getInt("attack_id");
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement(insertLink)) {
                        ps.setInt(1, pokemonId);
                        ps.setInt(2, attackId);
                        ps.setInt(3, i + 1);    // slot 1–4
                        ps.executeUpdate();
                    }
                }

                conn.commit();                  // allt lyckades: spara
                return pokemonId;
            } catch (SQLException e) {
                conn.rollback();                // något misslyckades: ångra allt
                throw e;
            } finally {
                // återställ innan anslutningen går tillbaka till poolen
                conn.setAutoCommit(true);
            }
        }
    }

    // Som findOrCreateType, men på en befintlig anslutning så att den ingår i transaktionen.
    private int typeId(Connection conn, String typeName) throws SQLException {
        String sql = """
                INSERT INTO type(name) VALUES (?)
                ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                RETURNING type_id""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, typeName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("type_id");
            }
        }
    }

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