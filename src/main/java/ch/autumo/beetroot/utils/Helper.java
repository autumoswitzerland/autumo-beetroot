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

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

/**
 * Helper methods.
 */
public class Helper {
	
	/** alpha-numeric HEX characters */
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	
	/**
	 * User home directory.
	 */
    public static final String USER_HOME = System.getProperty("user.home");
	
    /**
     * System-specific file separator.
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	/**
	 * Get Windows APPDATA directory.
	 */
    public static final String WIN_APPDATA_FOLDER = System.getenv("APPDATA" /*"LOCALAPPDATA"*/);

    
	/**
	 * Bytes 2 Hex.
	 * 
	 * @param bytes bytes
	 * @return Hex representation
	 */
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}	

	
    /**
     * Get properties path.
     * 
     * @param appName app name
     * @return properties path
     */
    public static String getDesktopPropertiesPath(String appName) {
        if (OS.isMac())
            return USER_HOME + FILE_SEPARATOR + "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR + "autumo" + FILE_SEPARATOR + appName + FILE_SEPARATOR;
        if (OS.isWindows())
            return WIN_APPDATA_FOLDER + FILE_SEPARATOR + "autumo" + FILE_SEPARATOR + appName + FILE_SEPARATOR;
        if (OS.isUnix())
            return USER_HOME + FILE_SEPARATOR + "." + appName + FILE_SEPARATOR;
        return USER_HOME + FILE_SEPARATOR;
    }

	/**
	 * Create proper display name for reference entities by removing 
	 * possible key prefixes or post-fixes.
	 * 
	 * @param displayName display name
	 * @return proper display name
	 */
	public static String adjustRefDisplayName(String displayName) {
		if (displayName.length() > 3) {
			if (displayName.endsWith(" Id") || displayName.endsWith(" Fk") || displayName.endsWith(" Pk"))
				return displayName.substring(0, displayName.length() - 3);
			else if (displayName.startsWith("Id ") || displayName.startsWith("Fk ") || displayName.startsWith("Pk "))
				return displayName.substring(3, displayName.length());
			else return displayName;
		} else {
			return displayName;
		}
	}
	
	/**
	 * Create banner; colorized or not.
	 *  
	 * @param banner banner text
	 * @return adjusted banner text
	 */
	public static String createBanner(String banner, Attribute colorAttribute) {
		boolean coloredBanner = true;
		if (OS.isWindows()) {
			int v = -1;
			String vstr = System.getProperty("os.version");
			try {
				v = Double.valueOf(vstr).intValue();
				if (v < 10)
					coloredBanner = false;
			} catch (Exception e) {
				coloredBanner = false;
			}
		}
		if (coloredBanner)
			banner = Ansi.colorize(banner, colorAttribute);
		return banner;
	}	

	
	// JVM exits.
	//------------------------------------------------------------------------------
	
    /**
     * Exit console program, due to unprocessable error.
     */
	public static void fatalExit() {
		exit(1);
	}
    /**
     * Exit console program, due to an error.
     */
	public static void errorExit() {
		exit(1);
	}
    /**
     * Exit because of invalid argument use.
     */
	public static void invalidArgumentsExit() {
		exit(128);
	}
    /**
     * Exit console program, due desired end.
     */
	public static void normalExit() {
		exit(0);
	}
    /**
     * Exit console program.
     */
	public static void exit(int code) {
		System.exit(code);
	}

}
