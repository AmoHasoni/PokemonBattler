package com.pokemonbattler.exception;

public class InputAbortedException extends RuntimeException {
    public InputAbortedException() {
        super("Inmatningen avbröts.");
    }
}
