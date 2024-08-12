package integration.messaging.component.processingstep.transformation;

/**
 * A transformation exception
 * 
 * @author brendan_douglas_a
 *
 */
public class TransformationException extends Exception {

    private static final long serialVersionUID = 200520031395049131L;

    public TransformationException(String message) {
        super(message);
    }

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

}
