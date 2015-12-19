package com.jakartawebs.poc;

import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonObjectMapperFactory {
	/**
	 * Create custom object mapper factory.
	 *
	 * @return
	 */
	public static JsonObjectMapper createObjectMapper() {
		ObjectMapper delegatedObjectMapper = new ObjectMapper();
		delegatedObjectMapper.registerModule(new Hibernate5Module());
		delegatedObjectMapper.registerModule(new Jdk8Module());
		delegatedObjectMapper.registerModule(new JavaTimeModule());

		Jackson2JsonObjectMapper integrationObjectMapper = new Jackson2JsonObjectMapper(delegatedObjectMapper);

		return integrationObjectMapper;
	}
}