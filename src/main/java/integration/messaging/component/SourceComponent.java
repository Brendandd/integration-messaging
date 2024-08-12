package integration.messaging.component;

import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * A component which is a source of messages.
 * 
 * @author Brendan Douglas
 */
public interface SourceComponent extends Component {

    MessageForwardingPolicy getMessageForwardingPolicy();

}
