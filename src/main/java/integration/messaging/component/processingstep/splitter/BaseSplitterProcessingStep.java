package integration.messaging.component.processingstep.splitter;

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

        from("direct:process-" + identifier.getComponentPath()).routeId("process-" + identifier.getComponentPath())
            .routeGroup(identifier.getComponentPath())
            .split().method(getSplitter(), "split")
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
