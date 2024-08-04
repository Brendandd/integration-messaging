package integration.messaging.component.processingstep.filter;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.processingstep.ProcessingStep;

/**
 * Base class for all filter processing steps.
 */
public abstract class BaseFilterProcessingStep extends ProcessingStep {
	
	public BaseFilterProcessingStep(String componentName) {
		super(componentName);
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

		 from("direct:process-" + identifier.getComponentPath())
			.routeId("process-" + identifier.getComponentPath())
				.routeGroup(identifier.getComponentPath())
				.bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
				
				// Outbound message filter.
				.bean(getMessageForwardingPolicy(), "applyPolicy")
				
				.choice()
					.when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
						.to("direct:filterMessage-" + identifier.getComponentPath())
					.otherwise()
						.bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)")
				.end();

		 
		 TemplatedRouteBuilder.builder(camelContext, "processingStepOutboundProcessorTemplate")
		 	.parameter("isOutboundRunning", isOutboundRunning)
		 	.parameter("componentPath", identifier.getComponentPath())
		 	.add();
		 
		 
		 TemplatedRouteBuilder.builder(camelContext, "outboundProcessingCompleteTopicConsumer")
		 	.parameter("componentPath", identifier.getComponentPath())
		 	.add();
	}
	
	
	
}
