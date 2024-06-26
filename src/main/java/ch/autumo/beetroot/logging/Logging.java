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
package ch.autumo.beetroot.logging;

import java.io.IOException;

/**
 * Logging interface.
 */
public interface Logging {

	/**
	 * Configure the logging framework specifically with a log configuration file.
	 * - Configuration name used: 'BeetRootConfig'
	 * 
	 * @param path a path to the configuration
	 * @throws IOException IO exception
	 */
	public void initialize(String path) throws IOException;

	/**
	 * Configure the logging framework specifically with a log configuration file.
	 * 
	 * @param path a path to the configuration
	 * @param name configuration name
	 * @throws IOException IO exception
	 */
	public void initialize(String path, String name) throws IOException;
	
}
