package integration.messaging.component.communicationpoint.directory;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.communicationpoint.BaseOutboundCommunicationPoint;

/**
 * Base class for all directory output communication points.
 * 
 * @author Brendan
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
		
		TemplatedRouteBuilder.builder(camelContext, "readMessageFromInboundProcessingCompleteQueueTemplate")
	     .parameter("isOutboundRunning", isOutboundRunning)
	     .parameter("componentPath", identifier.getComponentPath())
	     .parameter("componentRouteId", identifier.getComponentRouteId())
	     .add();

	 
	 for (String sourceComponent : sourceComponentPaths) {
		 TemplatedRouteBuilder.builder(camelContext, "componentInboundFilterableTopicConsumer")
	     	.parameter("isInboundRunning", isInboundRunning)
	     	.parameter("componentPath", identifier.getComponentPath())
	     	.parameter("sourceComponentPath", sourceComponent)
	     	.parameter("componentRouteId", identifier.getComponentRouteId())
	     	.bean("messageAcceptancePolicy", getMessageAcceptancePolicy())
	     	.add();
	 }
	 
	 
	 TemplatedRouteBuilder.builder(camelContext, "handleInboundProcessingCompleteEventTemplate")
	 	.parameter("isOutboundRunning", isOutboundRunning)
	 	.parameter("componentPath", identifier.getComponentPath())
	 	.add();
	 
	 
	 
		// Outbound message flow from an inbound communication point.
		from("direct:outboundProcessor-" + identifier.getComponentPath())
			.routeId("outboundProcessor-" + identifier.getComponentPath())
			.routeGroup(identifier.getComponentPath())
			.autoStartup(isOutboundRunning)
			.setHeader("contentType", simple(getContentType()))
			.bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")	
			.bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)");
		
		
		// Process outbound processing complete events.
		from("direct:handleOutboundProcessCompleteEvent-" + identifier.getComponentPath())
			.routeId("handleOutboundProcessCompleteEvent-" + identifier.getComponentPath())
			.routeGroup(identifier.getComponentPath())
			.transacted("")
				.bean(messageProcessor, "deleteMessageFlowEvent(*)")
				.transform().method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
				.bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
				.to(getToUriString());
		
	}
}
