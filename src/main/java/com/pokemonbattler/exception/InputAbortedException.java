package com.pokemonbattler.exception;

// Kastas när inmatningen tar slut (t.ex. Ctrl+D), så att App kan spara och avsluta snyggt.
public class InputAbortedException extends RuntimeException {
    public InputAbortedException() { // Fast meddelande, eftersom orsaken alltid är densamma.
        super("Inmatningen avbröts.");
    }
}
