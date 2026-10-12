package com.pokemonbattler.ui;

import com.pokemonbattler.exception.InputAbortedException;
import com.pokemonbattler.model.Type;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/*
 Samlar all läsning från konsolen på ett ställe. Alla metoder frågar om
 tills användaren skriver något giltigt. Om input tar slut kastas
 InputAbortedException.
 */
public class ConsoleInput {
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleInput(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            throw new InputAbortedException();
        }
        return scanner.nextLine().trim();
    }

    // Läser ett heltal i intervallet [min, max]. "abc", -1, 99 osv. ger nytt försök.
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                out.printf("Ange ett tal mellan %d och %d.%n", min, max);
            } catch (NumberFormatException e) {
                out.println("Det där var inget heltal. Försök igen.");
            }
        }
    }

    public Type readType(String prompt) {
        while (true) {
            Type type = Type.fromInput(readLine(prompt + " (" + Type.options() + "): "));
            if (type != null) {
                return type;
            }
            out.println("Ogiltig typ. Tillåtna: " + Type.options());
        }
    }
}