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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	private final Map<Class<?>, List<CreateListener>> createListeners = new HashMap<Class<?>, List<CreateListener>>();
	private final Map<Class<?>, List<UpdateListener>> updateListeners = new HashMap<Class<?>, List<UpdateListener>>();
	private final Map<Class<?>, List<DeleteListener>> deleteListeners = new HashMap<Class<?>, List<DeleteListener>>();
	

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
	 * 
	 * @param entityClass entity
	 * @param listener create listener
	 */
	public void addCreateListener(Class<?> entityClass, CreateListener listener) {
		List<CreateListener> l = createListeners.get(entityClass);
		if (l != null) {
			l.add(listener);
		} else {
			l = new ArrayList<CreateListener>();
			l.add(listener);
			createListeners.put(entityClass, l);
		}
	}
	
	/**
	 * Add an update listener for update notifications for a specific entity.
	 * 
	 * @param entityClass entity
	 * @param listener update listener
	 */
	public void addUpdateListener(Class<?> entityClass, UpdateListener listener) {
		List<UpdateListener> l = updateListeners.get(entityClass);
		if (l != null) {
			l.add(listener);
		} else {
			l = new ArrayList<UpdateListener>();
			l.add(listener);
			updateListeners.put(entityClass, l);
		}
	}

	/**
	 * Add a delete listener for delete notifications for a specific entity.
	 * 
	 * @param entityClass entity
	 * @param listener delete listener
	 */
	public void addDeleteListener(Class<?> entityClass, DeleteListener listener) {
		List<DeleteListener> l = deleteListeners.get(entityClass);
		if (l != null) {
			l.add(listener);
		} else {
			l = new ArrayList<DeleteListener>();
			l.add(listener);
			deleteListeners.put(entityClass, l);
		}
	}

	/**
	 * Notify create listeners for after-create and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 */
	public void notifyAfterCreate(Class<?> entityClass, int id) {
		final List<CreateListener> l = createListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
				for (CreateListener createListener : l) {
					createListener.afterCreate(model);
				}
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for after-create notification!", e);
			}
		}
	}
	
	/**
	 * Notify update listeners for before-update and specific entity.
	 * Every called listener can abort the update!
	 * 
	 * @param entityClass entity
	 * @param id id
	 * @return true, if update should be aborted, otherwise false
	 */
	public boolean notifyBeforeUpdate(Class<?> entityClass, int id) {
		final List<UpdateListener> l = updateListeners.get(entityClass);
		if (l != null) {
			boolean doNotUpdate = false;
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
				for (UpdateListener updateListener : l) {
					if (updateListener.beforeUpdate(model))
						doNotUpdate = true;
				}
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for before-update notification!", e);
			}
			return doNotUpdate;
		}
		return false;
	}

	/**
	 * Notify update listeners for after-update and specific entity.
	 * 
	 * @param entityClass entity
	 * @param id id
	 */
	public void notifyAfterUpdate(Class<?> entityClass, int id) {
		final List<UpdateListener> l = updateListeners.get(entityClass);
		if (l != null) {
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
				for (UpdateListener updateListener : l) {
					updateListener.afterUpdate(model);
				}
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for after-update notification!", e);
			}
		}
	}

	/**
	 * Notify delete listeners for before-delete and specific entity.
	 * Every called listener can abort the deletion!
	 * 
	 * @param entityClass entity
	 * @param id id
	 * @return true, if deletion should be aborted, otherwise false
	 */
	public boolean notifyBeforeDelete(Class<?> entityClass, int id) {
		final List<DeleteListener> l = deleteListeners.get(entityClass);
		if (l != null) {
			boolean doNotUpdate = false;
			Model model = null;
			try {
				model = DB.selectRecord(entityClass, id);
				for (DeleteListener deleteListener : l) {
					if (deleteListener.beforeDelete(model))
						doNotUpdate = true;
				}
			} catch (SQLException e) {
				LOG.error("Couldn't load bean from database for before-delete notification!", e);
			}
			return doNotUpdate;
		}
		return false;
	}
	
}
