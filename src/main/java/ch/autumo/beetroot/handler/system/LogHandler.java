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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.NoConfigHandler;
import ch.autumo.beetroot.logging.LogEventAppender;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.modules.log.LogFactory;
import ch.autumo.beetroot.utils.system.OS;
import ch.autumo.beetroot.utils.web.Web;


/**
 * Log extended handler.
 */
public class LogHandler extends NoConfigHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LogHandler.class);

	/** Default log size . */
	public static final int DEFAULT_LOG_SIZE = LogEventAppender.DEFAULT_LOG_SIZE;

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
	private static final String LOG_PATTERN = "%highlight{%-5p}{TRACE=white} %style{%d{yyyyMMdd-HH:mm:ss.SSS}}{bright_black} %style{[%-26.26t]}{magenta} %style{%-35.35c{1.1.1.*}}{cyan} %style{:}{bright_black} %.-1000m%ex%n";

	private final PatternLayout layout;


	/**
	 * @param entity
	 */
	public LogHandler(String entity) {
		super(entity);
		this.layout = PatternLayout.newBuilder()
                .withPattern(LOG_PATTERN)
                .withCharset(StandardCharsets.UTF_8)
                .withDisableAnsi(false)
                .build();
	}

	/**
	 * Replaces variables in the whole page.
	 */
	@Override
	public void renderAll(BeetRootHTTPSession session) {
		// timer
		setVarAll("refresh", LanguageManager.getInstance().translate("base.name.refresh", session.getUserSession()));

		String time;
		try {
			time = BeetRootDatabaseManager.getInstance().getProperty("log.refresh.time");
			if (time == null) {
				LOG.warn("Couldn't load refresh time from database; setting it to 60s.");
				time = ""+DEFAULT_REFRESH_TIME; //seconds
			}

			if (Integer.parseInt(time) < 15) {
				// minimum polling time = 15s
				time = ""+DEFAULT_REFRESH_TIME; //seconds
			}


			setVarAll("logRefreshTime", time);

		} catch (Exception e) {
			LOG.warn("Couldn't load refresh time from database; setting it to 60s.", e);
			setVarAll("logRefreshTime", DEFAULT_REFRESH_TIME);
		}
	}

	/**
	 * Replaces variables template only.
	 */
	@Override
	public void render(BeetRootHTTPSession session) {

		int nlines = DEFAULT_LOG_SIZE;
		String sSize;
		try {
			sSize = BeetRootDatabaseManager .getInstance().getProperty("log.size");
			if (sSize != null) {
				try {
					nlines = Integer.parseInt(sSize);
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
		setVar("logSize", ""+nlines);

		ClientAnswer answer;
		try {
			answer = LogFactory.getInstance().getLog();
		} catch (Exception e) {
			LOG.error("Couldn't read answer!", e);
			// Nothing to be done
			answer = new ClientAnswer();
			answer.setEntity(LanguageManager.getInstance().translate("system.log.nobackend", session.getUserSession()));
		}

		setVar("logSource", answer.getEntity());

		final Object obj = answer.getObject();
		if (obj != null) {
			final StringBuilder buf = new StringBuilder();
			@SuppressWarnings("unchecked")
			final List<LogEvent> events = (List<LogEvent>) obj;
			for (Iterator<LogEvent> iterator = events.iterator(); iterator.hasNext();) {
				final LogEvent event = iterator.next();
				final String line = this.format(event);
				buf.append(line);
			}
			setVar("logData", buf.toString());
		} else {
			setVar("logData", "-");
		}
	}

    public String format(LogEvent event) {
        String formattedMessage = layout.toSerializable(event);
        // Post-process the formattedMessage if needed
        // formattedMessage = processCustomPatternReferences(formattedMessage, event);
        // Escape HTML characters to avoid issues with displaying in HTML
        formattedMessage = Web.escapeHtmlReserved(formattedMessage);
        // Replace ANSI escape codes with corresponding HTML styles
        formattedMessage = replaceAnsiWithHtml(formattedMessage);
        return formattedMessage;
    }

    /**
    private String processCustomPatternReferences(String formattedMessage, LogEvent event) {
        // Example: Replace %im with custom logic
        formattedMessage = formattedMessage.replace("%im", customIMConverter(event));
        return formattedMessage;
    }
    private String customIMConverter(LogEvent event) {
        // Example custom logic for %im
        String message = event.getMessage().getFormattedMessage();
        // Apply your custom formatting logic here
        return message;
    }
    */

    private String replaceAnsiWithHtml(String input) {
        // If the platform is Windows, handle full ANSI sequences including \u001b
        if (OS.isWindows() || OS.isUnix()) {

            // Full escape sequences (with \u001b) for Windows and Unix

	        // Standard colors
            input = input.replace("\u001b[30m", "<span style=\"color: darkgrey;\">");
            input = input.replace("\u001b[31m", "<span style=\"color: darkred;\">");
            input = input.replace("\u001b[32m", "<span style=\"color: green;\">");
            input = input.replace("\u001b[33m", "<span style=\"color: yellow;\">");
            input = input.replace("\u001b[34m", "<span style=\"color: blue;\">");
            input = input.replace("\u001b[35m", "<span style=\"color: magenta;\">");
            input = input.replace("\u001b[36m", "<span style=\"color: cyan;\">");
            input = input.replace("\u001b[37m", "<span style=\"color: lightgrey;\">");

            // Standard bright colors
            input = input.replace("\u001b[1;30m", "<span style=\"color: lightgrey;\">");
            input = input.replace("\u001b[1;31m", "<span style=\"color: red;\">");
            input = input.replace("\u001b[1;32m", "<span style=\"color: #90EE90;\">");
            input = input.replace("\u001b[1;33m", "<span style=\"color: #FFFFE0;\">");
            input = input.replace("\u001b[1;34m", "<span style=\"color: #ADD8E6;\">");
            input = input.replace("\u001b[1;35m", "<span style=\"color: #FF80FF;\">");
            input = input.replace("\u001b[1;36m", "<span style=\"color: #E0FFFF;\">");
            input = input.replace("\u001b[1;37m", "<span style=\"color: white;\">");

	        // beetRoot ANSI colors
	        input = input.replace("\u001b[90m", "<span style=\"color: darkgrey;\">");
	        input = input.replace("\u001b[91m", "<span style=\"color: red;\">");
	        input = input.replace("\u001b[92m", "<span style=\"color: green;\">");
	        input = input.replace("\u001b[93m", "<span style=\"color: yellow;\">");
	        input = input.replace("\u001b[94m", "<span style=\"color: blue;\">");
	        input = input.replace("\u001b[95m", "<span style=\"color: magenta;\">");
	        input = input.replace("\u001b[96m", "<span style=\"color: cyan;\">");
	        input = input.replace("\u001b[97m", "<span style=\"color: lightgrey;\">");

            // Resets (any reset sequence)
            input = input.replaceAll("\u001b\\[0m", "</span>");
            input = input.replaceAll("\u001b\\[m", "</span>");

        } else {

        	// macOSm Linux: Handle only the color codes, no \u001b present

	        // Standard colors
	        input = input.replace("[30m", "<span style=\"color: darkgrey;\">");
	        input = input.replace("[31m", "<span style=\"color: darkred;\">");
	        input = input.replace("[32m", "<span style=\"color: green;\">");
	        input = input.replace("[33m", "<span style=\"color: yellow;\">");
	        input = input.replace("[34m", "<span style=\"color: blue;\">");
	        input = input.replace("[35m", "<span style=\"color: magenta;\">");
	        input = input.replace("[36m", "<span style=\"color: cyan;\">");
	        input = input.replace("[37m", "<span style=\"color: lightgrey;\">");

	        // Standard bright colors
	        input = input.replace("[1;30m", "<span style=\"color: lightgrey;\">");
	        input = input.replace("[1;31m", "<span style=\"color: red;\">");
	        input = input.replace("[1;32m", "<span style=\"color: #90EE90;\">");
	        input = input.replace("[1;33m", "<span style=\"color: #FFFFE0;\">");
	        input = input.replace("[1;34m", "<span style=\"color: #ADD8E6;\">");
	        input = input.replace("[1;35m", "<span style=\"color: #FF80FF;\">");
	        input = input.replace("[1;36m", "<span style=\"color: #E0FFFF;\">");
	        input = input.replace("[1;37m", "<span style=\"color: white;\">");

	        // beetRoot ANSI colors
	        input = input.replace("[90m", "<span style=\"color: darkgrey;\">");
	        input = input.replace("[91m", "<span style=\"color: red;\">");
	        input = input.replace("[92m", "<span style=\"color: green;\">");
	        input = input.replace("[93m", "<span style=\"color: yellow;\">");
	        input = input.replace("[94m", "<span style=\"color: blue;\">");
	        input = input.replace("[95m", "<span style=\"color: magenta;\">");
	        input = input.replace("[96m", "<span style=\"color: cyan;\">");
	        input = input.replace("[97m", "<span style=\"color: lightgrey;\">");

	        // Resets
	        input = input.replace("[0m", "</span>");
	        input = input.replace("[m", "</span>");
        }

        // Special words (artistic freedom)
        input = input.replace("READER", "<span style=\"color: green;\">READER</span>");
        input = input.replace("READING", "<span style=\"color: green;\">READING</span>");
        input = input.replace("WRITER", "<span style=\"color: red;\">WRITER</span>");
        input = input.replace("WRITING", "<span style=\"color: red;\">WRITING</span>");

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
