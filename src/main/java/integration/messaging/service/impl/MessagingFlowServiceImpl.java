package integration.messaging.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import integration.core.domain.configuration.ComponentRoute;
import integration.core.domain.configuration.DirectionEnum;
import integration.core.domain.messaging.Message;
import integration.core.domain.messaging.MessageFlowEvent;
import integration.core.domain.messaging.MessageFlowGroup;
import integration.core.domain.messaging.MessageFlowStep;
import integration.core.domain.messaging.MessageFlowTypeEvent;
import integration.core.dto.MessageFlowEventDto;
import integration.core.dto.MessageFlowStepDto;
import integration.core.dto.mapper.MessageFlowEventMapper;
import integration.core.dto.mapper.MessageFlowStepMapper;
import integration.core.exception.ConfigurationException;
import integration.core.repository.ComponentRouteRepository;
import integration.core.repository.MessageFlowEventRepository;
import integration.core.repository.MessageFlowRepository;
import integration.core.repository.MessageFlowStepRepository;
import integration.core.repository.MessageRepository;
import integration.core.util.Utils;
import integration.messaging.service.MessagingFlowService;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class MessagingFlowServiceImpl implements MessagingFlowService {

    @Autowired
    private MessageFlowStepRepository messageFlowStepRepository;

    @Autowired
    private MessageFlowRepository messageFlowRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ComponentRouteRepository componentRouteRepository;

    @Autowired
    private MessageFlowEventRepository eventRepository;

    /**
     * Records the ACK.
     */
    @Override
    public MessageFlowStepDto recordAck(long componentRouteId, Map<String, Object> headers, String messageContent,
            Long fromMessageFlowStepId) {
        Optional<MessageFlowStep> fromMessageFlow = null;

        if (fromMessageFlowStepId != null) {
            fromMessageFlow = messageFlowStepRepository.findById(fromMessageFlowStepId);
        }

        // Create the message and associated message flow.
        Message message = createMessage(messageContent, headers, "HL7 ACK");

        MessageFlowStep messageFlow = createMessageFlowStep(componentRouteId, message, fromMessageFlow, DirectionEnum.OUTBOUND,
                fromMessageFlow.get().getMessageFlowGroup());

        MessageFlowStepMapper mapper = new MessageFlowStepMapper();
        return mapper.doMapping(messageFlow);
    }

    /**
     * Stores a new message/message flow.
     */
    @Override
    public MessageFlowStepDto recordMessageFlow(long componentRouteId, String messageContent, Map<String, Object> headers,
            Long fromMessageFlowStepId, String contentType, DirectionEnum direction) {
        Optional<MessageFlowStep> fromMessageFlow = null;
        String fromMessageContent = null;

        MessageFlowGroup messageFlow = null;

        if (fromMessageFlowStepId != null) {
            // Not the original incoming message if there is a from message flow id.
            fromMessageFlow = messageFlowStepRepository.findById(fromMessageFlowStepId);

            // If a from message flow id is provided it must exist.
            if (fromMessageFlow.isEmpty()) {
                throw new ConfigurationException("from message flow not found. Id: " + fromMessageFlowStepId);
            }

            if (headers != null) {
                Map<String, Object> fromMessageHeaders = Utils.convertFromJSON(fromMessageFlow.get().getMessage().getHeaders());
                headers.putAll(fromMessageHeaders);
            }

            fromMessageContent = fromMessageFlow.get().getMessage().getContent();
            messageFlow = fromMessageFlow.get().getMessageFlowGroup();
        } else {
            messageFlow = new MessageFlowGroup();
        }

        // Create the message and associated message flow.
        Message message = null;

        if (fromMessageContent != null && fromMessageContent.equals(messageContent)) {
            message = fromMessageFlow.get().getMessage();
        } else {
            message = createMessage(messageContent, headers, contentType);
        }

        messageFlowRepository.save(messageFlow);

        MessageFlowStep messageFlowStep = createMessageFlowStep(componentRouteId, message, fromMessageFlow, direction,
                messageFlow);

        messageFlowStep = messageFlowStepRepository.save(messageFlowStep);

        MessageFlowStepMapper mapper = new MessageFlowStepMapper();
        return mapper.doMapping(messageFlowStep);
    }

    /**
     *
     */
    @Override
    public void recordMessageFlowEvent(long messageFlowId, MessageFlowTypeEvent eventType) {
        MessageFlowStep messageFlow = findMessageFlowById(messageFlowId);

        MessageFlowEvent event = new MessageFlowEvent();
        event.setMessageFlow(messageFlow);
        event.setType(eventType);
        eventRepository.save(event);
    }

    /**
     * Creates and returns a new message.
     * 
     * @param content
     * @param headers
     * @return
     */
    private Message createMessage(String content, Map<String, Object> headers, String contentType) {
        Message message = new Message();
        message.setContent(content);
        message.setContentType(contentType);
        message.setHeaders(Utils.convertToJSON(headers));
        messageRepository.save(message);

        return message;
    }

    /**
     * Creates an returns a message flow object with an optional from message flow.
     * 
     * @param route
     * @param component
     * @param message
     * @param from
     * @return
     */
    private MessageFlowStep createMessageFlowStep(long componentRouteId, Message message, Optional<MessageFlowStep> from,
            DirectionEnum direction, MessageFlowGroup messageFlow) {
        Optional<ComponentRoute> componentRouteOptional = componentRouteRepository.findById(componentRouteId);

        MessageFlowStep messageFlowStep = new MessageFlowStep();
        messageFlowStep.setComponentRoute(componentRouteOptional.get());
        messageFlowStep.setMessage(message);
        messageFlowStep.setDirection(direction);

        if (from != null) {
            messageFlowStep.setFromMessageFlowStep(from.get());
        }

        messageFlow.addMessageFlowStep(messageFlowStep);

        return messageFlowStepRepository.save(messageFlowStep);
    }

    /**
     * Retrieves a message flowDto by id.
     */
    @Override
    public MessageFlowStepDto retrieveMessageFlow(long messageFlowId) {
        Optional<MessageFlowStep> messageFlow = messageFlowStepRepository.findById(messageFlowId);

        // The message flow must exist.
        if (messageFlow.isEmpty()) {
            throw new ConfigurationException("Message flow not found. Id: " + messageFlowId);
        }

        MessageFlowStepMapper mapper = new MessageFlowStepMapper();

        return mapper.doMapping(messageFlow.get());
    }

    @Override
    public void filterMessage(long messageFlowId, String reason, String filterName) {
        MessageFlowStep messageFlow = findMessageFlowById(messageFlowId);
        messageFlow.setFiltered(true);

        messageFlow.filterMessage(reason, filterName);

        messageFlowStepRepository.save(messageFlow);
    }

    @Override
    public void recordMessageFlowStepError(long messageFlowId, String reason) {
        MessageFlowStep messageFlow = findMessageFlowById(messageFlowId);
        messageFlow.setError(true);

        messageFlow.errorMessage(reason);

        messageFlowStepRepository.save(messageFlow);

    }

    private MessageFlowStep findMessageFlowById(long messageFlowId) {
        Optional<MessageFlowStep> messageFlow = messageFlowStepRepository.findById(messageFlowId);

        // The message flow must exist.
        if (messageFlow.isEmpty()) {
            throw new ConfigurationException("Message flow not found. Id: " + messageFlowId);
        }

        return messageFlow.get();
    }

    @Override
    public List<MessageFlowEventDto> getEvents(long componentRouteId, int numberToRead, MessageFlowTypeEvent type) {
        MessageFlowEventMapper mapper = new MessageFlowEventMapper();

        List<MessageFlowEventDto> eventDtos = new ArrayList<>();

        List<MessageFlowEvent> events = eventRepository.getEvents(componentRouteId, type, numberToRead);

        for (MessageFlowEvent event : events) {
            eventDtos.add(mapper.doMapping(event));
        }

        return eventDtos;
    }

    @Override
    public void deleteEvent(long eventId) {
        eventRepository.deleteById(eventId);
    }

}
