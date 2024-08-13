package integration.messaging;

/**
 * An exception type which will not be retried.
 * 
 * @author Brendan Douglas
 *
 */
public class NonRetryableException extends MessageFlowException {
    private static final long serialVersionUID = 6399636165479131675L;

    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }

}
