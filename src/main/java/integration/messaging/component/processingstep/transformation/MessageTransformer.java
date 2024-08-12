package integration.messaging.component.processingstep.transformation;

import org.apache.camel.Exchange;

/**
 * Interface for all transformers.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class MessageTransformer {

    public String transform(Exchange exchange, String messageBody) throws TransformationException {
        try {
            return transformMessage(exchange, messageBody);
        } catch (Exception e) {
            throw new TransformationException("Error transforming the message", e);
        }
    }

    public abstract String transformMessage(Exchange exchange, String messageBody) throws TransformationException, Exception;
}
