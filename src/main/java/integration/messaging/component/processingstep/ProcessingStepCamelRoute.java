package integration.messaging.component.processingstep;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import integration.messaging.BaseMessagingCamelRoute;
import integration.messaging.MessageProcessor;
import integration.messaging.component.processingstep.filter.MessageAcceptancePolicy;
import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * Base class for all Apache Camel messaging component routes.
 * 
 * @author Brendan Douglas
 *
 */
public class ProcessingStepCamelRoute extends BaseMessagingCamelRoute {
    protected List<String> sourceComponentPaths;

    protected MessageForwardingPolicy messageForwardingPolicy;

    protected MessageAcceptancePolicy messageAcceptancePolicy;

    public void setSourceComponentPaths(List<String> sourceComponentPaths) {
        this.sourceComponentPaths = sourceComponentPaths;
    }

    public void setMessageForwardingPolicy(MessageForwardingPolicy messageForwardingPolicy) {
        this.messageForwardingPolicy = messageForwardingPolicy;
    }

    public void setMessageAcceptancePolicy(MessageAcceptancePolicy messageAcceptancePolicy) {
        this.messageAcceptancePolicy = messageAcceptancePolicy;
    }

    @Override
    public void configure() throws Exception {

        // A components outbound message processor.
        from("direct:outboundProcessor-" + identifier.getComponentPath())
                .routeId("outboundProcessor-" + identifier.getComponentPath()).routeGroup(identifier.getComponentPath())
                .autoStartup(isOutboundRunning)
                // .setHeader("contentType", simple(getContentType()))
                .to("direct:process-" + identifier.getComponentPath());

        // Process outbound processing complete events.
        from("direct:addToOutboundProcessingCompleteTopic-" + identifier.getComponentPath())
                .routeGroup(identifier.getComponentPath()).transacted("").bean(messageProcessor, "deleteMessageFlowEvent(*)")
                .process(new Processor() {

                    @Override
                    public void process(Exchange exchange) throws Exception {
                        long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
                        exchange.getMessage().setBody(workFlowStepId);
                    }
                }).to("jms:topic:VirtualTopic." + identifier.getComponentPath());
    }
}