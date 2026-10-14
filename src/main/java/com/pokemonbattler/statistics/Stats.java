package com.pokemonbattler.statistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pokemonbattler.battle.BattleOutcome;
import com.pokemonbattler.battle.BattleResult;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/*
 Persistent statistik över alla strider. Sparas som stats.json.

 - Totala vinster/förluster
 - Vinst/förlust per Pokémon
 - Hur många gånger varje attack använts (mest använda attack)
 - Hur många strider varje Pokémon gått (mest använda Pokémon)

 Flykt räknas som en strid för Pokémon, men varken som vinst eller förlust.
 */
public class Stats {
    private int totalWins;
    private int totalLosses;
    private Map<String, PokemonRecord> pokemonRecords = new TreeMap<>();
    private Map<String, Integer> attackUsage = new TreeMap<>();

    // Registrerar en strid: vinst/förlust totalt, per Pokémon och vilka attacker spelaren använde.
    public void record(BattleResult result) {
        PokemonRecord rec = pokemonRecords.computeIfAbsent(result.playerPokemon(), k -> new PokemonRecord());
        rec.addBattle();
        if (result.outcome() == BattleOutcome.WIN) {
            totalWins++;
            rec.addWin();
        } else if (result.outcome() == BattleOutcome.LOSS) {
            totalLosses++;
            rec.addLoss();
        }
        result.attacksUsed().forEach((attack, count) -> attackUsage.merge(attack, count, Integer::sum));
    }

    // Stämmer om inga strider har registrerats ännu.
    @JsonIgnore
    public boolean isEmpty() {
        return pokemonRecords.isEmpty();
    }

    // Attacken som använts flest gånger, eller tom om inga strider finns.
    public Optional<Map.Entry<String, Integer>> mostUsedAttack() {
        return attackUsage.entrySet().stream().max(Map.Entry.comparingByValue());
    }

    // Pokémon som deltagit i flest strider, eller tom om inga strider finns.
    public Optional<Map.Entry<String, PokemonRecord>> mostUsedPokemon() {
        return pokemonRecords.entrySet().stream()
                .max((a, b) -> Integer.compare(a.getValue().getBattles(), b.getValue().getBattles()));
    }

    // Bygger texten som visas under Stridsresultat.
    public String format() {
        if (isEmpty()) {
            return "Inga strider har utkämpats ännu.";
        }
        StringBuilder sb = new StringBuilder();
        int decided = totalWins + totalLosses;
        double rate = decided == 0 ? 0.0 : 100.0 * totalWins / decided;
        sb.append(String.format("Totalt: %d vinster, %d förluster (%.0f%% vinster)%n", totalWins, totalLosses, rate));
        sb.append("\nPer Pokémon:\n");
        pokemonRecords.forEach((name, r) -> sb.append(String.format(
                "  %-15s strider %3d   V %3d   F %3d   (%.0f%%)%n",
                name, r.getBattles(), r.getWins(), r.getLosses(), r.winRate())));
        sb.append("\nMest använda Pokémon: ")
                .append(mostUsedPokemon().map(e -> e.getKey() + " (" + e.getValue().getBattles() + " strider)").orElse("-"))
                .append('\n');
        sb.append("Mest använda attack:  ")
                .append(mostUsedAttack().map(e -> e.getKey() + " (" + e.getValue() + " gånger)").orElse("-"))
                .append('\n');
        return sb.toString();
    }

    // Getters/setters för Jackson.

    public int getTotalWins() { return totalWins; }

    public void setTotalWins(int totalWins) {
        this.totalWins = PokemonRecord.requireNonNegative(totalWins);
    }

    public int getTotalLosses() { return totalLosses; }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = PokemonRecord.requireNonNegative(totalLosses);
    }

    public Map<String, PokemonRecord> getPokemonRecords() { return pokemonRecords; }

    // Kopierar kartan och tar bort tomma poster från en handredigerad fil.
    public void setPokemonRecords(Map<String, PokemonRecord> pokemonRecords) {
        this.pokemonRecords = pokemonRecords == null ? new TreeMap<>() : new TreeMap<>(pokemonRecords);
        this.pokemonRecords.values().removeIf(Objects::isNull);
    }

    public Map<String, Integer> getAttackUsage() { return attackUsage; }

    // Kopierar kartan och hoppar över tomma värden. Negativa tal ger exception.
    public void setAttackUsage(Map<String, Integer> attackUsage) {
        this.attackUsage = new TreeMap<>();
        if (attackUsage != null) {
            attackUsage.forEach((k, v) -> {
                if (k != null && v != null) {
                    this.attackUsage.put(k, PokemonRecord.requireNonNegative(v));
                }
            });
        }
    }
}