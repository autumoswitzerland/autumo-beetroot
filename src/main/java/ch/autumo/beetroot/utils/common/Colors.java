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
package ch.autumo.beetroot.utils.common;

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
	 * @return colorized or uncolorized string
	 */
	public static String foregroundColorize(String text, Attribute attribute) {
		if (showAnsiColors)
			return Ansi.colorize(text, attribute);
		else 
			return text;
	}

	/**
	 * Black colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String black(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_BLACK_TEXT());
	}
	
	/**
	 * Dark-Black colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkBlack(String text) {
		return foregroundColorize(text, Attribute.BLACK_TEXT());
	}
	
	/**
	 * White colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String white(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_WHITE_TEXT());
	}
	
	/**
	 * Dark-White colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkWhite(String text) {
		return foregroundColorize(text, Attribute.WHITE_TEXT());
	}
	
	/**
	 * Mangenta colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String mangenta(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_MAGENTA_TEXT());
	}
	
	/**
	 * Dark-Mangenta colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkMangenta(String text) {
		return foregroundColorize(text, Attribute.MAGENTA_TEXT());
	}
	
	/**
	 * Cyan colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String cyan(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_CYAN_TEXT());
	}
	
	/**
	 * Dark-Cyan colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkCyan(String text) {
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
	 * Dark-Green colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkGreen(String text) {
		return foregroundColorize(text, Attribute.GREEN_TEXT());
	}

	/**
	 * Yellow colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String yellow(String text) {
		return foregroundColorize(text, Attribute.BRIGHT_YELLOW_TEXT());
	}
	
	/**
	 * Dark-Yellow colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkYellow(String text) {
		return foregroundColorize(text, Attribute.YELLOW_TEXT());
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
	
	/**
	 * Orange colored string.
	 * Unsupported by most terminals.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String orange(String text) {
		return foregroundColorize(text, Attribute.TEXT_COLOR(255, 128, 0));
	}
	
}
