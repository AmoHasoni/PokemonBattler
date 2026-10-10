package com.pokemonbattler.exception;

/*
  Kastas när en Pokémon inte kan hittas, t.ex. vid ett index som ligger
  utanför listan.
 */
public class PokemonNotFoundException extends RuntimeException {
    public PokemonNotFoundException(String message) {
        super(message);
    }
}