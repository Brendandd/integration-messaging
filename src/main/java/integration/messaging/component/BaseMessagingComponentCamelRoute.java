package integration.messaging.component;

import org.springframework.stereotype.Component;

import integration.messaging.BaseMessagingCamelRoute;

/**
 * Base class for all Apache Camel messaging component routes.
 * 
 * @author Brendan Douglas
 *
 */
@Component
public class BaseMessagingComponentCamelRoute extends BaseMessagingCamelRoute {

		@Override
		public void configure() throws Exception {
			onException(Exception.class)
			.maximumRedeliveries(-1)
			.redeliveryDelay(1000);
		}
		
		//TODO add more exception handling.
}