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
 * Outbound route connector.  Sends messages to other routes.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseRouteOutboundConnector extends BaseRouteConnector implements DestinationComponent {
	private List<String>sourceComponentPaths = new ArrayList<>();

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
		 
		 
		 TemplatedRouteBuilder.builder(camelContext, "routeConnectorPointOutboundProcessorTemplate")
	     	.parameter("isOutboundRunning", isOutboundRunning)
	     	.parameter("componentPath", identifier.getComponentPath())
	     	.parameter("componentRouteId", identifier.getComponentRouteId())
	     	.add();
		 
		 
			// Process outbound processing complete events.
			from("direct:handleOutboundProcessCompleteEvent-" + identifier.getComponentPath())
				.routeGroup(identifier.getComponentPath())
				.transacted("")
					.bean(messageProcessor, "deleteMessageFlowEvent(*)")
					.process(new Processor() {
				
						@Override
						public void process(Exchange exchange) throws Exception {
							long workFlowStepId = (long)exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
							exchange.getMessage().setBody(workFlowStepId);
						}
					})
					.to("jms:topic:VirtualTopic." + getName());
	}
	
	
}
