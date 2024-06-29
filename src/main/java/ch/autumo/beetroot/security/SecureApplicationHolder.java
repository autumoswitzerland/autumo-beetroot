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
package ch.autumo.beetroot.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * Sec App Holder.
 */
public class SecureApplicationHolder {

	protected final static Logger LOG = LoggerFactory.getLogger(SecureApplicationHolder.class.getName());
	
	private static SecureApplicationHolder holder = null;
	private static final SecApp SEC_APP= new SecApp();
	
	private static String secKey = null; 
	
	private SecureApplicationHolder() {
		
		secKey = BeetRootConfigurationManager.getInstance().getString(Constants.SEC_KEY_SEED);
		
		if (secKey == null || secKey.length() == 0)
			throw new SecurityException("No security key seed has been defined! See configurations!");
	}

	/**
	 * Get Secure Application holder.
	 * 
	 * @return secure application holder
	 */
	public static SecureApplicationHolder getInstance() {
		
		if (holder == null) {
			holder = new SecureApplicationHolder();
		}
		return holder;
	}

	/**
	 * Get Secure Application.
	 * 
	 * @return secure application
	 */
	public SecureApplication getSecApp() {
		return SEC_APP;
	}

	/**
	 * Secure Application.
	 */
	private static final class SecApp implements SecureApplication {
		@Override
		public String getUniqueSecurityKey() {
			return secKey;
		}
	}
	
}
