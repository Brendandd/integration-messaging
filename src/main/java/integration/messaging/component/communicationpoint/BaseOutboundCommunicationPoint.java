package integration.messaging.component.communicationpoint;

import java.util.ArrayList;
import java.util.List;

import integration.messaging.component.DestinationComponent;
import integration.messaging.component.SourceComponent;

/**
 * Base class for all outbound communication points.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseOutboundCommunicationPoint extends BaseCommunicationPoint implements DestinationComponent {
    public BaseOutboundCommunicationPoint(String componentName) {
        super(componentName);
    }

    protected List<String> sourceComponentPaths = new ArrayList<>();

    public abstract String getToUriString();

    @Override
    public void addSourceComponent(SourceComponent sourceComponent) {
        this.sourceComponentPaths.add(sourceComponent.getIdentifier().getComponentPath());
    }
}
