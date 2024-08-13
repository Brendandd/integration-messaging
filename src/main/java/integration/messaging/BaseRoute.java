package integration.messaging;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import integration.core.service.ConfigurationService;
import integration.messaging.component.Component;
import integration.messaging.component.DestinationComponent;
import integration.messaging.component.RouteTemplates;
import integration.messaging.component.SourceComponent;

/**
 * Base class for all routes. A route is composed of components and determines
 * the message flow within a system.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseRoute {

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected ConfigurationService configurationService;

    protected String name;

    private List<Component> components = new ArrayList<Component>();

    @Autowired
    private RouteTemplates routeTemplates;

    public BaseRoute(String name) {
        this.name = name;
    }

    /**
     * Add the message flow from a source component to one or more destination
     * components.
     * 
     * @param sourceComponent
     * @param destinationComponents
     */
    public void addFlow(SourceComponent sourceComponent, DestinationComponent... destinationComponents) {

        for (DestinationComponent destination : destinationComponents) {
            destination.addSourceComponent(sourceComponent);
        }
    }

    /**
     * Associates a component with this route.
     * 
     * @param component
     * @throws Exception
     */
    public void addComponentToRoute(Component component) throws Exception {
        component.setRoute(name);
        component.config();
        components.add(component);
    }

    public abstract void configure() throws Exception;

    public void start() throws Exception {
        camelContext.addRoutes(routeTemplates);

        for (Component component : components) {
            camelContext.addRoutes(((RouteBuilder) component));
        }

        camelContext.start();
    }
}
