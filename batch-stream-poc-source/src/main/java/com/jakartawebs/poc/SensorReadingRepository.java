package com.jakartawebs.poc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface SensorReadingRepository
		extends JpaRepository<SensorReading, Long>, QueryDslPredicateExecutor<SensorReading> {

}
