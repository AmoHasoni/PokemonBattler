package exception;

/*
  Kastas när data för en Attack bryter mot domänreglerna, t.ex. tomt
  attacknamn eller skada/träffsäkerhet utanför tillåtet intervall.
 */
public class InvalidAttackException extends RuntimeException {
    public InvalidAttackException(String message) {
        super(message);
    }
}
