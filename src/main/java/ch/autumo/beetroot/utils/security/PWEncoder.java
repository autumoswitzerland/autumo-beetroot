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
package ch.autumo.beetroot.utils.security;

import org.apache.commons.lang3.SystemUtils;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.security.SecureApplication;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Colors;
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;

/**
 * Encoder for passwords.
 */
public class PWEncoder {
	
	static {
		try {
			BeetRootConfigurationManager.getInstance().initialize();
		} catch (Exception e) {
			System.err.println(Colors.red("Couldn't initialize configuration 'cfg/beetroot.cfg' !"));
			System.err.println(Colors.red("ERROR") + ": " + e.getMessage());
			e.printStackTrace();
			Utils.fatalExit();
		}		
	}
	
	/**
	 * Encoder.
	 * 
	 * @param args command line args
	 */
	private PWEncoder(String args[]) {

		if (args.length > 0 && (args[0].equals("-help") || args[0].equals("-h"))) {
			System.out.println(Help.TEXT);
			Utils.normalExit();
		}
		
		if (args.length != 2) {
			System.out.println(Help.TEXT);
			Utils.invalidArgumentsExit();
		}

		// MODE
		String strMode = args[0].trim();
		int mode = -1;
		try {
			mode = Integer.valueOf(strMode).intValue();
		} catch (Exception e) {
			System.err.println(Colors.red("Mode '"+strMode+"' is invalid!"));
			Utils.fatalExit();
		}
		if (mode < 0 || mode > 1) {
			System.err.println(Colors.red("Mode '"+mode+"' is invalid!"));
			Utils.fatalExit();
		}
		
		// VALUE
		String data = args[1].trim();
		
		// get the app
		final SecureApplication app = SecureApplicationHolder.getInstance().getSecApp();

		System.out.println("");
				
		
		
		System.out.println("beetRoot Password Encoder Result:");
		System.out.println("---------------------------------");
		System.out.println("");
		
		String encoded = null;
		try {
				
			if (mode == 0)
				encoded = Utils.hashPw(data);
			else
				encoded = Utils.encode(data, app);
				
		} catch (UtilsException e) {
			System.err.println(Colors.red("Couldn't encode!"));
			System.err.println(Colors.red("Error")+": " + e.getMessage());
			e.printStackTrace();
			Utils.fatalExit();
		}
		
		System.out.println(Colors.green(encoded));
		System.out.println("");
	}
	
	public static void main(String args[]) {
		
		new PWEncoder(args);
	}

    private static final class Help {
		private static final String SHELL_EXT = SystemUtils.IS_OS_UNIX ? "sh" : "bat";
		private static final String USAGEA = Colors.yellow("pwencoder."+SHELL_EXT+" <mode> \"<password to encode>\"");
		private static final String USAGE0 = Colors.yellow("pwencoder.sh 1 \"mySecretPass\"");
    	public static final String TEXT =
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				Colors.cyan("beetRoot Password Encoder")											+ Utils.LINE_SEPARATOR +
				"-------------------------"	 														+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Help:" 																			+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"In the beetRoot configuration file and database, keys & passwords can be encoded." + Utils.LINE_SEPARATOR +
				"This tool does the enconding. Keep the passwords in a password store, this tool" 	+ Utils.LINE_SEPARATOR +
				"doesn't decode encoded passwords!" 												+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Usage:" 																			+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"  " + USAGEA									 									+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"     mode  :  0  -->  For beetRoot user passwords only,"							+ Utils.LINE_SEPARATOR +
				"                      hashed by configured hash provider"							+ Utils.LINE_SEPARATOR +
				"     mode  :  1  -->  For configuration parameters only (beetroot.cfg)"			+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Important notes:" 																	+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Rules: - The character ' as well as the character \" cannot be used in the" 		+ Utils.LINE_SEPARATOR +
				"         in the passwords!"						 								+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"       - The <password/key> should be set within double quotation marks," 			+ Utils.LINE_SEPARATOR +
				"         e.g. \"mySecretPass\" when executing the encoder in the console," 		+ Utils.LINE_SEPARATOR +
				"         otherwise special characters may be removed and beetRoot will"			+ Utils.LINE_SEPARATOR +
				"         not be able to decode the correct values!" 								+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"         Example:" 																+ Utils.LINE_SEPARATOR +
				"         " + USAGE0					 											+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR;
    }
    
}
