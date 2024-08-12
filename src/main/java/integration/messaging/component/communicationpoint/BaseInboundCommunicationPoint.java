package integration.messaging.component.communicationpoint;

import integration.messaging.component.SourceComponent;

/**
 * Base class for all inbound communication points.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseInboundCommunicationPoint extends BaseCommunicationPoint implements SourceComponent {

    public BaseInboundCommunicationPoint(String componentName) {
        super(componentName);
    }

    public abstract String getFromUriString();
}
