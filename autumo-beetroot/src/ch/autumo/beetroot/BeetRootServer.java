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
package ch.autumo.beetroot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * beetRoot stand-alone server.
 */
public class BeetRootServer {

    private static String rootPath = null;
    
    private AdminListener adminListener = null;
	private ServerSocket serverSocket = null;
    private BeetRootWebServer webServer = null;
    
	private int portAdminServer = -1;
	private int portWebServer = -1;
	private boolean startWebServer = true;
	
	private boolean pwEncoded = false;
	
	private String name = "beetRoot";
	
    
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootServer.class.getName());

	static {
    	
    	rootPath = System.getProperty("ROOTPATH");
    	
    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Utils.FILE_SEPARATOR;
    	
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
    }

	/**
	 * Create an ifaceX server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[], String name) {
		this(params);
		this.name = name;
	}
	
	/**
	 * Create an ifaceX server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[]) {
		
		//------------------------------------------------------------------------------
		
		// Scheme:
		//      beetroot.sh <operation>
		// E.g  beetroot.sh start|stop
		//------------------------------------------------------------------------------
    	
		if (params.length == 0 || (params[0].equals("-help") || params[0].equals("-h"))) {
			System.out.println(Help.TEXT);
			Utils.normalExit();
		}
		
		// check args length
		if (params.length < 1) {
			System.out.println(Help.TEXT);
			Utils.normalExit();
		}

		// check op
		if (!(params[0].equalsIgnoreCase("start") || params[0].equalsIgnoreCase("stop"))) {
			System.out.println(Help.TEXT);
			System.out.println("Valid server operations are 'start' or 'stop'!");
			System.out.println("");
			Utils.normalExit();
		}
		
		//------------------------------------------------------------------------------
    	
    	if (rootPath == null || rootPath.length() == 0) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.fatalExit();
    	}
	    	
		// check root path
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
	    
		final File dir = new File(rootPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.fatalExit();
		}		
		
		//------------------------------------------------------------------------------
		
		// Read general config
		final ConfigurationManager configMan = ConfigurationManager.getInstance();
		// Must !
		try {
			configMan.initialize();
		} catch (Exception e) {
			LOG.error("Configuration initialization failed !", e);
			Utils.fatalExit();
		}

		//------------------------------------------------------------------------------
		
		// configure logging
		String logCfgFile = System.getProperty("log4j2.configurationFile");
		// check if it has been overwritten with the 'log4j.configuration' parameter
		if (logCfgFile != null) {
			
			// --> log4j2.configurationFile=file:<log-cfg-path>/logging.cfg
			// --> In this case nothing to do, log4j2 reads the path!
		} else {
			logCfgFile = configMan.getString("ws_log_cfg");
			try {
				if (logCfgFile != null && logCfgFile.length() != 0)
					Utils.configureLog4j2(rootPath + logCfgFile);
				else
					Utils.configureLog4j2(rootPath + "cfg/logging.xml");
			} catch (Exception e) {
				LOG.error("Logging configuration initialization failed !", e);
				Utils.fatalExit();
			}
		}

		//------------------------------------------------------------------------------

		// Are pw's in config encoded?
		pwEncoded = configMan.getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
		
		// read config params
		String v = null;
		try {
			portAdminServer = configMan.getInt(Constants.KEY_ADMIN_PORT);
		} catch (Exception e) {
			LOG.error("Admin server port has an invalid value: '" + v + "' !", e);
			Utils.fatalExit();
		}
		try {
			portWebServer = configMan.getInt(Constants.KEY_WS_PORT);
		} catch (Exception e) {
			LOG.error("Web server port has an invalid value: '" + v + "' !", e);
			Utils.fatalExit();
		}
		
		startWebServer = configMan.getYesOrNo(Constants.KEY_WS_START);

		
		//------------------------------------------------------------------------------
		
		// DB conn manager
		try {
			DatabaseManager.getInstance().initialize(
					configMan.getString("db_url"),
					configMan.getString("db_user"),
					pwEncoded ? 
							configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password")
				);
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			Utils.fatalExit();
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			Utils.fatalExit();
		}

		
		//------------------------------------------------------------------------------
		
		// OPERATION
		final String operation = params[0];
		
		if (operation.equalsIgnoreCase("start")) {

			this.startServer();
		}
		else if (operation.equalsIgnoreCase("stop")) {
			
			this.sendStopServer();
		}
	}
	
	private void startServer() {
		
		LOG.info(name + " server starting...");
		
		// Start web server
		if (startWebServer) {
			try {
				
				webServer = new BeetRootWebServer(portWebServer);
				
				final boolean https = ConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);
				if (https) {
					final String keystoreFile = ConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_FILE);
					
					final String keystorepw = pwEncoded ? 
							ConfigurationManager.getInstance().getDecodedString(Constants.KEY_WS_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
								ConfigurationManager.getInstance().getString(Constants.KEY_WS_KEYSTORE_PW);
					
					webServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(keystoreFile, keystorepw.toCharArray()), null);
				}
				
				webServer.start(false);
				
				LOG.info("HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
				
			} catch (Exception e) {
	
				LOG.error("Cannot start web-server on port "+portWebServer+" - Shutting down!", e);
				Utils.fatalExit();
			}
		}

		this.beforeStart();
		
		// Admin listener
		adminListener = new AdminListener(portAdminServer);
		new Thread(adminListener).start();
		LOG.info("Admin listener started on port "+portAdminServer+".");
		
		LOG.info(name + " server started.");
	}

	private void stopServer() {
		
		this.beforeStop();
		
		try {
			serverSocket.close();
		} catch (IOException e) {
			LOG.warn("Coudln't properly close server socket. Ignored.");
		}
		
		if (startWebServer)
			webServer.stop();
		
		LOG.info(name + " server stopped.");
	}

	private void sendStopServer() {
		
		//send signal and end !
		Socket socket = null;
		try {
			socket = new Socket("localhost", portAdminServer);
			final DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			final String command = "STOP";
			byte[] dataInBytes = command.getBytes(StandardCharsets.UTF_8);
			output.writeInt(dataInBytes.length);
			final PrintWriter writer = new PrintWriter(output, true);
			writer.println(command);
			
			LOG.info("Server stop sent!");
			writer.flush();
			
		} catch (UnknownHostException e) {
			
			LOG.error("Local admin server cannot be contacted! Host seems to be unknown or cannot be resolved. [UHE]", e);
			Utils.fatalExit();
			
		} catch (IOException e) {
			
			LOG.error("Local admin server cannot be contacted!\nPS: Is it really running? [IO]", e);
			Utils.fatalExit();
			
		} finally {
			
			try {
				
				if (socket != null)
					socket.close();
				
			} catch (IOException e) {
				
				LOG.error("Client socket couldn't be closed!", e);
			}
		}
	}
	
	private String readCommand(DataInputStream in) throws IOException {
		
		final int length = in.readInt();
		byte[] messageByte = new byte[length];
	    boolean end = false;
	    final StringBuilder dataString = new StringBuilder(length);
	    int totalBytesRead = 0;
	    
	    while (!end) {
	    	
	        int currentBytesRead = in.read(messageByte);
	        totalBytesRead = currentBytesRead + totalBytesRead;
	        if(totalBytesRead <= length) {
	            dataString.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
	        } else {
	            dataString.append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead, StandardCharsets.UTF_8));
	        }
	        
	        if (dataString.length() >= length) {
	            end = true;
	        }
	    }
	    return dataString.toString();
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
	protected void beforeStop() {
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
	 * Admin server listener for operation signals.
	 */
	private final class AdminListener implements Runnable {
	
		private int listenerPort = -1;
		
		/**
		 * Create admin listener on specific port
		 * @param listenerPort listenr port
		 */
		public AdminListener(int listenerPort) {
			
			this.listenerPort = listenerPort;
			
			try {
				serverSocket = new ServerSocket(this.listenerPort);
			} catch (IOException e) {
				LOG.error("Admin server listener cannot be created on port '" + this.listenerPort + "' !", e);
				Utils.fatalExit();
			}
		}
		
		@Override
		public void run() {
			
			try {

				LOOP: while (true) {
					
					// it waits for a connection
					final Socket incomingConnection =  serverSocket.accept();
					final DataInputStream in = new DataInputStream(new BufferedInputStream(incomingConnection.getInputStream()));
					final String command = BeetRootServer.this.readCommand(in);
					
					switch (command) {
						case "STOP":
							LOG.info("[STOP] signal received! Shutting down...");
							
							break LOOP;
						default:
							break;
					}
	            } 
			
				BeetRootServer.this.stopServer();
	        } 
	        catch (IOException e) {
				LOG.error("Admin server listener failed! We recommend to restart the server!", e);
	        }
		}
	}
	
	/**
	 * Help class for shell script.
	 */
	protected static final class Help {
		
		public static final String TEXT =
				"" 																						+ Utils.LINE_SEPARATOR +
				"" 																						+ Utils.LINE_SEPARATOR +
				"beetRoot Server" + Constants.APP_VERSION 												+ Utils.LINE_SEPARATOR +
				"---------------" 																		+ Utils.LINE_SEPARATOR +
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

