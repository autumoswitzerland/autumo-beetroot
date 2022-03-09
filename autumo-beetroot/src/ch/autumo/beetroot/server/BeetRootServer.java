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
package ch.autumo.beetroot.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Utils;

/**
 * beetRoot stand-alone server.
 */
public class BeetRootServer extends BaseServer {
	
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootServer.class.getName());

	/**
	 * Create an beetRoot server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[]) {
		super(params, "beetRoot");
	}
	
	/**
	 * Create an beetRoot server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[], String name) {

		super(params, name);
	}

	/**
	 * Overwrite do do something before starting the server.
	 * Handle exceptions by your own!
	 */
	protected void beforeStart() {
	}

	/**
	 * Overwrite do do something after starting the server.
	 * Handle exceptions by your own!
	 */
	protected void afterStart() {
	}
	
	/**
	 * Overwrite do do something before stopping the server.
	 * Handle exceptions by your own!
	 */
	protected void beforeStop() {
	}

	/**
	 * Overwrite do do something after stopping the server.
	 * Handle exceptions by your own!
	 */
	protected void afterStop() {
	}
	
	/**
	 * Create server and start it.
	 * 
	 * @param args only one: stop or start
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Go !
    	new BeetRootServer(args);
	}
	
	/**
	 * Help class for shell script.
	 */
	protected static final class Help {
		
		public static final String TEXT =
				"" 																						+ Utils.LINE_SEPARATOR +
				"" 																						+ Utils.LINE_SEPARATOR +
				"beetRoot Server" + Constants.APP_VERSION 												+ Utils.LINE_SEPARATOR +
				"---------------------" 																+ Utils.LINE_SEPARATOR +
    			"Usage:"																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Here's a detailed usage of the java-process, but you should use the server-script" 	+ Utils.LINE_SEPARATOR +
    			"  in the root-directory, which takes the argument 'start' or 'stop'."					+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    server.sh start|stop"				 												+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Without script - the Java processes:" 												+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    java -DROOTPATH=\"<root-path>\" \\"												+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.ifacex.Server start|stop"						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      or" 																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    java -DROOTPATH=\"<root-path>\" \\"												+ Utils.LINE_SEPARATOR +
    			"         -Dlog4j.configuration=file:<log-cfg-path>/server-logging.cfg \\"		 		+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.ifacex.Server start|stop"						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      <root-path>      :  Root directory where ifaceX is installed." 					+ Utils.LINE_SEPARATOR +
    			"                          Defined in run-script (Variable ROOT)."						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      <classpath>      :  The Java classpath."								 			+ Utils.LINE_SEPARATOR +
    			"                          Is build by the run-script."									+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  or" 																					+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    beetroot.sh -help" 																+ Utils.LINE_SEPARATOR +
    			"    beetroot.sh -h" 																	+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"";    	
	}

}

