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
package ch.autumo.beetroot.call;

import java.lang.reflect.Constructor;

import ch.autumo.beetroot.BeetRootConfigurationManager;


/**
 * Caller Messenger factory.
 */
public class CallerFactory {

	private static Caller caller;
	
	/**
	 * Private constructor.
	 */
	private CallerFactory() {
	}
	
	/**
	 * Get configured caller implementation.
	 * 
	 * @return caller
	 * @throws Exception exception
	 */
	public static synchronized Caller getInstance() throws Exception {
		
		if (caller == null) {
			
			final String impl = BeetRootConfigurationManager.getInstance().getString("call_implementation");
			if (impl == null || impl.length() == 0) {
				throw new RuntimeException("No caller implementation defined!");
			}
			
	        final Constructor<?> constructor = Class.forName(impl).getDeclaredConstructor();
	        constructor.setAccessible(true);
	        caller = (Caller) constructor.newInstance();
	        caller.init();
		}
		
		return caller;
	}
	
}
