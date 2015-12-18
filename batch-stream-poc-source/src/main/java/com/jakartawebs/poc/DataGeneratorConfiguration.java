package com.jakartawebs.poc;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataGeneratorConfiguration {
	@Bean
	public MessageSource<Double> dataMessageSource() {
		return new MessageSource<Double>() {
			@Override
			public Message<Double> receive() {
				return MessageBuilder.withPayload(new Random().nextDouble()).build();
			}
		};
	}
	
	@Bean
	public GenericTransformer<Double, SensorReading> doubleValueToSensorReading() {
		return new GenericTransformer<Double, SensorReading>() {
			@Override
			public SensorReading transform(Double source) {
				SensorReading sensorReading = new SensorReading();
				sensorReading.setValue(source);
				sensorReading.setCreatedDate(new Date());
				return sensorReading;
			}
		};
	}
	
	@Bean
	public GenericHandler<SensorReading> persistingHandler() {
		return new GenericHandler<SensorReading>() {
			@PersistenceContext
			private EntityManager entityManager;
			
			@Override
			@Transactional(propagation=Propagation.REQUIRED)
			public Object handle(SensorReading payload, Map<String, Object> headers) {
				return entityManager.merge(payload);
			}
		};
	}
	
	@Bean
	public IntegrationFlow generateFlow() {
		return IntegrationFlows.from(dataMessageSource(), sourceSpec -> sourceSpec.poller(Pollers.fixedDelay(1000)))
				.transform(doubleValueToSensorReading())
				.handle(persistingHandler())
				.handle(message -> System.out.println("============ New sensor reading generated"))
				.get();
	}
}
