package integration.messaging.component.processingstep.transformation;

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


        from("direct:process-" + identifier.getComponentPath()).routeId("process-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath()).bean(getTransformer(), "transform")
            .setHeader("contentType", constant(getContentType()))
            .bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")

            // Outbound message filter.
            .bean(getMessageForwardingPolicy(), "applyPolicy")

            .choice()
                .when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
                    .to("direct:filterMessage-" + identifier.getComponentPath())
                .otherwise()
                    .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)")
                .end();
    }
}