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

		// Reads a message from the inbound processing complete queue.
		routeTemplate("readMessageFromInboundProcessingCompleteQueueTemplate").templateParameter("isOutboundRunning")
		        .templateParameter("componentPath").templateParameter("componentRouteId")
		        .from("jms:queue:inboundProcessingComplete-{{componentPath}}?acknowledgementModeName=CLIENT_ACKNOWLEDGE&concurrentConsumers=5")
		        .autoStartup("{{isOutboundRunning}}").routeGroup("{{componentPath}}").transacted().transform()
		        .method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
		        .to("direct:outboundProcessor-{{componentPath}}");

		// The outbound processing for a inbound communication point.
		routeTemplate("inboundCommunicationPointOutboundProcessorTemplate").templateParameter("isOutboundRunning")
		        .templateParameter("componentPath").from("direct:outboundProcessor-{{componentPath}}")
		        .routeId("outboundProcessor-{{componentPath}}").routeGroup("{{componentPath}}")
		        .autoStartup("{{isOutboundRunning}}").routeGroup("{{componentPath}}")
		        .bean(messageProcessor, "storeOutboundMessageFlowStep(*,{{componentRouteId}})")

		        // Filter the outbound message if required.
		        .bean("{{messageForwardingPolicy}}", "applyPolicy")

		        .choice().when(header(MessageForwardingPolicy.FORWARD_MESSAGE).isEqualTo(false))
		        .to("direct:filterMessage-{{componentPath}}").otherwise()
		        .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)").end();

		// The outbound processing for a inbound communication point.
		routeTemplate("handleInboundProcessingCompleteEventTemplate").templateParameter("isOutboundRunning")
		        .templateParameter("componentPath").from("direct:handleInboundProcessingCompleteEvent-{{componentPath}}")
		        .routeId("handleInboundProcessingCompleteEvent-{{componentPath}}").routeGroup("{{componentPath}}").transacted()
		        .bean(messageProcessor, "deleteMessageFlowEvent(*)").process(new Processor() {

			        @Override
			        public void process(Exchange exchange) throws Exception {
				        long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
				        exchange.getMessage().setBody(workFlowStepId);
			        }

		        }).to("jms:queue:inboundProcessingComplete-{{componentPath}}");

		routeTemplate("componentInboundFilterableTopicConsumer").templateParameter("isInboundRunning")
		        .templateParameter("componentPath").templateParameter("componentRouteId")
		        .from("jms:VirtualTopic.{{sourceComponentPath}}::Consumer.{{componentPath}}.VirtualTopic.{{sourceComponentPath}}?acknowledgementModeName=CLIENT_ACKNOWLEDGE&concurrentConsumers=5")
		        .routeId("messageReceiver-{{componentPath}}-{{sourceComponentPath}}").routeGroup("{{componentPath}}")

		        .autoStartup("{{isInboundRunning}}").transacted("").transform()
		        .method(messageProcessor, "replaceMessageBodyIdWithMessageContent(*)")
		        .bean(messageProcessor, "storeInboundMessageFlowStep(*,{{componentRouteId}})")

		        // Inbound message filter.
		        .bean("{{messageAcceptancePolicy}}", "applyPolicy").choice()
		        .when(header(MessageAcceptancePolicy.ACCEPT_MESSAGE).isEqualTo(false))
		        .to("direct:filterMessage-{{componentPath}}").otherwise()
		        .bean(messageProcessor, "recordInboundProcessingCompleteEvent(*)").end();

		routeTemplate("outboundProcessingCompleteTopicConsumer").templateParameter("componentPath")
		        .from("direct:handleOutboundProcessCompleteEvent-{{componentPath}}")
		        .routeId("handleOutboundProcessCompleteEvent-{{componentPath}}").routeGroup("{{componentPath}}").transacted("")
		        .bean(messageProcessor, "deleteMessageFlowEvent(*)").process(new Processor() {

			        @Override
			        public void process(Exchange exchange) throws Exception {
				        long workFlowStepId = (long) exchange.getMessage().getHeader(MessageProcessor.MESSAGE_FLOW_STEP_ID);
				        exchange.getMessage().setBody(workFlowStepId);
			        }
		        }).to("jms:topic:VirtualTopic.{{componentPath}}");

		// The outbound processing for a inbound communication point.
		routeTemplate("routeConnectorPointOutboundProcessorTemplate").templateParameter("isOutboundRunning")
		        .templateParameter("componentPath").templateParameter("componentRouteId")
		        .from("direct:outboundProcessor-{{componentPath}}").routeId("outboundProcessor-{{componentPath}}")
		        .routeGroup("{{componentPath}}").autoStartup("{{isOutboundRunning}}")
		        .bean(messageProcessor, "storeOutboundMessageFlowStep(*,{{componentRouteId}})")
		        .bean(messageProcessor, "recordOutboundProcessingCompleteEvent(*)");

		routeTemplate("processingStepOutboundProcessorTemplate").templateParameter("isOutboundRunning")
		        .templateParameter("componentPath").from("direct:outboundProcessor-{{componentPath}}")
		        .routeId("outboundProcessor-{{componentPath}}").routeGroup("{{componentPath}}")
		        .autoStartup("{{isOutboundRunning}}")
		        // .setHeader("contentType", simple(getContentType()))
		        .to("direct:process-{{componentPath}}");
	}
}