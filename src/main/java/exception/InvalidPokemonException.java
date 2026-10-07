package exception;

/*
  Kastas när data för en Pokémon bryter mot domänreglerna, t.ex. tomt namn,
  HP utanför tillåtet intervall eller fel antal attacker.
  Är en RuntimeException, så anroparen måste själv komma ihåg att fånga den.
 */
public class InvalidPokemonException extends RuntimeException {
    public InvalidPokemonException(String message) {
        super(message);
    }
}
