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
import ch.autumo.beetroot.utils.Security;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.UtilsException;

/**
 * Encoder for passwords.
 */
public class PWEncoder {
	
    /**
     * System-specific separator.
     */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	static {
		try {
			BeetRootConfigurationManager.getInstance().initialize();
		} catch (Exception e) {
			System.err.println(Colors.red("Couldn't initialize configuration 'cfg/beetroot.cfg' !"));
			System.err.println(Colors.red("ERROR") + ": " + e.getMessage());
			e.printStackTrace();
			Helper.fatalExit();
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
			Helper.normalExit();
		}
		
		if (args.length != 2) {
			System.out.println(Help.TEXT);
			Helper.invalidArgumentsExit();
		}

		// MODE
		String strMode = args[0].trim();
		int mode = -1;
		try {
			mode = Integer.valueOf(strMode).intValue();
		} catch (Exception e) {
			System.err.println(Colors.red("Mode '"+strMode+"' is invalid!"));
			Helper.fatalExit();
		}
		if (mode < 0 || mode > 1) {
			System.err.println(Colors.red("Mode '"+mode+"' is invalid!"));
			Helper.fatalExit();
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
				encoded = Security.hashPw(data);
			else
				encoded = Security.encode(data, app);
				
		} catch (UtilsException e) {
			System.err.println(Colors.red("Couldn't encode!"));
			System.err.println(Colors.red("Error")+": " + e.getMessage());
			e.printStackTrace();
			Helper.fatalExit();
		}
		
		System.out.println(Colors.green(encoded));
		System.out.println("");
	}
	
	public static void main(String args[]) {
		
		new PWEncoder(args);
	}

    private static final class Help {
		private static final String SHELL_EXT = SystemUtils.IS_OS_UNIX ? "sh" : "bat";
		private static final String USAGEA = Colors.darkYellow("pwencoder."+SHELL_EXT+" <mode> \"<password to encode>\"");
		private static final String USAGE0 = Colors.darkYellow("pwencoder.sh 1 \"mySecretPass\"");
    	public static final String TEXT =
				"" 																					+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				Colors.darkCyan("beetRoot Password Encoder")											+ LINE_SEPARATOR +
				"-------------------------"	 														+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"Help:" 																			+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"In the beetRoot configuration file and database, keys & passwords can be encoded." + LINE_SEPARATOR +
				"This tool does the enconding. Keep the passwords in a password store, this tool" 	+ LINE_SEPARATOR +
				"doesn't decode encoded passwords!" 												+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"Usage:" 																			+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"  " + USAGEA									 									+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"     mode  :  0  -->  For beetRoot user passwords only,"							+ LINE_SEPARATOR +
				"                      hashed by configured hash provider"							+ LINE_SEPARATOR +
				"     mode  :  1  -->  For configuration parameters only (beetroot.cfg)"			+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"Important notes:" 																	+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"Rules: - The character ' as well as the character \" cannot be used in the" 		+ LINE_SEPARATOR +
				"         in the passwords!"						 								+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"       - The <password/key> should be set within double quotation marks," 			+ LINE_SEPARATOR +
				"         e.g. \"mySecretPass\" when executing the encoder in the console," 		+ LINE_SEPARATOR +
				"         otherwise special characters may be removed and beetRoot will"			+ LINE_SEPARATOR +
				"         not be able to decode the correct values!" 								+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"         Example:" 																+ LINE_SEPARATOR +
				"         " + USAGE0					 											+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR +
				"" 																					+ LINE_SEPARATOR;
    }
    
}
