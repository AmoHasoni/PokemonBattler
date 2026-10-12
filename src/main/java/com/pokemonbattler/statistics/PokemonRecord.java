package com.pokemonbattler.statistics;

/* Vinster, förluster och antal strider för en enskild Pokémon.
 Setters vägrar negativa tal så att en manipulerad stats.json upptäcks som korrupt. */
public class PokemonRecord {
    private int wins;
    private int losses;
    private int battles;

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = requireNonNegative(wins);
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = requireNonNegative(losses);
    }

    public int getBattles() {
        return battles;
    }

    public void setBattles(int battles) {
        this.battles = requireNonNegative(battles);
    }

    void addWin() {
        wins++;
    }

    void addLoss() {
        losses++;
    }

    void addBattle() {
        battles++;
    }

    // Vinstandel i procent av avgjorda strider (flykt räknas inte).
    public double winRate() {
        int decided = wins + losses;
        return decided == 0 ? 0.0 : 100.0 * wins / decided;
    }

    static int requireNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Statistik får inte vara negativ.");
        }
        return value;
    }
}