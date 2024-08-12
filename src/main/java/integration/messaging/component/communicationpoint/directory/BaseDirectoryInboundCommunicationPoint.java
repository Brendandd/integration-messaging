package integration.messaging.component.communicationpoint.directory;

import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.processor.idempotent.jpa.JpaMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import integration.messaging.component.communicationpoint.BaseInboundCommunicationPoint;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base class for all directory input communication points. This components
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

        from(getFromUriString()).routeId(identifier.getComponentPath() + "-inbound").routeGroup(identifier.getComponentPath())
                .autoStartup(isInboundRunning)

                // Store the message and an event in a single transaction.
                .transacted("").setHeader("contentType", simple(getContentType()))

                .bean(messageProcessor, "storeInboundMessageFlowStep(*," + identifier.getComponentRouteId() + ")")
                .bean(messageProcessor, "recordInboundProcessingCompleteEvent(*)");

        TemplatedRouteBuilder.builder(camelContext, "handleInboundProcessingCompleteEventTemplate")
                .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
                .add();

        TemplatedRouteBuilder.builder(camelContext, "readMessageFromInboundProcessingCompleteQueueTemplate")
                .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
                .parameter("componentRouteId", identifier.getComponentRouteId()).add();

        TemplatedRouteBuilder.builder(camelContext, "inboundCommunicationPointOutboundProcessorTemplate")
                .parameter("isOutboundRunning", isOutboundRunning).parameter("componentPath", identifier.getComponentPath())
                .parameter("componentRouteId", identifier.getComponentRouteId())
                .bean("messageForwardingPolicy", getMessageForwardingPolicy()).add();

        TemplatedRouteBuilder.builder(camelContext, "outboundProcessingCompleteTopicConsumer")
                .parameter("componentPath", identifier.getComponentPath()).add();
    }
}