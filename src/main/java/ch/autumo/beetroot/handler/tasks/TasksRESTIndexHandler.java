/**
 * Copyright 2022 autumo GmbH, Michael Gasche.
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
package ch.autumo.beetroot.handler.tasks;

import ch.autumo.beetroot.handler.DefaultRESTIndexHandler;

/**
 * Tasks JSON-REST index handler.
 */
public class TasksRESTIndexHandler extends DefaultRESTIndexHandler {
	
	public TasksRESTIndexHandler(String entity) {
		super(entity);
	}

	public TasksRESTIndexHandler(String entity, String msg) {
		super(entity);
	}

	@Override
	public Class<?> getBeanClass() {
		return Task.class;
	}
	
}
