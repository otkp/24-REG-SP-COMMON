package org.epragati.util;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

@Component
public class DateUtils {
	
	public Duration diff(LocalTime oldDate, LocalTime newDate){
		
		
		return Duration.between(oldDate, newDate);
	}

}
