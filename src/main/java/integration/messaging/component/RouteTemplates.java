package integration.messaging.component;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import integration.messaging.MessageProcessor;
import integration.messaging.component.processingstep.filter.MessageAcceptancePolicy;
import integration.messaging.component.processingstep.filter.MessageForwardingPolicy;

/**
 * Common messaging Apache Camel route templates.
 * 
 * @author Brendan Douglas
 */
@Component("routeTemplates")
public class RouteTemplates extends RouteBuilder {
    @Autowired
    protected MessageProcessor messageProcessor;

    @Override
    public void configure() throws Exception {
        // Common route templates.  Routes are created from templates.


        // A route which is called after an inbound communications points outbound processor has been completed.  The result of this route is either a completion event is created
        // or the message is filtered.  An inbound communication point does no message processing other than determining if the message should be filtered or not.
        routeTemplate("inboundCommunicationPointOutboundProcessorTemplate")
            .templateParameter("isOutboundRunning")
            .templateParameter("componentPath")
            .templateParameter("contentType")
            .from("direct:outboundProcessor-{{componentPath}}")
            .routeId("outboundProcessor-{{componentPath}}")
            .routeGroup("{{componentPath}}")
            .autoStartup("{{isOutboundRunning}}")
            .routeGroup("{{componentPath}}")
            .setHeader("contentType", constant("{{contentType}}"))
            .bean(messageProcessor, "storeOutboundMessageFlowStep(*,{{componentRouteId}})")

            // Filter the outbound message if required.
            .bean("{{messageForwardingPolicy}}", "applyPolicy")

            .choice()
                .when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
                    .to("direct:filterMessage-{{componentPath}}")
                .otherwise()
                    .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)")
                .end();

        
        // A route called within a transactional outbox process to add a message flow step event id to a virtual topic.  This routes adds the id to the topic and deletes the source event 
        // within a single transaction.  A topic is used here as the message can be consumed by multiple other components.  This route is the final route in any component which
        // produces messages for other components to consume.
        routeTemplate("addToOutboundProcessingCompleteTopicTemplate")
            .templateParameter("componentPath")
            .from("direct:addToOutboundProcessingCompleteTopic-{{componentPath}}")
            .routeId("addToOutboundProcessingCompleteTopic-{{componentPath}}")
            .routeGroup("{{componentPath}}")
            .transacted()
                .bean(messageProcessor, "deleteMessageFlowEvent(*)")
                .process(new Processor() {

                    @Override
                    public void process(Exchange exchange) throws Exception {
                        long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
                        exchange.getMessage().setBody(workFlowStepId);
                    }
                })
            .to("jms:topic:VirtualTopic.{{componentPath}}");
    
        
        
        // A route to read a message from a topic, store the message flow, and then either accept the message which allows further processing or filter the message.
        // This is the entry point for processing steps and outbound communication points.
        routeTemplate("componentInboundTopicConsumerTemplate")
            .templateParameter("isInboundRunning")
                .templateParameter("componentPath")
                .templateParameter("componentRouteId")
                .templateParameter("contentType")
                .from("jms:VirtualTopic.{{sourceComponentPath}}::Consumer.{{componentPath}}.VirtualTopic.{{sourceComponentPath}}?acknowledgementModeName=CLIENT_ACKNOWLEDGE&concurrentConsumers=5")
                .routeId("messageReceiver-{{componentPath}}-{{sourceComponentPath}}")
                .routeGroup("{{componentPath}}")
                .setHeader("contentType", constant("{{contentType}}"))

                .autoStartup("{{isInboundRunning}}")
                .transacted()
                    .transform()
                    .method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
                    .bean(messageProcessor, "storeInboundMessageFlowStep(*,{{componentRouteId}})")

                    // Inbound message filter.
                    .bean("{{messageAcceptancePolicy}}", "applyPolicy")
                    
                    .choice()
                        .when(header(MessageAcceptancePolicy.ACCEPT_MESSAGE).isEqualTo(false))
                            .to("direct:filterMessage-{{componentPath}}")
                        .otherwise()
                            .bean(messageProcessor, "recordInboundProcessingCompleteEvent(*)")
                        .end();
        
        

        // Outbound processing for a route connector.  A route connector is used to join routes together.  A route connector does no processing on a message.
        routeTemplate("routeConnectorOutboundProcessorTemplate")
            .templateParameter("isOutboundRunning")
            .templateParameter("componentPath")
            .templateParameter("componentRouteId")
            .templateParameter("contentType")
            .from("direct:outboundProcessor-{{componentPath}}")
            .routeId("outboundProcessor-{{componentPath}}")
            .routeGroup("{{componentPath}}")
            .setHeader("contentType", constant("{{contentType}}"))
            .autoStartup("{{isOutboundRunning}}")
            .bean(messageProcessor, "storeOutboundMessageFlowStep(*,{{componentRouteId}})")
            .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)");
        
        
        
        // Outbound processing for a processing step.  A processing step is anything other than a commumnication point and can include transformers, filters, splitters.
        // The message is received from the outbound processing entry route and is then passed to a route to do the actual processing which will vary depending on the component.
        routeTemplate("processingStepOutboundProcessorTemplate")
            .templateParameter("isOutboundRunning")
            .templateParameter("componentPath")
            .from("direct:outboundProcessor-{{componentPath}}")
            .routeId("outboundProcessor-{{componentPath}}")
            .routeGroup("{{componentPath}}")
            .autoStartup("{{isOutboundRunning}}")
            .to("direct:process-{{componentPath}}");
    }
}