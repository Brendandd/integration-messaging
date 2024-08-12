package integration.messaging.component.processingstep.transformation;

import org.apache.camel.builder.TemplatedRouteBuilder;

import integration.messaging.component.processingstep.ProcessingStep;
import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * Base class for all transformation processing steps.
 */
public abstract class BaseTransformationProcessingStep extends ProcessingStep {

    public BaseTransformationProcessingStep(String componentName) {
        super(componentName);
    }

    /**
     * The transformer. A transformer is responsible for transforming the message. A
     * transformer can also filter a message is required.
     * 
     * @return
     */
    public abstract MessageTransformer getTransformer();

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
                .routeGroup(identifier.getComponentPath()).bean(getTransformer(), "transform")
                .bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")

                // Outbound message filter.
                .bean(getMessageForwardingPolicy(), "applyPolicy")

                .choice().when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
                .to("direct:filterMessage-" + identifier.getComponentPath()).otherwise()
                .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)").end();
    }
}