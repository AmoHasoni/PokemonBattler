package com.pokemonbattler.persistence;

import com.mongodb.client.MongoCollection;
import com.pokemonbattler.battle.BattleResult;
import com.pokemonbattler.model.Pokemon;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

/*
 Sparar och läser stridsloggar i MongoDB, i collectionen battleLogs.
 Varje strid blir ett dokument där båda deltagarna ligger inbäddade i
 arrayen participants, så en logg läses i sin helhet utan JOIN.
 */
public class BattleLogRepository {
    private final MongoCollection<Document> logs;

    public BattleLogRepository(MongoCollection<Document> logs) {
        this.logs = logs;
    }

    // Kopplar upp mot collectionen battleLogs via Database.
    public static BattleLogRepository connect() {
        return new BattleLogRepository(Database.mongo().getCollection("battleLogs"));
    }

    // Sparar en avslutad strid som ett dokument.
    public void save(BattleResult result, Pokemon player, Pokemon opponent) {
        List<Document> attacks = new ArrayList<>();
        result.attacksUsed().forEach((name, count) ->
                attacks.add(new Document("attack", name).append("count", count)));

        Document log = new Document("playedAt", new Date())
                .append("outcome", result.outcome().name())
                .append("turns", result.turns())
                .append("participants", List.of(
                        participant(player, "player"),
                        participant(opponent, "wild")))
                .append("winner", winner(result))
                .append("attacksUsed", attacks);

        logs.insertOne(log);
    }

    // Alla stridsloggar där en Pokémon med det namnet deltagit, senaste först.
    public List<Document> findByPokemon(String name) {
        return logs.find(eq("participants.name", name))
                .sort(descending("playedAt"))
                .into(new ArrayList<>());
    }

    // En deltagare som inbäddat dokument. HP sparas som det var när striden tog slut.
    private static Document participant(Pokemon pokemon, String side) {
        return new Document("name", pokemon.getName())
                .append("type", pokemon.getType().name())
                .append("side", side)
                .append("hpLeft", pokemon.getCurrentHp())
                .append("maxHp", pokemon.getMaxHp());
    }

    // Vinnarens namn, eller null om spelaren flydde.
    private static String winner(BattleResult result) {
        return switch (result.outcome()) {
            case WIN -> result.playerPokemon();
            case LOSS -> result.opponentPokemon();
            case FLED -> null;
        };
    }
}