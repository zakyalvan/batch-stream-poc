package com.jakartawebs.poc;

import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author zakyalvan
 * @since 1.0
 */
@Configuration
public class StreamingConfiguration {
	@Bean
	public GenericHandler<List<SensorReading>> sensorReadingPersister() {
		return new GenericHandler<List<SensorReading>>() {
			@Autowired
			private SensorReadingRepository sensorReadings;
			
			@Override
			@Transactional(propagation=Propagation.REQUIRED)
			public Object handle(List<SensorReading> payload, Map<String, Object> headers) {
				return sensorReadings.save(payload);
			}
		};
	}
	
	@Autowired
	private ConnectionFactory connectionFactory;
	
	@Bean
	public IntegrationFlow dataPushHandlingFlow() {
		return IntegrationFlows.from(Jms.inboundAdapter(connectionFactory).destination("aaa.sensor.reading.push.queue"), spec -> spec.poller(Pollers.fixedDelay(100)))
				.split()
				.transform(Transformers.fromJson(SensorReading.class, JsonObjectMapperFactory.createObjectMapper()))
				.aggregate()
				.handle(sensorReadingPersister())
				.get();
	}
}
