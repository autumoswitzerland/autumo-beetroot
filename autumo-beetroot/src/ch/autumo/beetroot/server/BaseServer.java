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
import java.io.Closeable;
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

import ch.autumo.beetroot.BeetRootWebServer;
import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.UtilsException;

/**
 * Base server.
 */
public abstract class BaseServer {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());
	
	/** Stop command */
	protected final static String STOP_COMMAND = "STOP";
	
	private static String rootPath = null;

    private AdminListener adminListener = null;
	private ServerSocket serverSocket = null;
	private int portAdminServer = -1;
	private boolean serverStop = false;
	
    private BeetRootWebServer webServer = null;
	protected boolean startWebServer = true;
	private int portWebServer = -1;

	private boolean pwEncoded = false;
	
	protected String name = null;
	
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
	private BaseServer(String name) {
		this.name = name;
	}
		
	/**
	 * Create a base server.
	 * 
	 * @param params start or stop
	 */
	public BaseServer(String params[], String name) {
		
		this(name);
		
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
			Utils.normalExit();
		}

		// check op
		if (!(params[0].equalsIgnoreCase("start") || params[0].equalsIgnoreCase("stop"))) {
			System.out.println(this.getHelpText());
			System.out.println("["+ name +"] Valid server operations are 'start' or 'stop'!");
			System.out.println("");
			Utils.normalExit();
		}
		
		//------------------------------------------------------------------------------
    	
    	if (rootPath == null || rootPath.length() == 0) {
			System.err.println("["+ name +"] ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.fatalExit();
    	}
	    	
		// check root path
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
	    
		final File dir = new File(rootPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("["+ name +"] ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Utils.fatalExit();
		}		
		
		//------------------------------------------------------------------------------
		
		// Read general config
		final ConfigurationManager configMan = ConfigurationManager.getInstance();
		// Must !
		try {
			configMan.initialize();
		} catch (Exception e) {
			System.err.println("["+ name +"] Configuration initialization failed !");
			e.printStackTrace();
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
				System.err.println("["+ name +"] Logging configuration initialization failed!");
				e.printStackTrace();
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
			
			if (portAdminServer == -1) {
				LOG.error("Admin server port not specified!");
				System.err.println("["+ name +"] Admin server port not specified!");
				Utils.fatalExit();
			}
		} catch (Exception e) {
			LOG.error("Admin server port has an invalid value: '" + v + "' !", e);
			System.err.println("["+ name +"] Admin server port has an invalid value: '" + v + "' !");
			Utils.fatalExit();
		}
		
		try {
			
			portWebServer = configMan.getInt(Constants.KEY_WS_PORT);
			
		} catch (Exception e) {
			LOG.error("Web server port has an invalid value: '" + v + "'!", e);
			System.err.println("["+ name +"] Web server port has an invalid value: '" + v + "'!");
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
			System.err.println("["+ name +"] Couldn't decrypt DB password!");
			Utils.fatalExit();
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			System.err.println("["+ name +"] Couldn't create DB manager!");
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
	 * Start server and web server if configured.
	 */
	protected void startServer() {
		
		LOG.info("Server starting...");
		if (LOG.isErrorEnabled())
			System.out.println("["+ name +"] Server starting...");
		
		// Start web server
		if (startWebServer) {
			try {
				
				LOG.info("Starting internal web server...");
				if (LOG.isErrorEnabled())
					System.out.println("["+ name +"] Starting internal web server...");
				
				try {
					Class.forName("javax.servlet.ServletOutputStream");
				} catch (ClassNotFoundException e1) {
					LOG.error("Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println("["+ name +"] Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println("["+ name +"] Shutting down!");
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
				
				final boolean https = ConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);
				if (https) {
					final String keystoreFile = ConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_FILE);
					
					final String keystorepw = pwEncoded ? 
							ConfigurationManager.getInstance().getDecodedString(Constants.KEY_WS_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
								ConfigurationManager.getInstance().getString(Constants.KEY_WS_KEYSTORE_PW);
					
					webServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(keystoreFile, keystorepw.toCharArray()), null);
				}
				
				webServer.start(false);
				
				if (https)
					LOG.info("HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
				else
					LOG.info("HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
					
				if (LOG.isErrorEnabled())
					System.out.println("["+ name +"] HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
				
			} catch (Exception e) {
	
				LOG.error("Cannot start web-server on port "+portWebServer+" - Shutting down!", e);
				System.err.println("["+ name +"] Cannot start web-server on port "+portWebServer+" - Shutting down!");
				Utils.fatalExit();
			}
		}

		this.beforeStart();
	
		// Admin listener
		adminListener = new AdminListener(portAdminServer);
		new Thread(adminListener).start();
		LOG.info("Admin listener started on port "+portAdminServer+".");
		if (LOG.isErrorEnabled())
			System.out.println("["+ name +"] Admin listener started on port "+portAdminServer+".");

		this.afterStart();
		
		LOG.info("Server started.");
		if (LOG.isErrorEnabled())
			System.out.println("["+ name +"] Server started.");
	}

	/**
	 * Stop server and web server if configured.
	 */
	protected void stopServer() {
		
		this.beforeStop();
		
		if (startWebServer) {
			
			LOG.info("Stopping internal web server...");
			if (LOG.isErrorEnabled())
				System.out.println("["+ name +"] Stopping internal web server...");

			webServer.stop();
			
			LOG.info("Internal web server stopped.");
			if (LOG.isErrorEnabled())
				System.out.println("["+ name +"] Internal web server stopped.");
		}

		this.afterStop();
		
		LOG.info(name + " server stopped.");
		if (LOG.isErrorEnabled())
			System.out.println("["+ name +"] Server stopped.");
	}
	
	private void sendStopServer() {
		try {
			sendServerCommand(new ServerCommand(name, "localhost", portAdminServer, STOP_COMMAND));
		} catch (Exception e) {
			LOG.error("Send STOP server command failed!", e);
		}
	}
	
	/**
	 * Send a server command client side.
	 * 
	 * @param command server command
	 * @return client answer
	 * @throws Excpetion
	 */
	public static ClientAnswer sendServerCommand(ServerCommand command) throws Exception {
		
		//send signal and end !
		Socket socket = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		try {
			socket = new Socket(command.getHost(), command.getPort());
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

			output.writeInt(command.getDataLength());
			final PrintWriter writer = new PrintWriter(output, true);
			writer.println(command.getTransferString());
			
			LOG.trace("Server command '"+command.getCommand()+"' sent!");
			writer.flush();
			
			if (command.getCommand().equals(STOP_COMMAND)) {
				// we cannot expect an answer, because the server is already down
				return new StopAnswer();
			} else {
				// read answer
				input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				return readAnswer(input);
			}
			
		} catch (UnknownHostException e) {
			
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! Host seems to be unknown or cannot be resolved. [UHE]", e);
			throw e;
			
		} catch (IOException e) {
			
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! PS: Is it really running? [IO]", e);
			throw e;
			
		} finally {
			
			safeClose(input);
			safeClose(output);
			safeClose(socket);
		}
	}
	
	/**
	 * Write/send client answer from server to client.
	 * 
	 * @param client asnwer
	 * @param out output stream
	 * @throws Excpetion
	 */
	private static void writeAnswer(ClientAnswer answer, DataOutputStream out) throws IOException {
		
		out.writeInt(answer.getDataLength());
		final PrintWriter writer = new PrintWriter(out, true);
		writer.println(answer.getTransferString());
		
		LOG.trace("Server command '"+answer.getAnswer()+"' sent!");
		writer.flush();			
	}

	/**
	 * Read an answer from the server client side.
	 * 
	 * @param in input stream
	 * @return client answer or null, if answer received was invalid
	 * @throws IOException
	 */
	public static ClientAnswer readAnswer(DataInputStream in) throws IOException {
	    return ClientAnswer.parse(read(in));
	}
	
	/**
	 * Read a server command server side.
	 * 
	 * @param in input stream
	 * @return server command or null, if command received was invalid
	 * @throws IOException
	 */
	private static ServerCommand readCommand(DataInputStream in) throws IOException {
	    return ServerCommand.parse(read(in));
	}
	
	private static String read(DataInputStream in) throws IOException {
		
		final int length = in.readInt();
		
		if (length > 256) {
			// prevent other requests, max length should not be longer than 128 bytes
			return null; 
		}
		
		final byte[] messageByte = new byte[length];
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
	 * Safe close for closeable object  (e.g. stream, socket).
	 * 
	 * @param closeable closeable object
	 */
    protected static final void safeClose(Object closeable) {
        try {
            if (closeable != null) {
                if (closeable instanceof Closeable) {
                    ((Closeable) closeable).close();
                } else if (closeable instanceof Socket) {
                    ((Socket) closeable).close();
                } else if (closeable instanceof ServerSocket) {
                    ((ServerSocket) closeable).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close!");
                }
            }
        } catch (IOException e) {
            LOG.error("Could not close stream or socket!", e);
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
		if (command.getCommand().equals("STOP")) {
			return new StopAnswer();
		}
		
		// standard answer = OK
		return new ClientAnswer();
	}

	/**
	 * Overwrite do do something before starting the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void beforeStart();

	/**
	 * Overwrite do do something after starting the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void afterStart();
	
	/**
	 * Overwrite do do something before stopping the server.
	 * Handle exceptions by your own!
	 */
	protected abstract void beforeStop();

	/**
	 * Overwrite do do something after stopping the server.
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
			
			try {
				serverSocket = new ServerSocket(this.listenerPort);
			} catch (IOException e) {
				LOG.error("Admin server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println("["+ name +"] Admin server listener cannot be created on port '" + this.listenerPort + "'!");
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
						threadForClient.start();
					}
		        } 
		        catch (IOException e) {
		        	
		        	if (!BaseServer.this.serverStop)
		        		LOG.error("Admin server listener failed! We recommend to restart the server!", e);
		        }
            } 
		
			// loop has been broken by STOP command.
        	safeClose(serverSocket);
			
			// shutdown server
			stopServer();
		}
	}	

	/**
	 * Client handler for every request.
	 */
	private final class ClientHandler implements Runnable {

		private Socket clientSocket = null;

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
			
			DataInputStream in = null;
			ServerCommand command = null;
			try {
			
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

				// server command from client received
				command = readCommand(in);
	        } 
	        catch (UtilsException e) {
	        	
				LOG.error("Admin server couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the client's secret key seed is wrong or the server's configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
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
	        	safeClose(in);
				return;
			}
			// 1. correct server name?
			final String serverName = command.getServerName();
			if (!serverName.equals(BaseServer.this.getServerName())) {
				LOG.error("Server command: Wrong server name received, command is ignored!");
	        	safeClose(in);
				return;
			}
			// 2. sec-key
			final String secKey = command.getSecKey();
			if (!secKey.equals(SecureApplicationHolder.getInstance().getSecApp().getUniqueSecurityKey())) {
				LOG.error("Server command: Wrong security key received, command is ignored!");
	        	safeClose(in);
				return;
			}
			
			// execute command
			final ClientAnswer answer = BaseServer.this.processServerCommand(command);
			
			// shutdown received?
			if (answer instanceof StopAnswer) {
				LOG.info("[STOP] signal received! Shutting down...");
				if (LOG.isErrorEnabled()) {
					System.out.println("["+ name +"] [STOP] signal received! Shutting down...");
				}
				
				// only escape of this loop
				BaseServer.this.serverStop = true;
				
	        	safeClose(in);
	        	safeClose(serverSocket);
	        	
				return;
			}
			
			// We have to answer -> get output-stream to client
			DataOutputStream out = null;
			try {

				 out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
				 writeAnswer(answer, out);
			
			} catch (IOException e) {
	        	
				LOG.error("Admin server client response failed! We recommend to restart the server!", e);
				System.err.println("["+ name +"] Admin server client response failed! We recommend to restart the server!");
				
	        } finally {
	        	
	        	safeClose(in);
	        	safeClose(out);
	        	safeClose(clientSocket);
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
				"---------------------" 																+ Utils.LINE_SEPARATOR +
    			"Usage:"																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Here's a detailed usage of the java-process, but you should use the server-script" 	+ Utils.LINE_SEPARATOR +
    			"  in the root-directory, which takes the argument 'start' or 'stop'."					+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    beetroot.sh start|stop"				 											+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Without script - the Java processes:" 												+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    java -DROOTPATH=\"<root-path>\" \\"												+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer start|stop"		+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      or" 																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    java -DROOTPATH=\"<root-path>\" \\"												+ Utils.LINE_SEPARATOR +
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
    			"    beetroot.sh -help" 																+ Utils.LINE_SEPARATOR +
    			"    beetroot.sh -h" 																	+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"";    	
	}
	
}
