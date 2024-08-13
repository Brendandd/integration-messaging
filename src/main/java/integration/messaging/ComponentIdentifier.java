package integration.messaging;

/**
 * The identifier of a component.
 * 
 * @author Brendan Douglas
 */
public class ComponentIdentifier {
    private long componentRouteId;
    private long routeId;
    private long componentId;
    private String componentName;
    private String routeName;

    public ComponentIdentifier(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getComponentPath() {
        return routeName + "-" + componentName;
    }

    public long getComponentRouteId() {
        return componentRouteId;
    }

    public void setComponentRouteId(long componentRouteId) {
        this.componentRouteId = componentRouteId;
    }

    public long getRouteId() {
        return routeId;
    }

    public long getComponentId() {
        return componentId;
    }

    public void setRouteId(long routeId) {
        this.routeId = routeId;
    }

    public void setComponentId(long componentId) {
        this.componentId = componentId;
    }
}
