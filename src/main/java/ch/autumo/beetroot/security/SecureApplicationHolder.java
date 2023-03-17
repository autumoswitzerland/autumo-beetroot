/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
	 * Get sec app holder.
	 * 
	 * @return sec app holder
	 */
	public static SecureApplicationHolder getInstance() {
		
		if (holder == null) {
			holder = new SecureApplicationHolder();
		}
		return holder;
	}

	/**
	 * Get Sec App.
	 */
	public SecureApplication getSecApp() {
		return SEC_APP;
	}

	/**
	 * Sec App.
	 */
	private static final class SecApp implements SecureApplication {
		@Override
		public String getUniqueSecurityKey() {
			return secKey;
		}
	}
	
}