package exception;

public class ShortMessageException extends RuntimeException {

    public ShortMessageException(String msg) {
        super(msg);
    }
}