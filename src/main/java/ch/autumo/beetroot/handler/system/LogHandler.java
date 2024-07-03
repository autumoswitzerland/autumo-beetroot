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
package ch.autumo.beetroot.handler.system;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.logging.LogEventAppender;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.modules.log.LogFactory;


/**
 * Log extended handler.
 */
public class LogHandler extends BaseHandler {

	private final static Logger LOG = LoggerFactory.getLogger(LogHandler.class);

	/** Default log size . */
	public static int DEFAULT_LOG_SIZE = LogEventAppender.DEFAULT_LOG_SIZE;
	
	/**
	 * If there's a refresh time (tag: {$refreshTime}), this is the
	 * default value in seconds.
	 */
	public static final int DEFAULT_REFRESH_TIME = 60;

	/**
	 * Minimum refresh rate for log refresh.
	 */
	public static final int MINIMUM_REFRESH_TIME = 15;
	
	/** Log pattern. Note: 'im' not recognized in pattern. */
	private final static String LOG_PATTERN = "%highlight{%-5p}{TRACE=white} %style{%d{yyyyMMdd-HH:mm:ss.SSS}}{bright_black} %style{[%-26.26t]}{magenta} %style{%-35.35c{1.1.1.*}}{cyan} %style{:}{bright_black} %.-1000m%ex%n";
	
	private static Pattern PATTERN_REFRESH = Pattern.compile("\\{\\$logRefreshTime\\}");
	
	private final PatternLayout layout;
	
	
	/**
	 * @param entity
	 */
	public LogHandler(String entity) {
		super(entity);
		this.layout = PatternLayout.newBuilder()
                .withPattern(LOG_PATTERN)
                .withCharset(StandardCharsets.UTF_8)
                .build();
	}

	/**
	 * Replaces variables in the whole page.
	 */
	@Override
	public String replaceVariables(String text, BeetRootHTTPSession session) {
		
		// timer
		if (text.contains("{$refresh}")) {
			text = text.replace("{$refresh}", LanguageManager.getInstance().translate("base.name.refresh", session.getUserSession()));
		} else if (text.contains("{$logRefreshTime}")) { // refresh time if needed!
			String time;
			try {
				time = BeetRootDatabaseManager.getInstance().getProperty("log.refresh.time");
				if (time == null) {
					LOG.warn("Couldn't load refresh time from database; setting it to 60s.");
					time = ""+DEFAULT_REFRESH_TIME; //seconds
				}
				
				if (Integer.valueOf(time).intValue() < 15) {
					// minimum polling time = 15s
					time = ""+DEFAULT_REFRESH_TIME; //seconds
				}
				
				text = PATTERN_REFRESH.matcher(text).replaceFirst(time);
			} catch (Exception e) {
				LOG.warn("Couldn't load refresh time from database; setting it to 60s.", e);
				text = PATTERN_REFRESH.matcher(text).replaceFirst(""+DEFAULT_REFRESH_TIME);
			}
		}
		return text;
	}
	
	/**
	 * Replaces variables template only.
	 */
	@Override
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		
		int nlines = DEFAULT_LOG_SIZE;
		String sSize;
		try {
			sSize = BeetRootDatabaseManager .getInstance().getProperty("log.size");
			if (sSize != null) {
				try {
					nlines = Integer.valueOf(sSize).intValue();
				} catch (Exception e) {
					LOG.error("Couldn't read 'log.size' from database properties! Using default log size '"+DEFAULT_LOG_SIZE+"'!", e);
					nlines = DEFAULT_LOG_SIZE;
				}
			} else {
				nlines = DEFAULT_LOG_SIZE;
			}
		} catch (SQLException e) {
			LOG.error("Couldn't read 'log.size' from database properties! Using default log size '"+DEFAULT_LOG_SIZE+"'!", e);
			nlines = DEFAULT_LOG_SIZE;
		}
		text = text.replace("{$logSize}", ""+nlines);
		
		ClientAnswer answer;
		try {
			answer = LogFactory.getInstance().getLog();
		} catch (Exception e) {
			LOG.error("Couldn't read answer!", e);
			// Nothing to be done
			answer = new ClientAnswer();
			answer.setEntity("No backend server log available!"); //TODO trans
		}

		text = text.replace("{$logSource}", answer.getEntity());
		
		final Object obj = answer.getObject();
		if (obj != null) {
			final StringBuffer buf = new StringBuffer();
			@SuppressWarnings("unchecked")
			final List<LogEvent> events = (List<LogEvent>) obj;
			for (Iterator<LogEvent> iterator = events.iterator(); iterator.hasNext();) {
				final LogEvent event = iterator.next();
				final String line = this.format(event);
				buf.append(line);
			}
			text = text.replace("{$logData}", buf.toString());
		}
		
		return text;		
	}
	
    public String format(LogEvent event) {
        String formattedMessage = layout.toSerializable(event);

        // You can now post-process the formattedMessage if needed
        formattedMessage = processCustomPatternReferences(formattedMessage, event);

        // Escape HTML characters to avoid issues with displaying in HTML
        formattedMessage = escapeHtml(formattedMessage);

        // Replace ANSI escape codes with corresponding HTML styles
        formattedMessage = replaceAnsiWithHtml(formattedMessage);

        return formattedMessage;
    }
    
    private String processCustomPatternReferences(String formattedMessage, LogEvent event) {
        // Example: Replace %im with custom logic
        formattedMessage = formattedMessage.replace("%im", customIMConverter(event));
        return formattedMessage;
    }    

    private String customIMConverter(LogEvent event) {
        // Example custom logic for %im
        String message = event.getMessage().getFormattedMessage();
        // Apply your custom formatting logic here
        return "Custom IM: " + message; // Example formatting
    }
    
    private String escapeHtml(String input) {
        if (Strings.isBlank(input)) {
            return input;
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    private String replaceAnsiWithHtml(String input) {

        // Standard colors
        input = input.replaceAll("\\[30m", "<span style=\"color: darkgrey;\">");
        input = input.replaceAll("\\[31m", "<span style=\"color: darkred;\">");
        input = input.replaceAll("\\[32m", "<span style=\"color: green;\">");
        input = input.replaceAll("\\[33m", "<span style=\"color: yellow;\">");
        input = input.replaceAll("\\[34m", "<span style=\"color: blue;\">");
        input = input.replaceAll("\\[35m", "<span style=\"color: magenta;\">");
        input = input.replaceAll("\\[36m", "<span style=\"color: cyan;\">");
        input = input.replaceAll("\\[37m", "<span style=\"color: lightgrey;\">");

        // Standard bright colors
        input = input.replaceAll("\\[1;30m", "<span style=\"color: lightgrey;\">");
        input = input.replaceAll("\\[1;31m", "<span style=\"color: red;\">");
        input = input.replaceAll("\\[1;32m", "<span style=\"color: #90EE90;\">");
        input = input.replaceAll("\\[1;33m", "<span style=\"color: #FFFFE0;\">");
        input = input.replaceAll("\\[1;34m", "<span style=\"color: #ADD8E6;\">");
        input = input.replaceAll("\\[1;35m", "<span style=\"color: #FF80FF;\">");
        input = input.replaceAll("\\[1;36m", "<span style=\"color: #E0FFFF;\">");
        input = input.replaceAll("\\[1;37m", "<span style=\"color: white;\">");
        
        // beetRoot ANSI colors
        input = input.replaceAll("\\[90m", "<span style=\"color: darkgrey;\">");
        input = input.replaceAll("\\[91m", "<span style=\"color: red;\">");
        input = input.replaceAll("\\[92m", "<span style=\"color: green;\">");
        input = input.replaceAll("\\[93m", "<span style=\"color: yellow;\">");
        input = input.replaceAll("\\[94m", "<span style=\"color: blue;\">");
        input = input.replaceAll("\\[95m", "<span style=\"color: magenta;\">");
        input = input.replaceAll("\\[96m", "<span style=\"color: cyan;\">");
        input = input.replaceAll("\\[97m", "<span style=\"color: lightgrey;\">");

        // Resets
        input = input.replaceAll("\\[0m", "</span>");
        input = input.replaceAll("\\[m", "</span>");
        
        // Special words
        input = input.replaceAll("READER", "<span style=\"color: green;\">READER</span>");
        input = input.replaceAll("READING", "<span style=\"color: green;\">READING</span>");
        input = input.replaceAll("WRITER", "<span style=\"color: red;\">WRITER</span>");
        input = input.replaceAll("WRITING", "<span style=\"color: red;\">WRITING</span>");
        
        return input;
    }
    
	@Override
	public String getTitle(Session userSession) {
		return "Log";
	}

	@Override
	public String getResource() {
		return "web/html/:lang/system/log.html";
	}

	@Override
	public String getLayout(Session userSession) {
		return "web/html/:lang/blocks/layout_log.html";
	}
	
}
