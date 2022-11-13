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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.lang3.SystemUtils;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootWebServer;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.UtilsException;
import ch.autumo.beetroot.logging.LoggingFactory;

/**
 * Base server.
 */
public abstract class BaseServer {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());
	
	private static String rootPath = null;

	private BeetRootConfigurationManager configMan = null;
	
    private AdminListener adminListener = null;
	private ServerSocket serverSocket = null;
	
	private int portAdminServer = -1;
	private boolean serverStop = false;
	
    private BeetRootWebServer webServer = null;
	protected boolean startWebServer = true;
	private int portWebServer = -1;

	private boolean pwEncoded = false;
	
	protected String name = null;
	
	private int serverTimeout = -1;
	
	private boolean hookShutdown = false;
	
	// colored text strings
	protected String ansiServerName = null;
	protected String ansiErrServerName = null;
	
	
	static {
    	
    	rootPath = System.getProperty("ROOTPATH");
    	
    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Utils.FILE_SEPARATOR;
    	
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
    }
		
	/**
	 * Create a base server.
	 * 
	 * @param params start or stop
	 */
	public BaseServer(String params[]) {
		
		//------------------------------------------------------------------------------
		// Scheme:
		//      beetroot.sh <operation>
		// E.g  beetroot.sh start|stop
		//------------------------------------------------------------------------------
    	
		if (params.length == 0 || (params[0].equals("-help") || params[0].equals("-h"))) {
			System.out.println(this.getHelpText());
			Utils.normalExit();
		}
		
		// check args length
		if (params.length < 1) {
			System.out.println(this.getHelpText());
			Utils.invalidArgumentsExit();
		}

		// check op
		if (!(params[0].equalsIgnoreCase("start") || params[0].equalsIgnoreCase("stop"))) {
			System.out.println(this.getHelpText());
			System.out.println("Valid server operations are 'start' or 'stop'!");
			System.out.println("");
			Utils.invalidArgumentsExit();
		}
		
		//------------------------------------------------------------------------------
    	
    	if (rootPath == null || rootPath.length() == 0) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.invalidArgumentsExit();
    	}
	    	
		// check root path
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
	    
		final File dir = new File(rootPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.invalidArgumentsExit();
		}		
		
		//------------------------------------------------------------------------------
		
		// Read general config
		configMan = BeetRootConfigurationManager.getInstance();
		// Must !
		try {
			// A sub-classed server might already have been initializing the configuration manager
			if (!configMan.isInitialized())
				configMan.initialize();
		} catch (Exception e) {
			System.err.println("Configuration initialization failed !");
			e.printStackTrace();
			Utils.fatalExit();
		}

		this.name = BeetRootConfigurationManager.getInstance().getString("server_name");
		this.ansiServerName = Utils.cyan("["+ name +"]");
		this.ansiErrServerName = Utils.red("["+ name +"]");
		
		
		//------------------------------------------------------------------------------
		
		// configure logging
		String logCfgFile = System.getProperty("log4j2.configurationFile");
		// check if it has been overwritten with the 'log4j.configuration' parameter
		if (logCfgFile == null)
			logCfgFile = configMan.getString("ws_log_cfg");
		
		this.initializeLogging(logCfgFile);


		//------------------------------------------------------------------------------

		// read some undocumented settings if available
		serverTimeout = configMan.getIntNoWarn("server_timeout"); // in ms !
		
		//------------------------------------------------------------------------------
	
		// Are pw's in config encoded?
		pwEncoded = configMan.getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
		
		// read config params
		String v = null;
		try {
			
			portAdminServer = configMan.getInt(Constants.KEY_ADMIN_PORT);
			
			if (portAdminServer == -1) {
				LOG.error("Admin server port not specified!");
				System.err.println(this.ansiErrServerName + " Admin server port not specified!");
				Utils.fatalExit();
			}
		} catch (Exception e) {
			LOG.error("Admin server port has an invalid value: '" + v + "' !", e);
			System.err.println(this.ansiErrServerName + " Admin server port has an invalid value: '" + v + "' !");
			Utils.fatalExit();
		}
		
		try {
			
			portWebServer = configMan.getInt(Constants.KEY_WS_PORT);
			
		} catch (Exception e) {
			LOG.error("Web server port has an invalid value: '" + v + "'!", e);
			System.err.println(this.ansiErrServerName + " Web server port has an invalid value: '" + v + "'!");
			Utils.fatalExit();
		}
		
		startWebServer = configMan.getYesOrNo(Constants.KEY_WS_START);

		
		//------------------------------------------------------------------------------
		
		// DB conn manager
		try {
			BeetRootDatabaseManager.getInstance().initialize(
					configMan.getString("db_url"),
					configMan.getString("db_user"),
					pwEncoded ? 
							configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password")
				);
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			System.err.println(this.ansiErrServerName + " Couldn't decrypt DB password!");
			Utils.fatalExit();
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			System.err.println(this.ansiErrServerName + " Couldn't create DB manager!");
			Utils.fatalExit();
		}

		
		//------------------------------------------------------------------------------
		
		
		// OPERATION
		final String operation = params[0];
		
		if (operation.equalsIgnoreCase("start")) {
			
			this.startServer();
			
			// SHUTDOWN HOOK (Ctrl-C)
			final Runtime runtime = Runtime.getRuntime();    
			runtime.addShutdownHook(this.getShutDownHook());    
			
		} else if (operation.equalsIgnoreCase("stop")) {
			
			this.sendStopServer();
		}		
	}
	
	/**
	 * Initialize logging. Can be overwritten.
	 * 
	 * @param logCfgFile logging config file
	 */
	protected void initializeLogging(String logCfgFile) {
		try {
			if (logCfgFile != null && logCfgFile.length() != 0)
				LoggingFactory.getInstance().initialize(rootPath + logCfgFile, "");
			else
				LoggingFactory.getInstance().initialize(rootPath + "cfg/logging.xml");
		} catch (Exception e) {
			System.err.println(this.ansiErrServerName + " Logging configuration initialization failed!");
			e.printStackTrace();
			Utils.fatalExit();
		}
	}

	/**
	 * Get argument help text.
	 * 
	 * @return argument help text
	 */
	public String getHelpText() {
		return Help.TEXT;
	}
		
	/**
	 * Get server name.
	 * 
	 * @return server name
	 */
	protected String getServerName() {
		return this.name;
	}
	
	/**
	 * Get root path of this server.
	 * 
	 * @return root path
	 */
	protected static String getRootPath() {
		return rootPath;
	}
	
	/**
	 * Start server and web server if configured.
	 */
	protected void startServer() {
		
		final boolean start = this.beforeStart();
		// start pre-condition ok? 
		if (!start)
			return;
		
		
		LOG.info("Server starting...");
		if (LOG.isErrorEnabled())
			System.out.println(this.ansiServerName + " Server starting...");
		
		// Start web server
		if (startWebServer) {
			try {
				
				LOG.info("Starting internal web server...");
				if (LOG.isErrorEnabled())
					System.out.println(this.ansiServerName + " Starting internal web server...");
				
				try {
					Class.forName("javax.servlet.ServletOutputStream");
				} catch (ClassNotFoundException e1) {
					LOG.error("Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println(this.ansiErrServerName + " Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println(this.ansiErrServerName + " Shutting down!");
					Utils.fatalExit();
				}
				
				try {
					Class.forName("jakarta.mail.Authenticator");
				} catch (ClassNotFoundException e1) {
					LOG.warn("NOTE: It seems you haven't installed Jakarta Mail; you'll not be able to reset your password!");
					LOG.warn("      Check documentation for installing the Jakarta Mail libs.");
					//System.out.println("["+ name +"] NOTE: It seems you haven't installed Jakarta Mail; you'll not be able to reset your password!");
					//System.out.println("["+ name +"]       Check documentation for installing the Jakarta Mail libs.");
				}				
				
				webServer = new BeetRootWebServer(portWebServer);
				
				final boolean https = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);
				if (https) {
					final String keystoreFile = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_FILE);
					
					final String keystorepw = pwEncoded ? 
							BeetRootConfigurationManager.getInstance().getDecodedString(Constants.KEY_WS_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
								BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_KEYSTORE_PW);
					
					webServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(keystoreFile, keystorepw.toCharArray()), null);
				}
				
				webServer.start(false);
				
				if (https)
					LOG.info("HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
				else
					LOG.info("HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
					
				if (LOG.isErrorEnabled())
					if (https)
						System.out.println(this.ansiServerName + " HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
					else
						System.out.println(this.ansiServerName + " HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
				
			} catch (Exception e) {
	
				LOG.error("Cannot start web-server on port "+portWebServer+" - Shutting down!", e);
				System.err.println(this.ansiErrServerName + " Cannot start web-server on port "+portWebServer+" - Shutting down!");
				Utils.fatalExit();
			}
		}

		
		// Admin listener and serevr thread
		adminListener = new AdminListener(portAdminServer);
		final Thread server = new Thread(adminListener);
		server.setName(this.name+"-Server");
		server.start();
		
		LOG.info("Admin listener started on port "+portAdminServer+".");
		if (LOG.isErrorEnabled())
			System.out.println(this.ansiServerName + " Admin listener started on port "+portAdminServer+".");

		this.afterStart();
		
		LOG.info("Server started.");
		if (LOG.isErrorEnabled())
			System.out.println(this.ansiServerName + " Server started.");
	}

	/**
	 * Stop server and web server if configured.
	 */
	protected void stopServer() {
		
		this.beforeStop();
		
		if (startWebServer) {
			
			LOG.info("Stopping internal web server...");
			if (LOG.isErrorEnabled())
				System.out.println(this.ansiServerName + " Stopping internal web server...");

			webServer.stop();
			
			LOG.info("Internal web server stopped.");
			if (LOG.isErrorEnabled())
				System.out.println(this.ansiServerName + " Internal web server stopped.");
		}

		this.afterStop();
		
		LOG.info(name + " server stopped.");
		if (LOG.isErrorEnabled())
			System.out.println(this.ansiServerName + " Server stopped.");
	}

	/**
	 * OS shutdown hook. Don't overwrite this, unless you know
	 * exactly what you are doing. This implementation calls
	 * all methods necessary to properly stop the server.
	 * 
	 * @return thread for runtime shutdown hook.
	 */
	protected Thread getShutDownHook() {
		return new Thread("ShutDownHook") {
			@Override
			public void run() {
				
				if (!BaseServer.this.serverStop) {
					
					hookShutdown = true;
					
					LOG.info("[CTRL-C] signal received! Shutting down...");
					if (LOG.isErrorEnabled()) {
						System.out.println("");
						System.out.println(BaseServer.this.ansiServerName + " " + Utils.yellow("[CTRL-C]") + " signal received! Shutting down...");
					}
					
					BaseServer.this.serverStop = true;
					
					//Communicator.safeClose(in);
					Communicator.safeClose(serverSocket);
					
					// shutdown server
					stopServer();
					
					//alternative: sendStopServer();
				}
			}
		};
	}
	
	private void sendStopServer() {
		try {
			Communicator.sendServerCommand(new ServerCommand(name, "localhost", portAdminServer, Communicator.STOP_COMMAND));
		} catch (Exception e) {
			LOG.error("Send STOP server command failed!", e);
		}
	}
    
	/**
	 * This method is called when a server command has been received.
	 * At this point security checks have been made.
	 * 
	 * @param command received server command
	 * @return client answer
	 */
	public ClientAnswer processServerCommand(ServerCommand command) {
		
		// shutdown
		if (command.getCommand().equals(Communicator.STOP_COMMAND)) {
			return new StopAnswer();
		}
		
		// standard answer = OK
		return new ClientAnswer();
	}

	/**
	 * Overwrite to do something before starting the server.
	 * Handle exceptions by your own!
	 * 
	 * @return true id server should be started,
	 * 		otherwise false; e.g. a pre-condition is not met
	 */
	protected abstract boolean beforeStart();

	/**
	 * Overwrite to do something after starting the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void afterStart();
	
	/**
	 * Overwrite to do something before stopping the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void beforeStop();

	/**
	 * Overwrite to do something after stopping the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void afterStop();

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
			
			//final ServerSocketFactory socketFactory = SSLServerSocketFactory.getDefault();
			// communication is encrypted through the app
			try {
				//serverSocket = socketFactory.createServerSocket(this.listenerPort);
				serverSocket = new ServerSocket(this.listenerPort);
				if (serverTimeout > 0) // shouldn't be set, should be endless, just for testing purposes
					serverSocket.setSoTimeout(serverTimeout);
					
			} catch (IOException e) {
				LOG.error("Admin server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.this.ansiErrServerName + " Admin server listener cannot be created on port '" + this.listenerPort + "'!");
				Utils.fatalExit();
			}
		}
		
		@Override
		public void run() {
			
			while (!BaseServer.this.serverStop) {
				
				Socket clientSocket = null;
				try {
					
					// it waits for a connection
					clientSocket = serverSocket.accept();
					if (clientSocket != null) {
						final ClientHandler handler = new ClientHandler(clientSocket);
						final Thread threadForClient = new Thread(handler);
						threadForClient.setName(BaseServer.this.name + "-Client");
						threadForClient.start();
					}
					
		        } catch (IOException e) {
		        	
		        	if (!BaseServer.this.serverStop)
		        		LOG.error("Admin server connection listener failed! We recommend to restart the server!", e);
		        	
		        } finally {
		        	
		        	if (!BaseServer.this.serverStop) {
		        		if (serverSocket != null && serverSocket.isClosed()) {
		        			try {
			    				serverSocket = new ServerSocket(this.listenerPort);
			    				if (serverTimeout > 0)
			    					serverSocket.setSoTimeout(serverTimeout);
		        			} catch (IOException e) {
		        				// That's wild, I know 
		        			}
		        		}
		        	}
	            }				
            } 
		
			if (!hookShutdown) {
				// loop has been broken by STOP command.
				Communicator.safeClose(serverSocket);
				
				// shutdown server
				stopServer();
			}
		}
	}	

	/**
	 * Client handler for every request.
	 */
	private final class ClientHandler implements Runnable {

		private Socket clientSocket = null;
		private DataInputStream in = null;

		/**
		 * Constructor.
		 * 
		 * @param clientSocket client socket
		 */
		private ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		@Override
		public void run() {
			
			ServerCommand command = null;
			try {
			
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

				// server command from client received
				command = Communicator.readCommand(in);
	        } 
	        catch (UtilsException e) {
	        	
				LOG.error("Admin server couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the secret key seed doesn't match on both sides or the server's configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
				//LOG.error("  -> Exception: " + e);
				return;
	        }	
	        catch (IOException e) {
	        	
				LOG.error("Admin server listener failed! We recommend to restart the server!", e);
				return;
	        }	
			
			// Security checks:
			// 0. invalid server command?
			if (command == null) {
				LOG.error("Server command: received command is too long, command is ignored!");
				Communicator.safeClose(in);
				return;
			}
			// 1. correct server name?
			final String serverName = command.getServerName();
			if (!serverName.equals(BaseServer.this.getServerName())) {
				LOG.error("Server command: Wrong server name received, command is ignored!");
				Communicator.safeClose(in);
				return;
			}
			// -> not a good idea
			/*
			// 2. sec-key
			final String secKey = command.getSecKey();
			if (!secKey.equals(SecureApplicationHolder.getInstance().getSecApp().getUniqueSecurityKey())) {
				LOG.error("Server command: Wrong security key received, command is ignored!");
				Communicator.safeClose(in);
				return;
			}
			*/
			
			// execute command
			final ClientAnswer answer = BaseServer.this.processServerCommand(command);
			
			// shutdown received?
			if (answer instanceof StopAnswer) {
				LOG.info("[STOP] signal received! Shutting down...");
				if (LOG.isErrorEnabled()) {
					System.out.println("");
					System.out.println(BaseServer.this.ansiServerName + " " + Utils.darkRed("[STOP]") + " signal received! Shutting down...");
				}
				
				// only escape of this loop
				BaseServer.this.serverStop = true;
				
				Communicator.safeClose(in);
				Communicator.safeClose(serverSocket);
	        	
				return;
			}
			
			// We have to answer -> get output-stream to client
			DataOutputStream out = null;
			try {

				 out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
				 Communicator.writeAnswer(answer, out);
			
			} catch (IOException e) {
	        	
				LOG.error("Admin server client response failed! We recommend to restart the server!", e);
				System.err.println(BaseServer.this.ansiErrServerName + " Admin server client response failed! We recommend to restart the server!");
				
	        } finally {
	        	
	        	Communicator.safeClose(in);
	        	Communicator.safeClose(out);
	        	Communicator.safeClose(clientSocket);
			}			
		}
		
	}

	
	
	/**
	 * Help class for shell script.
	 */
	protected static final class Help {
		private static final String SHELL_EXT = SystemUtils.IS_OS_UNIX ? "sh" : "bat";
		private static final String TITLE = Utils.cyan("beetRoot Server");
		private static final String JAVA  = Utils.green("java");
		private static final String USAGE = Utils.yellow("beetroot."+SHELL_EXT+" start|stop");
		private static final String USAGE0 = Utils.yellow("beetroot."+SHELL_EXT+" -help");
		private static final String USAGE1 = Utils.yellow("beetroot."+SHELL_EXT+" -h");
		public static final String TEXT =
				"" 																						+ Utils.LINE_SEPARATOR +
				"" 																						+ Utils.LINE_SEPARATOR +
				TITLE									 												+ Utils.LINE_SEPARATOR +
				"---------------------" 																+ Utils.LINE_SEPARATOR +
    			"Usage:"																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Here's a detailed usage of the java-process, but you should use the server-script" 	+ Utils.LINE_SEPARATOR +
    			"  in the root-directory, which takes the argument 'start' or 'stop'."					+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    " + USAGE								 											+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Without script - the Java processes:" 												+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer start|stop"		+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      or" 																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ Utils.LINE_SEPARATOR +
    			"         -Dlog4j.configuration=file:<log-cfg-path>/server-logging.cfg \\"		 		+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer start|stop"		+ Utils.LINE_SEPARATOR +
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
    			"    " + USAGE0					 														+ Utils.LINE_SEPARATOR +
    			"    " + USAGE1					 														+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"";    	
	}
	
}
