package integration.messaging;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all Apache Camel routes.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseMessagingCamelRoute extends RouteBuilder {

    @Autowired
    protected MessageProcessor messageProcessor;

    protected ComponentIdentifier identifier;

    protected boolean isInboundRunning;
    protected boolean isOutboundRunning;

    public void setIdentifier(ComponentIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setInboundRunning(boolean isInboundRunning) {
        this.isInboundRunning = isInboundRunning;
    }

    public void setOutboundRunning(boolean isOutboundRunning) {
        this.isOutboundRunning = isOutboundRunning;
    }
}
