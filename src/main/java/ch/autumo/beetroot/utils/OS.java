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
package ch.autumo.beetroot.utils;

import org.apache.commons.lang3.SystemUtils;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;


/**
 * OS helper methods.
 */
public class OS {
	
	/**
	 * OS.
	 */
	public static final String OS = System.getProperty("os.name").toLowerCase();
	
    /**
     * System-specific file separator.
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    
    /**
     * System-specific separator.
     */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	
	/**
	 * Is Windows?
	 * 
	 * @return true if so
	 */
	public static boolean isWindows() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	/**
	 * Is Mac?
	 * 
	 * @return true if so
	 */
	public static boolean isMac() {
		return SystemUtils.IS_OS_MAC;
	}

	/**
	 * Is Unix?
	 * 
	 * @return true if so
	 */
	public static boolean isUnix() {
		return SystemUtils.IS_OS_UNIX;
	}

	/**
	 * Is Solaris?
	 * 
	 * @return true if so
	 */
	public static boolean isSolaris() {
		return SystemUtils.IS_OS_SOLARIS;
	}
	
	/**
	 * Get operating system.
	 * 
	 * @return true if so
	 */
	public static String getOS() {
		return OS;	
	}
	
	/**
	 * Get temporary directory.
	 * 
	 * @return temporary directory
	 */
	public static String getTemporaryDirectory() {

		String dir = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_TMP_DIR);
		
		if (dir != null && dir.length() == 0)
			dir = System.getProperty("java.io.tmpdir");
		
		if (dir == null)
			dir = System.getProperty("java.io.tmpdir");
		
		if (!dir.endsWith(FILE_SEPARATOR))
			dir += FILE_SEPARATOR;
		
		return dir;
	}
	
}
