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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;


/**
 * Log-buffer; used before the logging system is initialized.
 */
public class LogBuffer {

    /** List to buffer logs. */
    private static final List<LogEntry> logBuffer = new ArrayList<>();
	
    /** Enum for logging severity levels. */
    public enum LogLevel {
        INFO, DEBUG, ERROR, WARN, TRACE
    }

    /** Inner class to hold log entry data (message, severity level). */
    private static class LogEntry {
        LogLevel level;
        String message;
    	Object arguments[];    	
        LogEntry(LogLevel level, String message) {
            this.level = level;
            this.message = message;
        }
        LogEntry(LogLevel level, String message, Object... arguments) {
            this.level = level;
            this.message = message;
            this.arguments = arguments;
        }
    }

    /**
     * Log message to log buffer.
     * 
     * @param level level
     * @param message message
     * @param arguments arguments
     */
    public static void log(LogLevel level, String message) {
        logBuffer.add(new LogEntry(level, message));
    }
    
    /**
     * Log message to log buffer.
     * 
     * @param level level
     * @param message message
     * @param arguments arguments
     */
    public static void log(LogLevel level, String message, Object... arguments) {
        logBuffer.add(new LogEntry(level, message, arguments));
    }
    
    /**
     * Flush the buffered logs to the actual SLF4J logger
     * 
     * @param logger the logger
     */
    public static void flushToLogger(Logger logger) {
        for (LogEntry entry : logBuffer) {
            switch (entry.level) {
                case INFO:
            		logger.info(entry.message, entry.arguments);
                    break;
                case DEBUG:
            		logger.debug(entry.message, entry.arguments);
                    break;
                case ERROR:
            		logger.error(entry.message, entry.arguments);
                    break;
                case WARN:
            		logger.warn(entry.message, entry.arguments);
                    break;
                case TRACE:
            		logger.trace(entry.message, entry.arguments);
                    break;
                default:
            		logger.info(entry.message, entry.arguments);
            }
        }
        logBuffer.clear(); // Clear buffer after flushing
    }
    
}
