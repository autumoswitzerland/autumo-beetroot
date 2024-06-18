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

import ch.autumo.beetroot.Model;


/**
 * Database update listener for entities.
 * Hook methods are only called from CRUD handlers. 
 */
public interface UpdateListener {

	/**
	 * Called before DB update.
	 * 
	 * @param bean the bean
	 * @return true, if update should be aborted, otherwise false
	 */
	public boolean beforeUpdate(Model bean);
	
	/**
	 * Called after DB update.
	 * 
	 * @param bean the updated bean
	 */
	public void afterUpdate(Model bean);
	
}
