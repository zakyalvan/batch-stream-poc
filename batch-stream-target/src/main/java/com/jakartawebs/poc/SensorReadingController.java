package com.jakartawebs.poc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensorReadingController {
	@Autowired
	private SensorReadingRepository sensorReadings;
	
	@RequestMapping(value="/sensor-readings", method=RequestMethod.GET)
	public List<SensorReading> listAll() {
		return sensorReadings.findAll();
	}
}
