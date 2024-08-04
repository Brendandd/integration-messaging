package integration.messaging;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Apache Camel config.
 * 
 * @author Brendan Douglas
 */
@Configuration
public class CamelConfig {

    @Autowired
    private PlatformTransactionManager jmsTransactionManager; // Autowire your JMS transaction manager

    @Bean
    public SpringTransactionPolicy txRequiresNew() {
        SpringTransactionPolicy policy = new SpringTransactionPolicy();
        policy.setTransactionManager(jmsTransactionManager);
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        return policy;
    }
}