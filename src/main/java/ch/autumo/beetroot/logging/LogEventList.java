/**
 * Copyright 2024 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 * 
 */
package ch.autumo.beetroot.logging;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Log event list with a maximum size and with shadow copies
 * of log events.
 * 
 * @param <LogEvent>
 */
public class LogEventList<LogEvent> extends CopyOnWriteArrayList<LogEvent> {
	
    private static final long serialVersionUID = 1L;
    
	private int maxSize = 0;

	/**
	 * Create a log event list
	 * 
	 * @param maxSize initial max. size
	 */
    public LogEventList(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Set a new max. size for the list.
     * 
     * @param maxSize new max. size
     */
    public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

    /**
     * Resize the list, if it is too big.
     */
    public synchronized void resize() {
    	// We remove until maxSize is reached
    	while (size() > maxSize)
            remove(0); // remove the first element
    }
    
	@Override
    public boolean add(LogEvent e) {

    	// We remove until maxSize is reached
    	while (size() >= maxSize)
            remove(0); // remove the first element
        
        return super.add(e);
    }
    
}
