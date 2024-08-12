package integration.messaging.component;

import integration.messaging.component.processingstep.filter.MessageAcceptancePolicy;

/**
 * A component which can received messages.
 * 
 * @author Brendan Douglas
 */
public interface DestinationComponent extends Component {

    void addSourceComponent(SourceComponent sourceComponent);

    MessageAcceptancePolicy getMessageAcceptancePolicy();

}
