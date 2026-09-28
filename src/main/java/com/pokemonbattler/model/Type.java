package com.pokemonbattler.model;

/*
De typer en Pokémon och en Attack kan ha. Delas mellan båda klasserna
 enligt uppgiftens domänmodell (minst fyra typer krävs, vi har fem).
 */
public enum Type {
    FIRE,
    WATER,
    GRASS,
    ELECTRIC,
    NORMAL;

    /* Försöker tolka en textrad från användaren som en Type.
     Returnerar null vid ogiltig text istället för att kasta ett undantag,
     så att anroparen kan visa ett felmeddelande och be om ny inmatning. */
    public static Type fromInput(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().toUpperCase();
        for (Type t : values()) {
            if (t.name().equals(cleaned)) {
                return t;
            }
        }
        return null;
    }

    /* Bygger en läsbar lista av alla giltiga typer, t.ex. "FIRE, WATER, ....".
     Används i menyer för att visa användaren vilka val som finns. */
    public static String options() {
        StringBuilder sb = new StringBuilder();
        Type[] all = values();
        for (int i = 0; i < all.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(all[i].name());
        }
        return sb.toString();
    }
}