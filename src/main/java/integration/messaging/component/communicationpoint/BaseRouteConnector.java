package integration.messaging.component.communicationpoint;

import integration.messaging.component.BaseMessagingComponent;

/**
 * Base class for components designed to connect routes together.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseRouteConnector extends BaseMessagingComponent {

    public BaseRouteConnector(String componentName) {
        super(componentName);
    }

    public abstract String getName();
}
