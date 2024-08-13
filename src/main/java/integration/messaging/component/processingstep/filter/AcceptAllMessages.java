package integration.messaging.component.processingstep.filter;

import org.springframework.stereotype.Component;

/**
 * A message filter which accepts all messages (does not filter). This is the
 * default behaviour.
 * 
 * @author Brendan Douglas
 *
 */
@Component("acceptAllMessages")
public class AcceptAllMessages extends MessageAcceptancePolicy {

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
        return "Accept All Messages";
    }
}
