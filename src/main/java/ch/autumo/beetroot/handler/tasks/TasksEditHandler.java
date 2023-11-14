/**
 * 
 * Copyright (c) 2023 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.handler.tasks;

import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultEditHandler;

/**
 * Tasks edit handler. 
 */
public class TasksEditHandler extends DefaultEditHandler {
	
	public TasksEditHandler(String entity) {
		super(entity);
	}

	public TasksEditHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return TasksIndexHandler.class;
	}

	@Override
	public Class<?> getBeanClass() {
		return Task.class;
	}
	
	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("Administrator") ||
				userSession.getUserRole().equalsIgnoreCase("Operator");
	}
	
}
