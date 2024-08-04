package integration.messaging.component.processingstep;

import java.util.ArrayList;
import java.util.List;

import integration.messaging.component.BaseMessagingComponent;
import integration.messaging.component.DestinationComponent;
import integration.messaging.component.SourceComponent;

/**
 * Base class for all processing steps.  A processing step is a component which is not an inbound or outbound communication point.
 * 
 * A processing step allows both the incoming and outgoing messages to be filtered.  By default all messages are allowed to be processed.
 * 
 * 
 * @author Brendan Douglas
 *
 */
public abstract class ProcessingStep extends BaseMessagingComponent implements SourceComponent, DestinationComponent {
	protected List<String>sourceComponentPaths = new ArrayList<>();
	
	
	public ProcessingStep(String componentName) {
		super(componentName);
	}
	
	@Override
	public void addSourceComponent(SourceComponent sourceComponent) {
		this.sourceComponentPaths.add(sourceComponent.getIdentifier().getComponentPath());
	}
}
