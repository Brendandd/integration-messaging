package integration.messaging;

/**
 * @author Brendan Douglas
 *
 */
public class MessageFlowException extends Exception {
    private static final long serialVersionUID = -1639485569289392443L;

    public MessageFlowException(String message) {
        super(message);
    }

    public MessageFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
