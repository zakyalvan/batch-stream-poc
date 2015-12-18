package com.jakartawebs.poc;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import com.jakartawebs.poc.StreamingConfiguration.PushDataFlowConfiguration;

/**
 * Push sensor data to jms queue
 * 
 * @author zakyalvan
 * @see PushDataFlowConfiguration
 * @since 1.0
 */
public class SensorReadingPusher implements ItemWriter<SensorReading> {
	private MessageChannel inputChannel;

	public SensorReadingPusher(MessageChannel inputChannel) {
		this.inputChannel = inputChannel;
	}
	
	@Override
	public void write(List<? extends SensorReading> items) throws Exception {
		inputChannel.send(MessageBuilder.withPayload(items).build());
	}
}