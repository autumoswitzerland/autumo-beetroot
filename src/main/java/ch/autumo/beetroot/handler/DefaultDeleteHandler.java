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
package ch.autumo.beetroot.handler;

import java.sql.SQLIntegrityConstraintViolationException;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.crud.EventHandler;
import ch.autumo.beetroot.utils.DB;

/**
 * Default delete handler.
 */
public class DefaultDeleteHandler extends BaseHandler {
	
	public DefaultDeleteHandler(String entity) {
		super(entity);
	}

	@Override
	public HandlerResponse deleteData(BeetRootHTTPSession session, int id) throws Exception {
		
		// Notify listeners
		if (EventHandler.getInstance().notifyBeforeDelete(getBeanClass(), id)) {
			// Abort?
			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.delete.abort", session.getUserSession(), getEntity(), id));
		}
		
		try {
			DB.delete(getEntity(), id);
		} catch (SQLIntegrityConstraintViolationException ex) {
			// In this case, the entity references another
			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.delete.integrity", session.getUserSession()));
		}
		return null;
	}
	
	@Override
	public String getResource() {
		return null;
	}

	/**
	 * Get bean entity class that has been generated trough PLANT, 
	 * self-written or null (then null in extract calls too).
	 * 
	 * Should be overwritten if you use before-delete notification!
	 * 
	 * @return bean entity class
	 */
	public Class<?> getBeanClass() {
		return null;
	}
	
}
