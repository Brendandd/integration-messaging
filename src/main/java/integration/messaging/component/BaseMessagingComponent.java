package integration.messaging.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import integration.core.domain.messaging.MessageFlowTypeEvent;
import integration.core.dto.ComponentDto;
import integration.core.dto.ComponentRouteDto;
import integration.core.dto.MessageFlowEventDto;
import integration.core.dto.RouteDto;
import integration.core.exception.ConfigurationException;
import integration.core.service.ConfigurationService;
import integration.messaging.ComponentIdentifier;
import integration.messaging.MessageProcessor;
import integration.messaging.service.MessagingFlowService;

/**
 * Base class for all Apache Camel messaging component routes.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseMessagingComponent extends RouteBuilder implements Component {
	
	@Autowired
	protected CamelContext camelContext;
		
	public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
	
	@Autowired
	protected Ignite ignite;
	
	@Autowired
	protected MessageProcessor messageProcessor;
	
	@Autowired
	protected ConfigurationService configurationService;
	
	protected ComponentIdentifier identifier;
	
	protected boolean isInboundRunning;
	protected boolean isOutboundRunning;
	
	protected Map<String, String>componentProperties;
		
	@Autowired
	protected MessagingFlowService messagingFlowService;
	
	@Autowired
	protected ProducerTemplate producerTemplate;
	
	
	public BaseMessagingComponent(String componentName) {	
		this.identifier = new ComponentIdentifier(componentName);
	}
	
	
	/**
	 * The content type handled by this component.
	 * 
	 * @return
	 */
	public abstract String getContentType();
	
	
	@Override
	public void config() throws Exception {		
		// Get the route
		RouteDto routeDto = configurationService.getRouteByName(getIdentifier().getRouteName());
	    
		if (routeDto == null) {
			throw new ConfigurationException("Route. Route name: " + getIdentifier().getRouteName());
		}
	
		// Get the component
		ComponentDto componentDto = configurationService.getComponentByName(getIdentifier().getComponentName());
		
		if (componentDto == null) {
			throw new ConfigurationException("Component. Component name: " + getIdentifier().getComponentName());
		}
		
		// Now get the component route object.  This will throw an exception if the component is not on this route.
		ComponentRouteDto componentRouteDto = configurationService.getComponentRoute(componentDto.getId(), routeDto.getId());
		
		getIdentifier().setComponentRouteId(componentRouteDto.getId());
		getIdentifier().setRouteId(routeDto.getId());
		getIdentifier().setComponentId(componentDto.getId());
		
		componentProperties = componentDto.getProperties();
		
		// Now we need to read the component state from the database to see if it should be started on startup.
		isInboundRunning = configurationService.isInboundRunning(componentRouteDto.getId());
		isOutboundRunning = configurationService.isOutboundRunning(componentRouteDto.getId());	
	}

	
	public List<String>getRoutes(Exchange exchange) {
		List<Route>allRoutes = exchange.getContext().getRoutes();
		
		List<String>routeIds = new ArrayList<String>();
		
		for (Route route : allRoutes) {
			if (route.getGroup() != null && route.getGroup().equals(identifier.getComponentPath())) {
				routeIds.add(route.getId());
			}
		}
		
		return routeIds;
	}

	
	/**
	 * Stops the inbound message flow into this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void stopInbound(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			if (route.startsWith("messageReceiver")) {
				exchange.getContext().getRouteController().stopRoute(route);
			}
		}
	}

	
	/**
	 * Stops the outbound message flow from this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void stopOutbound(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			if (route.startsWith("messageSender")) {
				exchange.getContext().getRouteController().stopRoute(route);
			}
		}	
	}

	
	/**
	 * Stops both the inbound and outbound message flow to/from this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void stopEntireComponent(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			exchange.getContext().getRouteController().stopRoute(route);
		}		
	}

	
	/**
	 * Starts the inbound message flow into this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void startInbound(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			if (route.startsWith("messageReceiver")) {
				exchange.getContext().getRouteController().startRoute(route);
			}
		}
	}

	
	/**
	 * Starts the outbound message flow from this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void startOutbound(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			if (route.startsWith("messageSender")) {
				exchange.getContext().getRouteController().startRoute(route);
			}
		}	
	}

	
	/**
	 * Starts both the inbound and outbound flows to/from this component.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void startEntireComponent(Exchange exchange) throws Exception {
		List<String>allRoutes = getRoutes(exchange);
		
		for (String route : allRoutes) {
			exchange.getContext().getRouteController().startRoute(route);
		}		
	}


	@Override
	public ComponentIdentifier getIdentifier() {
		return identifier;
	}

	
	public void setIdentifier(ComponentIdentifier identifier) {
		this.identifier = identifier;
	}

	
	/**
	 * A timer to process messages which have completed inbound prossing.
	 */
	@Scheduled(fixedRate = 100)
	public void processComponentInboundProcessingCompleteEvents() {		
		if (!camelContext.isStarted()) {
			return;
		}
			
		IgniteCache<String, Integer> cache = ignite.getOrCreateCache("eventCache3");
		
		List<MessageFlowEventDto>events = null;
		
		Lock lock = cache.lock(MessageFlowTypeEvent.COMPONENT_INBOUND_PROCESSING_COMPLETE + "-" + identifier.getComponentPath());
	
		try {
		    // Acquire the lock
		    lock.lock();
		  
			events = messagingFlowService.getEvents(identifier.getComponentRouteId(), 20, MessageFlowTypeEvent.COMPONENT_INBOUND_PROCESSING_COMPLETE);
			
			// Each event read we add to the queue and then delete the event and update the master table.
			for (MessageFlowEventDto event : events) {
				long messageFlowId = event.getMessageFlowId();
				
				producerTemplate.sendBodyAndHeader("direct:handleInboundProcessingCompleteEvent-" + identifier.getComponentPath(), event.getId(), MessageProcessor.MESSAGE_FLOW_STEP_ID, messageFlowId);
			}
		} finally {
		    // Release the lock
		    lock.unlock();
		}	
	}

	
	/**
	 * A timer to process messages which have completed inbound processing.
	 */
	@Scheduled(fixedRate = 100)
	public void processComponentOutboundProcessingCompleteEvents() {		
		if (!camelContext.isStarted()) {
			return;
		}
			
		IgniteCache<String, Integer> cache = ignite.getOrCreateCache("eventCache3");
		
		List<MessageFlowEventDto>events = null;
		
		Lock lock = cache.lock(MessageFlowTypeEvent.COMPONENT_OUTBOUND_PROCESSING_COMPLETE + "-" + identifier.getComponentPath());
	
		try {
		    // Acquire the lock
		    lock.lock();
		  
			events = messagingFlowService.getEvents(identifier.getComponentRouteId(), 20, MessageFlowTypeEvent.COMPONENT_OUTBOUND_PROCESSING_COMPLETE);
			
			// Each event read we add to the queue and then delete the event and update the master table.
			for (MessageFlowEventDto event : events) {
				long messageFlowId = event.getMessageFlowId();
				
				producerTemplate.sendBodyAndHeader("direct:handleOutboundProcessCompleteEvent-" + identifier.getComponentPath(), event.getId(), MessageProcessor.MESSAGE_FLOW_STEP_ID, messageFlowId);
			}
		} finally {
		    // Release the lock
		    lock.unlock();
		}	
	}
	
	
	@Override
	public void setRoute(String route) {
		identifier.setRouteName(route);
	}


	@Override
	public void configure() throws Exception {
		from("direct:filterMessage-" + identifier.getComponentPath())
			.process(new Processor() {
				
				@Override
				public void process(Exchange exchange) throws Exception {
					//TODO change this - filter the message in the db
					System.out.println("Filtered");
				}
			});
	}
}
