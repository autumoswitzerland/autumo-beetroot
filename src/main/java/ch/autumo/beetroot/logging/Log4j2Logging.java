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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;


/**
 * Log4j2 Logging initialization.
 */
public class Log4j2Logging extends AbstractLogging {

	private boolean initialized = false;
	
	@Override
	public void initialize(String path) throws IOException {
		this.initialize(path, "BeetRootConfig");
	}	

	@Override
	public void initialize(String path, String name) throws IOException {
		
		if (initialized)
			return;
		
		final File cf = new File(path);
		if (cf.exists()) {
			Configurator.initialize(name, null, cf.toURI());
		}
		else {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			ConfigurationSource cs = new ConfigurationSource(is);
			Configurator.initialize(Thread.currentThread().getContextClassLoader(), cs);
		}
		
		initialized = true;
	}	
	
}
