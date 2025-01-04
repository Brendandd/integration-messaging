package integration.messaging.component.communicationpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.MessageProcessor;
import integration.messaging.component.DestinationComponent;
import integration.messaging.component.SourceComponent;

/**
 * Outbound route connector. Sends messages to other routes.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseRouteOutboundConnector extends BaseRouteConnector implements DestinationComponent {
    private List<String> sourceComponentPaths = new ArrayList<>();

    public BaseRouteOutboundConnector(String componentName) throws Exception {
        super(componentName);
    }

    @Override
    public void addSourceComponent(SourceComponent sourceComponent) {
        this.sourceComponentPaths.add(sourceComponent.getIdentifier().getComponentPath());
    }

    @Override
    public void configure() throws Exception {
        super.configure();

       
        // Creates one or more routes based on this components source components.  Each route reads from a topic.  This is the entry point for outbound route connectors.
        for (String sourceComponent : sourceComponentPaths) {
            TemplatedRouteBuilder.builder(camelContext, "componentInboundTopicConsumerTemplate")
                .parameter("isInboundRunning", isInboundRunning).parameter("componentPath", identifier.getComponentPath())
                .parameter("sourceComponentPath", sourceComponent)
                .parameter("componentRouteId", identifier.getComponentRouteId())
                .parameter("contentType", getContentType())
                .bean("messageAcceptancePolicy", getMessageAcceptancePolicy())
                .add();
        }

               
        TemplatedRouteBuilder.builder(camelContext, "routeConnectorOutboundProcessorTemplate")
            .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
            .parameter("componentRouteId", identifier.getComponentRouteId())
            .parameter("contentType", getContentType())
            .add();

        
        
        // Process outbound processing complete events.
        from("direct:addToOutboundProcessingCompleteTopic-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath())
            .transacted()
                .bean(messageProcessor, "deleteMessageFlowEvent(*)")
                .process(new Processor() {
    
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
                        exchange.getMessage().setBody(workFlowStepId);
                    }
                })
                .to("jms:topic:VirtualTopic." + getName());
    }

}
