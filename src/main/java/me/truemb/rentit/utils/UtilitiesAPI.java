package me.truemb.rentit.utils;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Locale;

public class UtilitiesAPI {

	
	public static TemporalAmount getTimeParsed(String feString) {
		try {
		    if (Character.isUpperCase(feString.charAt(feString.length() - 1))) {
		        return Period.parse("P" + feString);
		    } else {
		        return Duration.parse("PT" + feString);
		    }
		}catch(DateTimeParseException ex){
			return null;
		}
	}
	
	public static Timestamp getNewTimestamp(String feString) {
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		
		for(String s : feString.split("(?<=[a-zA-Z])")) {
			//Convert to LocalDateTime. Use no offset for timezone
			LocalDateTime time = LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.ofHours(0));

			//Add time
			time = time.plus(getTimeParsed(s)); //Time from String

			//Convert back to instant, again, no time zone offset.
			Instant output = time.atZone(ZoneOffset.ofHours(0)).toInstant();

			ts = Timestamp.from(output);
		}
		
		return ts;
	}

	public static Timestamp getTimestampBefore(Timestamp ts, String feString) {
		
		for(String s : feString.split("(?<=[a-zA-Z])")) {
			//Convert to LocalDateTime. Use no offset for timezone
			LocalDateTime time = LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.ofHours(0));

			//Add time
			time = time.minus(getTimeParsed(s)); //Time from String

			//Convert back to instant, again, no time zone offset.
			Instant output = time.atZone(ZoneOffset.ofHours(0)).toInstant();

			ts = Timestamp.from(output);
		}
		
		return ts;
	}

	public static Timestamp addTimeToTimestamp(Timestamp ts, String feString) {
		
		for(String s : feString.split("(?<=[a-zA-Z])")) {
			//Convert to LocalDateTime. Use no offset for timezone
			LocalDateTime time = LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.ofHours(0));

			//Add time
			time = time.plus(getTimeParsed(s)); //Time from String

			//Convert back to instant, again, no time zone offset.
			Instant output = time.atZone(ZoneOffset.ofHours(0)).toInstant();

			ts = Timestamp.from(output);
		}
		
		return ts;
	}
	
	public static String getHumanReadablePriceFromNumber(double number){
	    NumberFormat fmtShort = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
	    fmtShort.setMinimumFractionDigits(3);
	    
	    return fmtShort.format(number);

	}
}
