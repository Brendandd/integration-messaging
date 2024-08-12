package integration.messaging.component.processingstep.splitter;

import org.apache.camel.Exchange;

/**
 * Interface for all splitters. A splitter will duplicate a mesage. Each message
 * will be returned as part of the array.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class MessageSplitter {

    public String[] split(Exchange exchange, String messageBody) throws SplitterException {
        try {
            String[] splitMessages = splitMessage(exchange, messageBody);
            exchange.getMessage().setHeader("splitCount", splitMessages.length);

            return splitMessages;
        } catch (Exception e) {
            throw new SplitterException("Error splitting the message", e);
        }
    }

    public abstract String[] splitMessage(Exchange exchange, String messageBody) throws SplitterException;
}
