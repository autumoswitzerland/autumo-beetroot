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

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * Provides a hash with Argon2id. 
 */
public class Argon2HashProvider implements PasswordHashProvider {

	private static final int DEFAULT_SALT_LENGTH = 16;
    private static final int DEFAULT_HASH_LENGTH = 32;
    private static final int DEFAULT_PARALLELISM = 2;
    private static final int DEFAULT_MEMORY = 1 << 16; // or 2^16, 65536k, 64M
    private static final int DEFAULT_ITERATIONS = 3;
    
    private Argon2PasswordEncoder encoder = null;
    
	public Argon2HashProvider() {
		encoder = new Argon2PasswordEncoder(DEFAULT_SALT_LENGTH, DEFAULT_HASH_LENGTH, DEFAULT_PARALLELISM, DEFAULT_MEMORY, DEFAULT_ITERATIONS);
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
