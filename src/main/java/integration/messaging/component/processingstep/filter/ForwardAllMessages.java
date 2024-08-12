package integration.messaging.component.processingstep.filter;

import org.springframework.stereotype.Component;

/**
 * A message filter which accepts all messages (does not filter). This is the
 * default behaviour.
 * 
 * @author Brendan Douglas
 *
 */
@Component("forwardAllMessages")
public class ForwardAllMessages extends MessageForwardingPolicy {

    @Override
    public boolean applyPolicy(String messageContent) throws FilterException {
        return true;
    }

    @Override
    public String getFilterReason() {
        return "";
    }

    @Override
    public String getName() {
        return "Forward All Messages";
    }
}
