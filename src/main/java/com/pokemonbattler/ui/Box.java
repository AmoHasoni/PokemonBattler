package com.pokemonbattler.ui;

import java.io.PrintStream;
import java.util.List;

/*
 Skriver ut text i en ram, i samma stil som menyn. Ramen blir minst lika
 bred som menyn och växer om en rad är längre.
 */
public final class Box {
    private static final int MIN_WIDTH = 34;

    // Privat konstruktor: Box har bara statiska metoder och ska inte skapas med new.
    private Box() {
    }

    // Skriver ut en flerradig text, t.ex. formatDetails(), i en ram.
    public static void print(PrintStream out, String title, String text) {
        print(out, title, text.stripTrailing().lines().toList());
    }

    // Skriver ut en titel och rader i en ram. Bredden anpassas efter längsta raden.
    public static void print(PrintStream out, String title, List<String> lines) {
        int width = MIN_WIDTH;
        for (String line : lines) {
            width = Math.max(width, line.length() + 2);
        }
        String thick = "+" + "=".repeat(width) + "+";
        out.println();
        out.println(thick);
        row(out, width, " " + title.toUpperCase());
        out.println("+" + "-".repeat(width) + "+");
        for (String line : lines) {
            row(out, width, " " + line);
        }
        out.println(thick);
    }

    // En rad inuti ramen, utfylld med mellanslag så att högerkanten blir rak.
    private static void row(PrintStream out, int width, String text) {
        out.println("|" + text + " ".repeat(Math.max(0, width - text.length())) + "|");
    }
}