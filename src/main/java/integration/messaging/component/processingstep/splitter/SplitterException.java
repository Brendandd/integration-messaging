package integration.messaging.component.processingstep.splitter;

/**
 * A message splitter exception
 * 
 * @author brendan_douglas_a
 *
 */
public class SplitterException extends Exception {

    private static final long serialVersionUID = 200520031395049131L;

    public SplitterException(String message) {
        super(message);
    }

    public SplitterException(String message, Throwable cause) {
        super(message, cause);
    }
}
