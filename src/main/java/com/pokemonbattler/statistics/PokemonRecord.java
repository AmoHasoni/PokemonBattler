package com.pokemonbattler.statistics;

/* Vinster, förluster och antal strider för en enskild Pokémon.
 Setters vägrar negativa tal så att en manipulerad stats.json upptäcks som korrupt. */
public class PokemonRecord {
    private int wins;
    private int losses;
    private int battles;

    // Antal vinster.
    public int getWins() {
        return wins;
    }

    // Sätter antal vinster (används av Jackson). Negativa tal ger exception.
    public void setWins(int wins) {
        this.wins = requireNonNegative(wins);
    }

    // Antal förluster.
    public int getLosses() {
        return losses;
    }

    // Sätter antal förluster (används av Jackson). Negativa tal ger exception.
    public void setLosses(int losses) {
        this.losses = requireNonNegative(losses);
    }

    // Antal strider, inklusive flykt.
    public int getBattles() {
        return battles;
    }

    // Sätter antal strider (används av Jackson). Negativa tal ger exception.
    public void setBattles(int battles) {
        this.battles = requireNonNegative(battles);
    }

    // Räknar upp en vinst. Anropas av Stats efter en strid.
    void addWin() {
        wins++;
    }

    // Räknar upp en förlust.
    void addLoss() {
        losses++;
    }

    // Räknar upp en strid (även flykt).
    void addBattle() {
        battles++;
    }

    // Vinstandel i procent av avgjorda strider (flykt räknas inte).
    public double winRate() {
        int decided = wins + losses;
        return decided == 0 ? 0.0 : 100.0 * wins / decided;
    }

    // Kastar exception om värdet är negativt, så att en manipulerad stats.json räknas som korrupt.
    static int requireNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Statistik får inte vara negativ.");
        }
        return value;
    }
}