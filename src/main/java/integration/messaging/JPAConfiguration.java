package integration.messaging;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@EntityScan(basePackages = { "integration.core.domain", "org.apache.camel.processor.idempotent.jpa" })
@Configuration
public class JPAConfiguration {

}
