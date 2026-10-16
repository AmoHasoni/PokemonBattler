package com.pokemonbattler.ui;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/*
 Skriver ut text i en ram, i samma stil som menyn. Ramen blir minst lika
 bred som menyn och växer om en rad är längre.
 */
public final class Box {
    private static final int MIN_WIDTH = 34;
    private static final int MAX_LINE = 70;

    // Privat konstruktor: Box har bara statiska metoder och ska inte skapas med new.
    private Box() {
    }

    // Skriver ut en flerradig text, t.ex. formatDetails(), i en ram.
    public static void print(PrintStream out, String title, String text) {
        print(out, title, text.stripTrailing().lines().toList());
    }

    // Skriver ut en titel och rader i en ram. Bredden anpassas efter längsta raden.
    public static void print(PrintStream out, String title, List<String> text) {
        List<String> lines = wrap(text);
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
    // Början på ett formulär: övre kant, titel och avdelare. Frågorna under skrivs av anroparen.
    public static void open(PrintStream out, String title) {
        out.println();
        out.println("+" + "=".repeat(MIN_WIDTH) + "+");
        row(out, MIN_WIDTH, " " + title.toUpperCase());
        out.println("+" + "-".repeat(MIN_WIDTH) + "+");
    }

    // En avdelare med rubrik inne i ett formulär, t.ex. +-- Första attacken -----+
    public static void divider(PrintStream out, String label) {
        String start = "+-- " + label + " ";
        out.println(start + "-".repeat(Math.max(0, MIN_WIDTH + 1 - start.length())) + "+");
    }

    // Avslutar ett formulär med en nedre kant.
    public static void close(PrintStream out) {
        out.println("+" + "=".repeat(MIN_WIDTH) + "+");
    }

    // Delar upp rader som är längre än MAX_LINE vid ett mellanslag, så att ramen inte blir för bred.
    private static List<String> wrap(List<String> text) {
        List<String> result = new ArrayList<>();
        for (String line : text) {
            String rest = line;
            while (rest.length() > MAX_LINE) {
                int cut = rest.lastIndexOf(' ', MAX_LINE);
                if (cut <= 0) {
                    cut = MAX_LINE;
                }
                result.add(rest.substring(0, cut));
                rest = rest.substring(cut).strip();
            }
            result.add(rest);
        }
        return result;
    }

    // En rad inuti ramen, utfylld med mellanslag så att högerkanten blir rak.
    private static void row(PrintStream out, int width, String text) {
        out.println("|" + text + " ".repeat(Math.max(0, width - text.length())) + "|");
    }
}