package integration.messaging;

/**
 * An exception type which needs to be retried.
 * 
 * @author Brendan Douglas
 *
 */
public class RetryableException extends MessageFlowException {
    private static final long serialVersionUID = -3712533515354380661L;

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
