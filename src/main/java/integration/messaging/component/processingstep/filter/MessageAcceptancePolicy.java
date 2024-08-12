package integration.messaging.component.processingstep.filter;

/**
 * A message filter.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class MessageAcceptancePolicy extends MessageFlowPolicy {
    public static final String ACCEPT_MESSAGE = "ACCEPT_MESSAGE";

    @Override
    public String getHeader() {
        return ACCEPT_MESSAGE;
    }
}
