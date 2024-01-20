/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.autumo.beetroot.utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ch.autumo.beetroot.BeetRootDatabaseManager;

public class Time {

	/**
	 * Get readable duration.
	 * 
	 * @param durationInMilliseconds duration in ms
	 * @param printUpTo The maximum timeunit that should be printed
	 * @return readable duration
	 */
	public static String getReadableDuration(long durationInMilliseconds, TimeUnit printUpTo) {
		
		long dy = TimeUnit.MILLISECONDS.toDays(durationInMilliseconds);
		long allHours = TimeUnit.MILLISECONDS.toHours(durationInMilliseconds);
		long allMinutes = TimeUnit.MILLISECONDS.toMinutes(durationInMilliseconds);
		long allSeconds = TimeUnit.MILLISECONDS.toSeconds(durationInMilliseconds);
		long allMilliSeconds = TimeUnit.MILLISECONDS.toMillis(durationInMilliseconds);
		
		final long hr = allHours - TimeUnit.DAYS.toHours(dy);
		final long min = allMinutes - TimeUnit.HOURS.toMinutes(allHours);
		final long sec = allSeconds - TimeUnit.MINUTES.toSeconds(allMinutes);
		final long ms = allMilliSeconds - TimeUnit.SECONDS.toMillis(allSeconds);
		
		switch (printUpTo) {
			case DAYS: return String.format("%d Days %d Hours %d Minutes %d Seconds %d Milliseconds", dy, hr, min, sec, ms);
			case HOURS: return String.format("%d Hours %d Minutes %d Seconds %d Milliseconds", hr, min, sec, ms);
			case MINUTES: return String.format("%d Minutes %d Seconds %d Milliseconds", min, sec, ms);
			case SECONDS: return String.format("%d Seconds %d Milliseconds", sec, ms);
			case MILLISECONDS: return String.format("%d Milliseconds", ms);
			default: return String.format("%d Days %d Hours %d Minutes %d Seconds %d Milliseconds", dy, hr, min, sec, ms);
		}
	}	
	
	/**
	 * Get a timestamp representation that can be shown in GUI
	 * 
	 * @param tmestamp from DB
	 * @return timestamp representable date
	 */
	public static String getGUIDate(Timestamp tsFromDb) {

		// Oh boy...
		final Instant instant = tsFromDb.toInstant();
		final Instant instantTruncated = instant.with(ChronoField.NANO_OF_SECOND, 0);
		final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());
		final String output = formatter.format(instantTruncated);
		
		return output.replace("T", " ");
	}

	/**
	 * Get a time-stamp representation that can be stored in DB.
	 * 
	 * Note that this code returns a 'to_timestamp'-call when you are using
	 * an Oracle database, hence that value cannot be enclosed with
	 * apostrophes '...'; in case of Oracle it looks like this:
	 * 
	 * "to_timestamp('2022-12-21 23:59:59.999', 'YYYY-MM-DD HH24:MI:SS.FF')"
	 * 
	 * and in case of all other databases:
	 * 
	 * "2022-12-21 23:59:59.999"
	 * 
	 * @return time-stamp a time-stamp representation that works with used DB
	 */
	public static String nowTimeStamp() {
		return Time.timeStamp(new Date());
	}
    
	/**
	 * Get a time-stamp representation that can be stored in DB.
	 * 
	 * Note that this code returns a 'to_timestamp'-call when you are using
	 * an Oracle database, hence that value cannot be enclosed with
	 * apostrophes '...'; in case of Oracle it looks like this:
	 * 
	 * "to_timestamp('2022-12-21 23:59:59.999', 'YYYY-MM-DD HH24:MI:SS.FF')"
	 * 
	 * and in case of all other databases:
	 * 
	 * "2022-12-21 23:59:59.999"
	 * 
	 * @param date create time-stamp out of given date
	 * @return time-stamp a time-stamp representation that works with used DB
	 */
	public static String timeStamp(Date date) {
		
		String ts_str = null;
		
		final Timestamp ts = new Timestamp(date.getTime());
		ts_str = ts.toLocalDateTime().toString();

		if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
			ts_str = ts_str.replace("T", " ");
			ts_str = "to_timestamp('"+ts_str+"', 'YYYY-MM-DD HH24:MI:SS.FF')";
		}
		
		return ts_str;
	}
	
}
