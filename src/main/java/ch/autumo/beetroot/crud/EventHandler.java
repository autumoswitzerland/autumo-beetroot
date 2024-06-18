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
package ch.autumo.beetroot.crud;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.utils.DB;


/**
 * CRUD event handler.
 */
public class EventHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(EventHandler.class.getName());
	
	private static EventHandler handler;
	
	private final Map<Class<?>, CreateListener> createListeners = new HashMap<Class<?>, CreateListener>();
	private final Map<Class<?>, UpdateListener> updateListeners = new HashMap<Class<?>, UpdateListener>();
	private final Map<Class<?>, DeleteListener> deleteListeners = new HashMap<Class<?>, DeleteListener>();
	

	private EventHandler() {
	}
	
	/**
	 * Get event handler (singleton).
	 * 
	 * @return event handler
	 */
	public static EventHandler getInstance() {
		if (handler == null) {
			handler = new EventHandler();
		}
		return handler;
	}	

	/**
	 * Add a create listener for create notifications for a specific entity.
	 * If a listener already exists for that entity, it is not added.
	 * 
	 * @param entityClass entity
	 * @param listener create listener
	 */
	public void newCreateListener(Class<?> entityClass, CreateListener listener) {
		if (!createListeners.containsKey(entityClass)) {
			createListeners.put(entityClass, listener);
		}
	}
	
	/**
	 * Add an update listener for update notifications for a specific entity.
	 * If a listener already exists for that entity, it is not added.
	 * 
	 * @param entityClass entity
	 * @param listener update listener
	 */
	public void addUpdateListener(Class<?> entityClass, UpdateListener listener) {
		if (!updateListeners.containsKey(entityClass)) {
			updateListeners.put(entityClass, listener);
		}
	}

	/**
	 * Add a delete listener for delete notifications for a specific entity.
	 * If a listener already exists for that entity, it is not added.
	 * 
	 * @param entityClass entity
	 * @param listener delete listener
	 */
	public void addDeleteListener(Class<?> entityClass, DeleteListener listener) {
		if (!deleteListeners.containsKey(entityClass)) {
			deleteListeners.put(entityClass, listener);
		}
	}

	/**
	 * Notify create listener for after-create and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 */
	public void notifyAfterCreate(Class<?> entityClass, int id) {
		final CreateListener l = createListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for after-create notification!", e);
			}
			l.afterCreate(model);
		}
	}
	
	/**
	 * Notify update listener for before-update and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 * @return true, if update should be aborted, otherwise false
	 */
	public boolean notifyBeforeUpdate(Class<?> entityClass, int id) {
		final UpdateListener l = updateListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for before-update notification!", e);
			}
			return l.beforeUpdate(model);
		}
		return false;
	}

	/**
	 * Notify update listener for after-update and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 */
	public void notifyAfterUpdate(Class<?> entityClass, int id) {
		final UpdateListener l = updateListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for after-update notification!", e);
			}
			l.afterUpdate(model);
		}
	}

	/**
	 * Notify delete listener for before-delete and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 * @return true, if deletion should be aborted, otherwise false
	 */
	public boolean notifyBeforeDelete(Class<?> entityClass, int id) {
		final DeleteListener l = deleteListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for before-delete notification!", e);
			}
			return l.beforeDelete(model);
		}
		return false;
	}
	
}
