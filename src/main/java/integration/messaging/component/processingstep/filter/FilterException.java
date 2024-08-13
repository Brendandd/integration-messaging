package integration.messaging.component.processingstep.filter;

/**
 * An exception thrown from the filtering logic
 */
public class FilterException extends Exception {
    private static final long serialVersionUID = -6535976021157034699L;

    public FilterException(String message) {
        super(message);
    }

    public FilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
