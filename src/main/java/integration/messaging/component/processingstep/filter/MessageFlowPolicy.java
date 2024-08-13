package integration.messaging.component.processingstep.filter;

import org.apache.camel.Exchange;

public abstract class MessageFlowPolicy {
    public static final String REJECT_MESSAGE = "REJECT_MESSAGE";
    public static final String FILTER_NAME = "FILTER_NAME";
    public static final String REASON = "REASON";

    public void applyPolicy(Exchange exchange, String messageContent) throws FilterException {
        try {
            boolean acceptMessage = applyPolicy(messageContent);

            exchange.getMessage().setHeader(getHeader(), acceptMessage);

            if (!acceptMessage) {
                exchange.getMessage().setHeader(REASON, getFilterReason());
                exchange.getMessage().setHeader(FILTER_NAME, getName());
            }
        } catch (Exception e) {
            throw new FilterException("Error filtering the message", e);
        }
    }

    public abstract String getFilterReason();

    public abstract String getName();

    public abstract String getHeader();

    public abstract boolean applyPolicy(String messageContent) throws FilterException;
}
