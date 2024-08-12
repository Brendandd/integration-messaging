package integration.messaging.component.processingstep.filter;

/**
 * A message filter.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class MessageForwardingPolicy extends MessageFlowPolicy {
    public static final String FORWARD_MESSAGE = "FORWARD_MESSAGE";

    @Override
    public String getHeader() {
        return FORWARD_MESSAGE;
    }
}
