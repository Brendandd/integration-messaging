package integration.messaging.component.processingstep;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.BaseMessagingComponent;
import integration.messaging.component.DestinationComponent;
import integration.messaging.component.SourceComponent;

/**
 * Base class for all processing steps. A processing step is a component which
 * is not an inbound or outbound communication point.
 * 
 * A processing step allows both the incoming and outgoing messages to be
 * filtered. By default all messages are allowed to be processed.
 * 
 * 
 * @author Brendan Douglas
 *
 */
public abstract class ProcessingStep extends BaseMessagingComponent implements SourceComponent, DestinationComponent {
    protected List<String> sourceComponentPaths = new ArrayList<>();

    public ProcessingStep(String componentName) {
        super(componentName);
    }

    @Override
    public void addSourceComponent(SourceComponent sourceComponent) {
        this.sourceComponentPaths.add(sourceComponent.getIdentifier().getComponentPath());
    }


    @Override
    public void configure() throws Exception {
        super.configure();
               
        // Creates one or more routes based on this components source components.  Each route reads from a topic.  This is the entry point for processing steps.
        for (String sourceComponent : sourceComponentPaths) {
            TemplatedRouteBuilder.builder(camelContext, "componentInboundTopicConsumerTemplate")
                .parameter("isInboundRunning", isInboundRunning).parameter("componentPath", identifier.getComponentPath())
                .parameter("sourceComponentPath", sourceComponent)
                .parameter("componentRouteId", identifier.getComponentRouteId())
                .parameter("contentType", constant(getContentType()))
                .bean("messageAcceptancePolicy", getMessageAcceptancePolicy())
                .add();
        }

        
        // Route to do the actual processing step processing.
        TemplatedRouteBuilder.builder(camelContext, "processingStepOutboundProcessorTemplate")
            .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
            .add();

        
        // Add the message flow step id to the outbound processing complete topic so it can be picked up by one or more other components.  This is the final
        // step in processing steps.
        TemplatedRouteBuilder.builder(camelContext, "addToOutboundProcessingCompleteTopicTemplate")
            .parameter("componentPath", identifier.getComponentPath())
            .add();
    }
}
