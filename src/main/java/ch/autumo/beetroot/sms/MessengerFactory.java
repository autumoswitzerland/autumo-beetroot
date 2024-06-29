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
package ch.autumo.beetroot.sms;

import java.lang.reflect.Constructor;

import ch.autumo.beetroot.BeetRootConfigurationManager;

/**
 * SMS Messenger factory.
 */
public class MessengerFactory {

	private static Messenger messenger;
	
	/**
	 * Get configured SMS messenger implementation.
	 * 
	 * @return SMS messenger
	 * @throws Exception exception
	 */
	public static Messenger getInstance() throws Exception {
		
		if (messenger == null) {
			
			final String impl = BeetRootConfigurationManager.getInstance().getString("sms_implementation");
			if (impl == null || impl.length() == 0) {
				throw new RuntimeException("No SMS implementation defined!");
			}
			
	        final Constructor<?> constructor = Class.forName(impl).getDeclaredConstructor();
	        constructor.setAccessible(true);
	        messenger = (Messenger) constructor.newInstance();
	        messenger.init();
		}
		
		return messenger;
	}
	
}
