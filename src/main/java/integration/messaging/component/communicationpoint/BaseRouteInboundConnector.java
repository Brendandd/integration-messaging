package integration.messaging.component.communicationpoint;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import integration.messaging.MessageProcessor;
import integration.messaging.component.SourceComponent;
import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * Inbound route connector. Accepts messages from other routes.
 * 
 * @author Brendan Douglas
 */
public abstract class BaseRouteInboundConnector extends BaseRouteConnector implements SourceComponent {

    public BaseRouteInboundConnector(String componentName) {
        super(componentName);
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        // Inbound message flow into this component.  The message is read from a single topic.  In the future I might allow multiple topics.  This is the entry point for an
        // inbound route connector.
        from("jms:VirtualTopic." + getName() + "::Consumer." + identifier.getComponentPath() + ".VirtualTopic." + getName() + "?acknowledgementModeName=CLIENT_ACKNOWLEDGE&concurrentConsumers=5")
            .routeId("messageReceiver-" + identifier.getComponentPath() + "-" + getName())
            .routeGroup(identifier.getComponentPath())

            .autoStartup(isInboundRunning)
            .transacted()
                .transform()
                .method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
                .bean(messageProcessor, "storeInboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
                .bean(messageProcessor, "recordInboundProcessingCompleteEvent(*)");

          
       
        // Process outbound processing complete events.
        from("direct:addToOutboundProcessingCompleteTopic-" + identifier.getComponentPath())
                .routeGroup(identifier.getComponentPath())
                .transacted()
                    .bean(messageProcessor, "deleteMessageFlowEvent(*)")
                    .process(new Processor() {
    
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
                            exchange.getMessage().setBody(workFlowStepId);
                        }
                    }).to("jms:topic:VirtualTopic." + identifier.getComponentPath());


        
        // Outbound message flow from an inbound route connector.
        from("direct:outboundProcessor-" + identifier.getComponentPath())
            .routeId("outboundProcessor-" + identifier.getComponentPath()).routeGroup(identifier.getComponentPath())
            .autoStartup(isOutboundRunning)
            .setHeader("contentType", constant(getContentType()))
            .bean(messageProcessor, "storeOutboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
            
            // Filter the outbound message if required.
            .bean(getMessageForwardingPolicy(), "applyPolicy")

            .choice()
                .when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
                    .to("direct:filterMessage-" + identifier.getComponentPath())
                .otherwise()
                    .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)")
                .end();

    }

}
