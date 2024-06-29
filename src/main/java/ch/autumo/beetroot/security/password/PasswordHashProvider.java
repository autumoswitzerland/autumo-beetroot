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
package ch.autumo.beetroot.security.password;

/**
 * Provides a irreversible hash for a password/key.
 */
public interface PasswordHashProvider {

	/**
	 * Create password hash.
	 * 
	 * @param password password
	 * @return hashed password
	 * @throws Exception exception
	 */
	public String hash(String password) throws Exception;
	
	/**
	 * Verify an entered password with a hashed password that has
	 * been previously hashed by {@link #hash(String)}.
	 * 
	 * @param password password
	 * @param hashedPassword hashed password
	 * @return true, if password matches hashed password
	 * @throws Exception exception
	 */
	public boolean verify(String password, String hashedPassword) throws Exception;
	
}
