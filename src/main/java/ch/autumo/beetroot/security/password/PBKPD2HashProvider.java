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

import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;

import ch.autumo.beetroot.security.SecureApplicationHolder;

/**
 * Provides a hash with PBKDF2 with HMAC and SHA256.
 */
public class PBKPD2HashProvider implements PasswordHashProvider {

	private static final int DEFAULT_SALT_LENGTH = 16;
    private static final int DEFAULT_ITERATIONS = 185000;
	private static final SecretKeyFactoryAlgorithm DEFAULT_ALGORITHM = SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256;

	
	private Pbkdf2PasswordEncoder encoder = null;
	
	public PBKPD2HashProvider() {
		encoder = new Pbkdf2PasswordEncoder(SecureApplicationHolder.getInstance().getSecApp().getUniqueSecurityKey(), DEFAULT_SALT_LENGTH, DEFAULT_ITERATIONS, DEFAULT_ALGORITHM);
	}

	@Override
	public String hash(String password) throws Exception {
		return encoder.encode(password);
	}

	@Override
	public boolean verify(String password, String hashedPassword) throws Exception {
		return encoder.matches(password, hashedPassword);
	}
	
}
