package integration.messaging.component.processingstep.filter;

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


        from("direct:process-" + identifier.getComponentPath()).routeId("process-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath())
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
