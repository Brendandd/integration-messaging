package integration.messaging.component;

import integration.messaging.ComponentIdentifier;

/**
 * A single component in the system.
 * 
 * @author Brendan Douglas
 */
public interface Component {
    ComponentIdentifier getIdentifier();

    void setRoute(String route);

    void config() throws Exception;
}
