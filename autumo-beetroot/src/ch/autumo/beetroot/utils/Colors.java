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
	 * Cyan colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String cyan(String text) {
		if (showAnsiColors)
			return Ansi.colorize(text, Attribute.CYAN_TEXT());
		else 
			return text;
	}

	/**
	 * Green colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String green(String text) {
		if (showAnsiColors)
			return Ansi.colorize(text, Attribute.BRIGHT_GREEN_TEXT());
		else 
			return text;
	}

	/**
	 * Yellow colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String yellow(String text) {
		if (showAnsiColors)
			return Ansi.colorize(text, Attribute.YELLOW_TEXT());
		else 
			return text;
	}
	
	/**
	 * Red colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String red(String text) {
		if (showAnsiColors)
			return Ansi.colorize(text, Attribute.BRIGHT_RED_TEXT());
		else 
			return text;
	}

	/**
	 * Dark-Red colored string.
	 * 
	 * @param text text
	 * @return colored string
	 */
	public static String darkRed(String text) {
		if (showAnsiColors)
			return Ansi.colorize(text, Attribute.RED_TEXT());
		else 
			return text;
	}
	
}
