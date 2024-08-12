package integration.messaging;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import integration.core.domain.configuration.DirectionEnum;
import integration.core.domain.messaging.MessageFlowTypeEvent;
import integration.core.dto.MessageFlowStepDto;
import integration.core.service.ConfigurationService;
import integration.messaging.component.processingstep.filter.MessageAcceptancePolicy;
import integration.messaging.service.MessagingFlowService;

/**
 * 
 */
@Component
public class MessageProcessor {
    public static final String FROM_MESSAGE_FLOW_STEP_ID = "fromMessageFlowStepId";
    public static final String MESSAGE_FLOW_STEP_ID = "messageFlowStepId";

    @Autowired
    protected Ignite ignite;

    @Autowired
    protected MessagingFlowService messagingFlowService;

    @Autowired
    protected ConfigurationService configurationService;

    /**
     * Stores a message flow.
     * 
     * @param exchange
     * @param routeId
     * @param componentId
     * @return
     * @throws Exception
     */
    private MessageFlowStepDto storeMessageFlow(Exchange exchange, long componentRouteId, DirectionEnum direction)
            throws Exception {
        // The from message flow step id can be null if it is the original inbound
        // message.
        Long fromMessageFlowId = (Long) exchange.getMessage().getHeader(FROM_MESSAGE_FLOW_STEP_ID);

        // Get the headers as these need to be stored.
        Map<String, Object> headers = exchange.getIn().getHeaders();

        String contentType = (String) exchange.getMessage().getHeader("contentType");
        String messageContent = exchange.getMessage().getMandatoryBody(String.class);

        return messagingFlowService.recordMessageFlow(componentRouteId, messageContent, headers, fromMessageFlowId, contentType,
                direction);
    }

    /**
     * Stores the inbound message.
     * 
     * @param exchange
     * @param routeId
     * @param componentId
     * @return
     * @throws Exception
     */
    public String storeInboundMessageFlowStep(Exchange exchange, long componentRouteId) throws Exception {
        MessageFlowStepDto messageFlowDto = storeMessageFlow(exchange, componentRouteId, DirectionEnum.INBOUND);
        exchange.getMessage().setHeader(MESSAGE_FLOW_STEP_ID, messageFlowDto.getId());

        return messageFlowDto.getMessageContent();
    }

    /**
     * Stores the outbound message.
     * 
     * @param exchange
     * @param routeId
     * @param componentId
     * @return
     * @throws Exception
     */
    public String storeOutboundMessageFlowStep(Exchange exchange, long componentRouteId) throws Exception {

        MessageFlowStepDto messageFlowDto = storeMessageFlow(exchange, componentRouteId, DirectionEnum.OUTBOUND);
        exchange.getMessage().setHeader(MESSAGE_FLOW_STEP_ID, messageFlowDto.getId());

        return messageFlowDto.getMessageContent();
    }

    /**
     * Stores the ACK.
     * 
     * @param exchange
     * @param routeId
     * @param componentId
     * @throws Exception
     */
    public String recordAck(Exchange exchange, long componentRouteId) throws Exception {
        String ackContent = exchange.getMessage().getMandatoryBody(String.class);

        // Store the ACK. An ACK is created from the inbound message.
        Long messageFlowId = (Long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        MessageFlowStepDto messageFlowDto = messagingFlowService.recordAck(componentRouteId, null, ackContent, messageFlowId);

        return messageFlowDto.getMessageContent();
    }

    /**
     * Filters the message.
     * 
     * @param exchange
     * @throws Exception
     */
    public void filterMessage(Exchange exchange) throws Exception {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        String filterReason = (String) exchange.getMessage().getHeader(MessageAcceptancePolicy.REASON);
        String filterName = (String) exchange.getMessage().getHeader(MessageAcceptancePolicy.FILTER_NAME);
        messagingFlowService.filterMessage(messageFlowId, filterReason, filterName);
    }

    public void recordMessageFlowStepError(Exchange exchange) throws Exception {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        messagingFlowService.recordMessageFlowStepError(messageFlowId, "Help!!!!");
    }

    /**
     * Returns the message content for a message flow step id.
     * 
     * @param exchange
     * @return
     * @throws Exception
     */
    public String getMessageContent(Exchange exchange) throws Exception {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        return messagingFlowService.retrieveMessageFlow(messageFlowId).getMessageContent();
    }

    /**
     * 
     * 
     * @param exchange
     * @return
     * @throws Exception
     */
    public String replaceMessageBodyIdWithMessageContent(Exchange exchange) throws Exception {
        // The message flow id will be the body.
        long messageFlowId = (long) exchange.getMessage().getBody();

        // Store the message flow id as a header
        exchange.getMessage().setHeader(FROM_MESSAGE_FLOW_STEP_ID, messageFlowId);

        // Now replace the body with the message content.
        return messagingFlowService.retrieveMessageFlow(messageFlowId).getMessageContent();
    }

    /**
     * records an external message flow event. An external event will send a message
     * to an external system.
     * 
     * @param exchange
     */
    public void recordMessageReadyForSendingEvent(Exchange exchange) {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        messagingFlowService.recordMessageFlowEvent(messageFlowId, MessageFlowTypeEvent.MESSAGE_READY_FOR_SENDING);
    }

    public void recordInboundProcessingCompleteEvent(Exchange exchange) {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        messagingFlowService.recordMessageFlowEvent(messageFlowId, MessageFlowTypeEvent.COMPONENT_INBOUND_PROCESSING_COMPLETE);
    }

    public void recordOutboundProcessingCompleteEvent(Exchange exchange) {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        messagingFlowService.recordMessageFlowEvent(messageFlowId, MessageFlowTypeEvent.COMPONENT_OUTBOUND_PROCESSING_COMPLETE);
    }

    public void recordOutboundRouteConnectorCompleteEvent(Exchange exchange) {
        long messageFlowId = (long) exchange.getMessage().getHeader(MESSAGE_FLOW_STEP_ID);
        messagingFlowService.recordMessageFlowEvent(messageFlowId, MessageFlowTypeEvent.ROUTE_OUTBOUND_CONNECTOR_COMPLETE);
    }

    public void deleteMessageFlowEvent(Exchange exchange) {
        long eventId = exchange.getMessage().getBody(Long.class);
        messagingFlowService.deleteEvent(eventId);
    }

    public void updateCache(Exchange exchange, long eventId) {
        System.out.println("Update cache");

    }
}