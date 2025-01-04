package integration.messaging.component.communicationpoint.directory;

import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.processor.idempotent.jpa.JpaMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import integration.messaging.component.communicationpoint.BaseInboundCommunicationPoint;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base class for all directory/file input communication points. This components
 * reads the file, stores it and writes and event No other processing should be
 * done here.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseDirectoryInboundCommunicationPoint extends BaseInboundCommunicationPoint {

    public BaseDirectoryInboundCommunicationPoint(String componentName) {
        super(componentName);
    }

    @Autowired
    private EntityManagerFactory emf;

    public String getSourceFolder() {
        return componentProperties.get("SOURCE_FOLDER");
    }

    @Override
    public String getFromUriString() {
        return "file:" + getSourceFolder() + "?idempotent=true&idempotentRepository=#jpaStore" + getOptions();
    }

    @Bean
    protected JpaMessageIdRepository jpaStore() {
        return new JpaMessageIdRepository(emf, "FileRepo");
    }

    @Override
    @DependsOn("routeTemplates")
    public void configure() throws Exception {
        super.configure();

        // Entry point route for directory/file communication points.  Reads the message from the configured location, writes the message details to the database and records
        // an event.
        from(getFromUriString())
            .routeId(identifier.getComponentPath() + "-inbound")
            .routeGroup(identifier.getComponentPath())
            .autoStartup(isInboundRunning)

            // Store the message and an event in a single transaction.
            .transacted()
                .setHeader("contentType", simple(getContentType()))
                .bean(messageProcessor, "storeInboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
                .bean(messageProcessor, "recordInboundProcessingCompleteEvent(*)");

        
        // Outbound processor for a directory/file inbound communication point.  This route will either create an event for further processing by other components or filter
        // the message.  No other processing is done here.
        TemplatedRouteBuilder.builder(camelContext, "inboundCommunicationPointOutboundProcessorTemplate")
            .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
            .parameter("componentRouteId", identifier.getComponentRouteId())
            .parameter("contentType", getContentType())
            .bean("messageForwardingPolicy", getMessageForwardingPolicy())
            .add();

        
        // Add the message flow step id to the outbound processing complete topic so it can be picked up by one or more other components.  This is the final
        // step in directory/file inbound communication points.
        TemplatedRouteBuilder.builder(camelContext, "addToOutboundProcessingCompleteTopicTemplate")
            .parameter("componentPath", identifier.getComponentPath())
            .add();
    }
}