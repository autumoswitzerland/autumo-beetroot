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
package ch.autumo.beetroot.utils.security;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.security.SecureApplication;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;

/**
 * Encoder for passwords.
 */
public class PWEncoder {
	
	/**
	 * Encoder.
	 * 
	 * @param args command line args
	 */
	private PWEncoder(String args[]) {
		
		if (args.length != 1) {

			System.out.println(Help.TEXT);
			Utils.invalidArgumentsExit();
		}
		
		if (args[0].equals("-help") || args[0].equals("-h")) {

			System.out.println(Help.TEXT);
			Utils.normalExit();
		}

		// Must !
		try {
			BeetRootConfigurationManager.getInstance().initialize();
		} catch (Exception e) {
			System.err.println("Couldn't initialize configuration 'cfg/beetroot.cfg' !");
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			Utils.fatalExit();
		}
		
		// get the app
		final SecureApplication app = SecureApplicationHolder.getInstance().getSecApp();

		System.out.println("");
				
		// value
		String data = args[0].trim();
		
		
		System.out.println("beetRoot Password Encoder Result:");
		System.out.println("---------------------------------");
		System.out.println("");
		
		String encoded = null;
		try {
				
			encoded = Utils.encode(data, app);
				
		} catch (UtilsException e) {
			System.err.println("Couldn't encode!");
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			Utils.fatalExit();
		}
		
		System.out.println(encoded);
		System.out.println("");
	}
	
	public static void main(String args[]) {
		
		new PWEncoder(args);
	}

    private static final class Help {
    	
    	public static final String TEXT =
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"beetRoot Password Encoder" 														+ Utils.LINE_SEPARATOR +
				"-------------------------"	 														+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Help:" 																			+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"In the beetRoot configuration file and database, passwords can be encoded." 		+ Utils.LINE_SEPARATOR +
				"This tool does the enconding. Keep the passwords in a password store, this" 		+ Utils.LINE_SEPARATOR +
				"tool doesn't decode encoded passwords!" 											+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"Usage:" 																			+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"  pwencoder.sh|.bat \"<password to encode>\""	 									+ Utils.LINE_SEPARATOR +
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
				"         pwencoder.sh \"mySecretPass\"" 											+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR +
				"" 																					+ Utils.LINE_SEPARATOR;
    }
    
}
