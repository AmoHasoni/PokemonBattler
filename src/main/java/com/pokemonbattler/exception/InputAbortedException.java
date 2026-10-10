package com.pokemonbattler.exception;

public class InputAbortedException extends RuntimeException {
    public InputAbortedException(String message) {
        super(message);
    }
}
