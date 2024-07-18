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
package ch.autumo.beetroot.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.crud.EventHandler;
import ch.autumo.beetroot.crud.UpdateListener;
import ch.autumo.beetroot.handler.properties.Property;


/**
 * Log4j log event appender.
 */
@Plugin(name = "LogEventAppender", category = "Core", elementType = Appender.ELEMENT_TYPE)
public class LogEventAppender extends AbstractAppender {

	/** Default log size . */
	public static int DEFAULT_LOG_SIZE = 100;
	
	private static int size = DEFAULT_LOG_SIZE;
	private static LogEventList<LogEvent> logEvents = new LogEventList<>(size);

	private static boolean isInitialized = false;
	
    
    /**
     * Log event appender.
     *  
     * @param name name
     * @param filter filter
     */
    protected LogEventAppender(String name, Filter filter) {
        super(name, filter, PatternLayout.createDefaultLayout(), true, null);
    }

    @PluginFactory
    public static LogEventAppender createAppender(@PluginAttribute("name") String name) {
        return new LogEventAppender(name, null);
    }
    
    @Override
    public void append(LogEvent event) {
        logEvents.add(event);
    }

    /**
     * Get all collected log events.
     * 
     * @return log events
     */
    public static List<LogEvent> getLogEvents() {
        return Collections.unmodifiableList(logEvents);
    }

    /**
     * Initialize the appender.
     * This call must be made!
     */
	public static void initializeAppender() {
		
		if (isInitialized)
			return;
		
		String sSize;
		try {
			sSize = BeetRootDatabaseManager.getInstance().getProperty("log.size");
			if (sSize != null) {
				try {
					size = Integer.valueOf(sSize).intValue();
				} catch (Exception e) {
					size = DEFAULT_LOG_SIZE;
				}
			} else {
				size = DEFAULT_LOG_SIZE;
			}	
		} catch (Exception e) {
			size = DEFAULT_LOG_SIZE;
		}

		// Recreate and re-fill
		final LogEvent tempEvents[] = logEvents.toArray(new LogEvent[logEvents.size()]);
		logEvents = new LogEventList<>(size);
		logEvents.addAll(Arrays.asList(tempEvents));
		
		// Install CRUD update listener for settings
		EventHandler.getInstance().addUpdateListener(Property.class, new UpdateListener() {
			@Override
			public boolean beforeUpdate(Model bean) {
				return false;
			}
			@Override
			public void afterUpdate(Model bean) {
				final Property prop = (Property) bean;
				if (prop.getName().equals("log.size")) {
					int val = size;
					try {
						val = Integer.valueOf(prop.getValue()).intValue();	
					} catch (Exception e) {
					}
					if (val != size) {
						size = val;
						logEvents.setMaxSize(size);
						logEvents.resize();
					}
				}
			}
		});
		
		isInitialized = true;
	} 
	
}
