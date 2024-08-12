package integration.messaging.component.processingstep.splitter;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.processingstep.ProcessingStep;
import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * Base class for splitting a message into 1 or more messages. A splitter is
 * only responsible for splitting a message and no transformations should be
 * done as part of the splitting process.
 */
public abstract class BaseSplitterProcessingStep extends ProcessingStep {

	public BaseSplitterProcessingStep(String componentName) {
		super(componentName);
	}

	public abstract MessageSplitter getSplitter();

	@Override
	public void configure() throws Exception {
		super.configure();

		TemplatedRouteBuilder.builder(camelContext, "readMessageFromInboundProcessingCompleteQueueTemplate")
		        .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
		        .parameter("componentRouteId", identifier.getComponentRouteId()).add();

		for (String sourceComponent : sourceComponentPaths) {
			TemplatedRouteBuilder.builder(camelContext, "componentInboundFilterableTopicConsumer")
			        .parameter("isInboundRunning", isInboundRunning).parameter("componentPath", identifier.getComponentPath())
			        .parameter("sourceComponentPath", sourceComponent)
			        .parameter("componentRouteId", identifier.getComponentRouteId())
			        .bean("messageAcceptancePolicy", getMessageAcceptancePolicy()).add();
		}

		TemplatedRouteBuilder.builder(camelContext, "handleInboundProcessingCompleteEventTemplate")
		        .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
		        .add();

		TemplatedRouteBuilder.builder(camelContext, "processingStepOutboundProcessorTemplate")
		        .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
		        .add();

		TemplatedRouteBuilder.builder(camelContext, "outboundProcessingCompleteTopicConsumer")
		        .parameter("componentPath", identifier.getComponentPath()).add();

		from("direct:process-" + identifier.getComponentPath()).routeId("process-" + identifier.getComponentPath())
		        .routeGroup(identifier.getComponentPath()).split().method(getSplitter(), "split")
		        .bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")

		        // Outbound message filter.
		        .bean(getMessageForwardingPolicy(), "applyPolicy")

		        .choice().when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
		        .to("direct:filterMessage-" + identifier.getComponentPath()).otherwise()
		        .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)").end();
	}
}
