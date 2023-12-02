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

import ch.autumo.beetroot.BeetRootConfigurationManager;

/**
 * ANSI Colors.
 * 
 * To activate ANSI colors in Windows:
 * - CMD and PowerShell: "reg add HKCU\Console /v VirtualTerminalLevel /t REG_DWORD /d 1"
 * - PowerShell: "Set-ItemProperty HKCU:\Console VirtualTerminalLevel -Type DWORD 1"
 * - You also could download the Windows Terminal: Windows Terminal
 * 
 * See also: https://stackoverflow.com/questions/51680709/colored-text-output-in-powershell-console-using-ansi-vt100-codes/51681675#51681675
 */
public final class Colors {

	/** show ANSI colors? */
	private static boolean showAnsiColors = false;
	static {
		showAnsiColors = BeetRootConfigurationManager.getInstance().getYesOrNo("console_colors");
	}
	
	/**
	 * Colorize text.
	 * 
	 * @param text text to colorize
	 * @param attribute color attribute
	 * @return
	 */
	public static String foregroundColorize(String text, Attribute attribute) {
		if (showAnsiColors)
			return Ansi.colorize(text, attribute);
		else 
			return text;
	}	
	/**
	 * Cyan colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String cyan(String text) {
		return foregroundColorize(text, Attribute.CYAN_TEXT());
	}

	/**
	 * Green colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String green(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_GREEN_TEXT());
	}

	/**
	 * Yellow colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String yellow(String text) {
		return foregroundColorize(text, Attribute.YELLOW_TEXT());
	}
	
	/**
	 * Orange colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String orange(String text) {
		return foregroundColorize(text, Attribute.TEXT_COLOR(255, 128, 0));
	}
	
	/**
	 * Red colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String red(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_RED_TEXT());
	}

	/**
	 * Dark-Red colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkRed(String text) {
		return foregroundColorize(text, Attribute.RED_TEXT());
	}
	
}
