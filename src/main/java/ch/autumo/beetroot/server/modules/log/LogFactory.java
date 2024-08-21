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
package ch.autumo.beetroot.server.modules.log;

import javax.servlet.ServletContext;

import ch.autumo.beetroot.BeetRootConfigurationManager;


public class LogFactory {

	private static Log log;

	/**
	 * Private constructor.
	 */
	private LogFactory() {
	}
	
	/**
	 * Get log (local or remote).
	 * @return info
	 * @throws Exception
	 */
	public static synchronized Log getInstance() throws Exception {
		
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		if (log == null) {
			if (context == null)
				log = new LocalLog();
			else
				log = new RemoteLog();
		}
		
		return log;
	}	
	
}
