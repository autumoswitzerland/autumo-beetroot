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
package ch.autumo.beetroot;

import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;

/**
 * BeetRoot service for servlet context.
 */
public interface BeetRootService {

	/**
	 * Get a new temporary file manager.
	 * 
	 * @return new temporary file manager
	 */
	public ITempFileManager newTempFileManager();

	/**
	 * Cleanup method. Close what needs to be closed
	 * and free resources.
	 */
	public void destroy();

}
