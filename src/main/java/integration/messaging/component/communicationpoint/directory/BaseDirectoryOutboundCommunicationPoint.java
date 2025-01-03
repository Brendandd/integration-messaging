package integration.messaging.component.communicationpoint.directory;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.communicationpoint.BaseOutboundCommunicationPoint;

/**
 * Base class for all directory output communication points.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseDirectoryOutboundCommunicationPoint extends BaseOutboundCommunicationPoint {

    public BaseDirectoryOutboundCommunicationPoint(String componentName) {
        super(componentName);
    }

    public String getDestinationFolder() {
        return componentProperties.get("TARGET_FOLDER");
    }

    @Override
    public String getToUriString() {
        return "file:" + getDestinationFolder();
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        // Read the message flow step id from the inbound processing complete queue and then forwards the message to the ouboundProcessor route which records an event indicating
        // processing has been complete and the message is ready for sending.  The message has not been sent to the destination at this point.
        TemplatedRouteBuilder.builder(camelContext, "readFromInboundProcessingCompleteQueueTemplate")
            .parameter("isOutboundRunning", isOutboundRunning)
            .parameter("componentPath", identifier.getComponentPath())
            .parameter("componentRouteId", identifier.getComponentRouteId())
            .parameter("contentType", getContentType())
            .add();

        
        
        // Creates one or more routes based on this components source components.  Each route reads from a topic.  This is the entry point for a directory/file outbound
        // communication point.
        for (String sourceComponent : sourceComponentPaths) {
            TemplatedRouteBuilder.builder(camelContext, "componentInboundTopicConsumerTemplate")
                .parameter("isInboundRunning", isInboundRunning)
                .parameter("componentPath", identifier.getComponentPath())
                .parameter("sourceComponentPath", sourceComponent)
                .parameter("componentRouteId", identifier.getComponentRouteId())
                .parameter("contentType", getContentType())
                .bean("messageAcceptancePolicy", getMessageAcceptancePolicy())
                .add();
        }

        
        // A route to add the message flow step id to the inbound processing complete queue so it can be picked up by the outbound processor.
        TemplatedRouteBuilder.builder(camelContext, "addToInboundProcessingCompleteQueueTemplate")
            .parameter("isOutboundRunning", isOutboundRunning)
            .parameter("componentPath", identifier.getComponentPath())
            .add();

        
        
        // A route to write an event record indicating the message is ready for sending to the destination.
        from("direct:outboundProcessor-" + identifier.getComponentPath())
            .routeId("outboundProcessor-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath())
            .autoStartup(isOutboundRunning)
            .setHeader("contentType", simple(getContentType()))
            .bean(messageProcessor, "recordMessageReadyForSendingEvent(*)");

        
        
        // Sends the message to the final destination which is the configured directory.  This is called from a transaction outbox process so we are guaranteed the message will be stored. 
        from("direct:sendMessageToDestination-" + identifier.getComponentPath())
            .routeId("sendMessageToDestination-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath())
            .transacted()
                .bean(messageProcessor, "deleteMessageFlowEvent(*)")
                .transform().method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
                .bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
                .to(getToUriString());
    }
}
