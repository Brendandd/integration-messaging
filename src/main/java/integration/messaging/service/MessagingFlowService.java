package integration.messaging.service;

import java.util.List;
import java.util.Map;

import integration.core.domain.configuration.DirectionEnum;
import integration.core.domain.messaging.MessageFlowTypeEvent;
import integration.core.dto.MessageFlowEventDto;
import integration.core.dto.MessageFlowStepDto;

/**
 * Service to store messages/message flows.
 */
public interface MessagingFlowService {

    /**
     * Records a message/message flow. This is not the original incoming message so
     * needs to be linked to the parent.
     * 
     * @param routeId
     * @param componentId
     * @param messageContent
     * @param headers
     * @param parentMessageFlowId
     * @return
     */
    MessageFlowStepDto recordMessageFlow(long componentRouteId, String messageContent, Map<String, Object> headers,
            Long fromMessageFlowStepId, String contentType, DirectionEnum direction);

    /**
     * Filters a message.
     * 
     * @param routeId
     * @param componentId
     * @param parentMessageFlowId
     * @return
     */
    void filterMessage(long messageFlowId, String reason, String filterName);

    void recordMessageFlowStepError(long messageFlowId, String reason);

    /**
     * Records an event.
     * 
     * @param messageFlow
     */
    void recordMessageFlowEvent(long messageFlowId, MessageFlowTypeEvent eventType);

    /**
     * Records an ACK.
     * 
     * @param routeId
     * @param componentId
     * @param content
     */
    MessageFlowStepDto recordAck(long componentRouteId, Map<String, Object> headers, String content, Long fromMessageFlowStepId);

    /**
     * Retrieves a message flow.
     * 
     * @param messageFlowId
     * @return
     */
    MessageFlowStepDto retrieveMessageFlow(long messageFlowId);

    List<MessageFlowEventDto> getEvents(long componentRouteId, int numberToRead, MessageFlowTypeEvent type);

    void deleteEvent(long eventId);
}
