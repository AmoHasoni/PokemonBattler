package com.pokemonbattler.exception;

/*
  Kastas när data för en Attack bryter mot domänreglerna, t.ex. tomt
  attacknamn eller skada/träffsäkerhet utanför tillåtet intervall.
 */
public class InvalidAttackException extends RuntimeException {
    public InvalidAttackException(String message) {  // message förklarar vad som var fel, t.ex. "Attacknamn får inte vara tomt."
        super(message);
    }
}
