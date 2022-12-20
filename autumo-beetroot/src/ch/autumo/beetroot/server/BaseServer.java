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
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootWebServer;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.logging.LoggingFactory;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.server.action.Download;
import ch.autumo.beetroot.server.action.Upload;
import ch.autumo.beetroot.server.communication.ClientCommunicator;
import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.HealthAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.message.StopAnswer;
import ch.autumo.beetroot.server.modules.Dispatcher;
import ch.autumo.beetroot.server.modules.FileStorage;
import ch.autumo.beetroot.transport.DefaultServerSocketFactory;
import ch.autumo.beetroot.transport.SecureServerSocketFactory;
import ch.autumo.beetroot.transport.ServerSocketFactory;
import ch.autumo.beetroot.utils.Colors;
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.security.SSLUtils;

/**
 * Base server.
 */
public abstract class BaseServer {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());
	
	private static String rootPath = null;

	private BeetRootConfigurationManager configMan = null;
	
	private Map<String, Dispatcher> dispatchers = new HashMap<>();
	
	protected ServerSocketFactory serverSocketFactory = null;
	
    private AdminListener adminListener = null;
	private ServerSocket serverSocket = null;

    private FileServer fileServer = null;
	protected boolean startFileServer = true;
	
	private FileStorage fileStorage = null;
	
	private int portAdminServer = -1;
	private boolean serverStop = false;
	
    private BeetRootWebServer webServer = null;
	protected boolean startWebServer = true;
	private int portWebServer = -1;

	private boolean pwEncoded = false;
	private boolean sslSockets = false;
	private boolean sha3Com = false;
	
	protected String name = null;
	
	private int serverTimeout = -1;
	
	private boolean hookShutdown = false;
	
	// colored text strings
	protected static String ansiServerName = null;
	protected static String ansiErrServerName = null;
	
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
		
		// Read general configuration
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
		
		//------------------------------------------------------------------------------
		// Scheme:
		//      beetroot.sh <operation>
		// E.g  beetroot.sh start|stop|health
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
		if (!(params[0].equalsIgnoreCase("start") || params[0].equalsIgnoreCase("stop") || params[0].equalsIgnoreCase("health"))) {
			System.out.println(this.getHelpText());
			System.out.println("Valid server operations are 'health', 'start' or 'stop'!");
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

		this.name = BeetRootConfigurationManager.getInstance().getString("server_name");
		ansiServerName = Colors.cyan("["+ name +"]");
		ansiErrServerName = Colors.red("["+ name +"]");
		
		
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

		// SSL sockets?
		final String mode = configMan.getString(Constants.KEY_ADMIN_COM_ENC);
		if (mode != null && mode.equalsIgnoreCase("ssl"))
			sslSockets = true;
		if (mode != null && mode.equalsIgnoreCase("sha3"))
			sha3Com = true;
		
		// Are pw's in config encoded?
		pwEncoded = configMan.getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
		
		// read config params
		String v = null;
		try {
			
			portAdminServer = configMan.getInt(Constants.KEY_ADMIN_PORT);
			
			if (portAdminServer == -1) {
				LOG.error("Admin server port not specified!");
				System.err.println(ansiErrServerName + " Admin server port not specified!");
				Utils.fatalExit();
			}
		} catch (Exception e) {
			LOG.error("Admin server port has an invalid value: '" + v + "' !", e);
			System.err.println(ansiErrServerName + " Admin server port has an invalid value: '" + v + "' !");
			Utils.fatalExit();
		}
		
		try {
			
			portWebServer = configMan.getInt(Constants.KEY_WS_PORT);
			
		} catch (Exception e) {
			LOG.error("Web server port has an invalid value: '" + v + "'!", e);
			System.err.println(ansiErrServerName + " Web server port has an invalid value: '" + v + "'!");
			Utils.fatalExit();
		}
		
		startWebServer = configMan.getYesOrNo(Constants.KEY_WS_START);

		
		//------------------------------------------------------------------------------
		
		// DB manager initialization if not yet done!
		try {
			
			final BeetRootDatabaseManager dbMan = BeetRootDatabaseManager.getInstance();
			if (!dbMan.isInitialized()) {

				/** this is an undocumented configuration key: it allows to use unsupported databases! */
				final String dbDriver = configMan.getStringNoWarn("db_driver");
				
				if (dbDriver != null && dbDriver.length() != 0) {
					
					// initialize unsupported DB
					BeetRootDatabaseManager.getInstance().initializeUnsupported(
							dbDriver,
							configMan.getString("db_url"),
							configMan.getString("db_user"),
							pwEncoded ? 
									configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password")
						);
					
				} else {
	
					// supported databases
					BeetRootDatabaseManager.getInstance().initialize(
							configMan.getString("db_url"),
							configMan.getString("db_user"),
							pwEncoded ? 
									configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password")
						);
				}
			}
			
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			System.err.println(ansiErrServerName + " Couldn't decrypt DB password!");
			Utils.fatalExit();
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			System.err.println(ansiErrServerName + " Couldn't create DB manager!");
			Utils.fatalExit();
		}

		
		//------------------------------------------------------------------------------
		
		// Server dispatcher initialization
		final String ds[] = configMan.getValues("dispatcher_");
		for (int i = 0; i < ds.length; i++) {
			Class<?> clazz;
			String currDisp = null;
			try {
				currDisp = ds[i];
				clazz = Class.forName(currDisp);
				final Constructor<?> constructor = clazz.getDeclaredConstructor();
	            constructor.setAccessible(true);
	            final Dispatcher d = (Dispatcher) constructor.newInstance();
	            // add dispatcher that handles server commands of distributed components/modules
	            dispatchers.put(d.getId(), d);
			} catch (Exception ex) {
				LOG.error("Cannot create server dispatcher '"+currDisp+"'! Stopping.", ex);
				System.err.println(ansiErrServerName + " Cannot create server dispatcher '"+currDisp+"'! Stopping.");
				Utils.fatalExit();
			}			
		}
		
		
		//------------------------------------------------------------------------------
		
		
		// OPERATION
		final String operation = params[0];
		
		if (operation.equalsIgnoreCase("health")) {
			
			this.sendServerCommand(Communicator.CMD_HEALTH);
			
		} else if (operation.equalsIgnoreCase("start")) {
			
			this.startServer();
			
			// SHUTDOWN HOOK (Ctrl-C)
			final Runtime runtime = Runtime.getRuntime();    
			runtime.addShutdownHook(this.getShutDownHook());    
			
		} else if (operation.equalsIgnoreCase("stop")) {
			
			this.sendServerCommand(Communicator.CMD_STOP);
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
			System.err.println(ansiErrServerName + " Logging configuration initialization failed!");
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
			System.out.println(ansiServerName + " Server starting...");
		
		// Start web server
		if (startWebServer) {
			try {
				
				LOG.info("Starting internal web server...");
				if (LOG.isErrorEnabled())
					System.out.println(ansiServerName + " Starting internal web server...");
				
				try {
					Class.forName("javax.servlet.ServletOutputStream");
				} catch (ClassNotFoundException e1) {
					LOG.error("Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println(ansiErrServerName + " Cannot start stand-alone web-server without Javax Servlet API! Check documentation for installing the Javax Servlet libs.");
					System.err.println(ansiErrServerName + " Shutting down!");
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
					webServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(SSLUtils.getKeystoreFile(), SSLUtils.getKeystorePw()), null);
					LOG.info("Web-Server communication is SSL (TLS) secured!");
					if (LOG.isErrorEnabled())
						System.out.println(ansiServerName + " Web-Server communication is SSL (TLS) secured!");
				}
				
				webServer.start(false);
				
				if (https)
					LOG.info("HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
				else
					LOG.info("HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
					
				if (LOG.isErrorEnabled())
					if (https)
						System.out.println(ansiServerName + " HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
					else
						System.out.println(ansiServerName + " HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");
				
			} catch (Exception e) {
	
				LOG.error("Cannot start web-server on port "+portWebServer+" - Already running? Stopping.", e);
				System.err.println(ansiErrServerName + " Cannot start web-server on port "+portWebServer+" - Already running? Stopping.");
				Utils.fatalExit();
			}
		}

		if (sslSockets) { // For C/S-communication
			try {
		        this.serverSocketFactory = new SecureServerSocketFactory(SSLUtils.makeSSLServerSocketFactory(SSLUtils.getKeystoreFile(), SSLUtils.getKeystorePw()), null);
				LOG.info("Client-Server communication (with file transfers) is SSL secured!");
				if (LOG.isErrorEnabled())
					System.out.println(ansiServerName + " Client-Server communication (with file transfers) is SSL secured!");
		        
			} catch (Exception e) {
				LOG.error("Cannot make server secure (SSL)! Stopping.", e);
				System.err.println(ansiErrServerName + " Cannot make server secure (SSL)! Stopping.");
				Utils.fatalExit();
			}
		} else {
	        this.serverSocketFactory = new DefaultServerSocketFactory();
		}
		
		if (sha3Com) {
			LOG.info("Client-Server communication is SHA3-256 encrypted!");
			if (LOG.isErrorEnabled())
				System.out.println(ansiServerName + " Client-Server communication is SHA3-256 encrypted!");
		}
		
		startFileServer = BeetRootConfigurationManager.getInstance().getYesOrNoNoWarn(Constants.KEY_ADMIN_FILE_SERVER);
		if (startFileServer) {
			
			// if we start the file server, we have to deliver a file storage
			// Without a file storage, the file server is not started!
			final String fileStorageClass = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_FILE_STORAGE);
			if (fileStorageClass != null && fileStorageClass.length() != 0) {
				Class<?> clazz;
				try {
					clazz = Class.forName(fileStorageClass);
					final Constructor<?> constructor = clazz.getDeclaredConstructor();
		            constructor.setAccessible(true);
		            fileStorage = (FileStorage) constructor.newInstance();
		            
				} catch (Exception e) {
					LOG.error("File server is not started, because configured file storage couldn't be created!");
					System.out.println(ansiErrServerName + " File server is not started, because configured file storage couldn't be created!");
					startFileServer = false;
				}
			} else {
				LOG.info("No file storage has been configured, try using internal methods!");
				if (LOG.isErrorEnabled())
					System.out.println(ansiServerName + " No file storage has been configured, try using internal methods!");
				startFileServer = true;
				fileStorage = null;
			}
			
            if (startFileServer) { // if it is still a GO...
				// File listener and server thread
				fileServer = new FileServer(this, fileStorage);
				fileServer.start();
				LOG.info("File server started. Ports: " + fileServer.portFileServer + ", " + fileServer.portFileReceiver + ".");
				if (LOG.isErrorEnabled())
					System.out.println(ansiServerName + " File server started. Ports: " + fileServer.portFileServer + ", " + fileServer.portFileReceiver + ".");
            }
		}
		
		// Admin listener and server thread
		adminListener = new AdminListener(portAdminServer);
		final Thread server = new Thread(adminListener);
		server.setName(this.name+"-Server");
		server.start();
		
		LOG.info("Admin listener started on port "+portAdminServer+".");
		if (LOG.isErrorEnabled())
			System.out.println(ansiServerName + " Admin listener started on port "+portAdminServer+".");

		this.afterStart();
		
		LOG.info("Server started.");
		if (LOG.isErrorEnabled())
			System.out.println(ansiServerName + " Server started.");
	}
	
	/**
	 * Stop server and web server if configured.
	 */
	protected void stopServer() {
		
		this.beforeStop();

		if (startFileServer) {
			
			LOG.info("Stopping internal file server...");
			if (LOG.isErrorEnabled())
				System.out.println(ansiServerName + " Stopping internal file server...");
			
			fileServer.stop();
			
			LOG.info("Internal file server stopped.");
			if (LOG.isErrorEnabled())
				System.out.println(ansiServerName + " Internal file server stopped.");
		}
		
		if (startWebServer) {
			
			LOG.info("Stopping internal web server...");
			if (LOG.isErrorEnabled())
				System.out.println(ansiServerName + " Stopping internal web server...");

			webServer.stop();
			
			LOG.info("Internal web server stopped.");
			if (LOG.isErrorEnabled())
				System.out.println(ansiServerName + " Internal web server stopped.");
		}

		this.afterStop();
		
		LOG.info(name + " server stopped.");
		if (LOG.isErrorEnabled())
			System.out.println(ansiServerName + " Server stopped.");
	}

	/**
	 * Internal delete method if no file-storage has been configured.
	 * -> Must be overwritten if this internal module is used.
	 * 
	 * @param uniqueFileId unique file ID
	 * @param domain domain or null
     * @return true if at least one (of all versions) has been found and deleted
	 * @throws Exception
	 */
	protected boolean delete(String uniqueFileId, String domain) throws Exception {
		throw new IllegalAccessError("Can't delete files, since no file-storage has been configured and neither an implementation (delete) has been provided!");
	}
	
	/**
	 * Internal find-file method if no file-storage has been configured.
	 * -> Must be overwritten if this internal module is used.
	 * 
	 * @param uniqueFileId unique file ID
	 * @param domain domain or null
	 * @return Download for the server to queue
	 * @throws Exception
	 */
	protected Download findFile(String uniqueFileId, String domain) throws Exception {
		throw new IllegalAccessError("Can't find files, since no file-storage has been configured and neither an implementation (findFile) has been provided!");
	}

	/**
	 * Internal file-store method if no file-storage has been configured.
	 * -> Must be overwritten if this internal module is used.
	 * 
	 * @param file file
	 * @param name file name
	 * @param user user or null
	 * @param domain domain or  null (default)
	 * @return unique file ID
	 * @throws Exception
	 */
	protected String store(File file, String name, String user, String domain) throws Exception {
		throw new IllegalAccessError("Can't store files, since no file-storage has been configured and neither an implementation (store) has been provided!");
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
						System.out.println(BaseServer.ansiServerName + " " + Colors.yellow("[CTRL-C]") + " signal received! Shutting down...");
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
	
    /**
     * Send a server command.
     * @param command server command
     */
	private void sendServerCommand(String command) {
		try {
			ClientCommunicator.sendServerCommand(new ServerCommand(ServerCommand.DISPATCHER_ID_INTERNAL, command));
		} catch (Exception e) {
			LOG.error("Send "+command+" server command failed!", e);
		}
	}
	
	/**
	 * This method is called when a server command has been received.
	 * At this point security checks have been made.
	 * 
	 * Usually mustn't be overwritten, because configured module/component
	 * dispatchers do the work beside internal commands. 
	 * 
	 * @param command received server command
	 * @return client answer
	 */
	protected ClientAnswer processServerCommand(ServerCommand command) {
		
		// --- 1. Internal commands without components/modules
		if (command.getDispatcherId().equals(ServerCommand.DISPATCHER_ID_INTERNAL)) {
			
			// shutdown
			if (command.getCommand().equals(Communicator.CMD_STOP)) {
				return new StopAnswer();
			}
			// health request
			if (command.getCommand().equals(Communicator.CMD_HEALTH)) {
				return new HealthAnswer();
			}
			// file request (for download)
			if (command.getCommand().equals(Communicator.CMD_FILE_REQUEST)) {
				
				if (startFileServer) {
					Download download = null;
					try {
						
						String domain = "default";
						if (command.getDomain() != null)
							domain = command.getDomain();
						
						if (fileStorage != null)
							download = fileStorage.findFile(command.getFileId(), domain);
						else
							download = this.findFile(command.getFileId(), domain);
						
					} catch (Exception e) {
						LOG.error("Find file request failed for file ID '"+command.getFileId()+"'!", e);
						System.err.println(BaseServer.ansiErrServerName + " Find file request failed for file ID '"+command.getFileId()+"'!");
						download = null;
					}
					
					if (download != null) { 
						fileServer.addToDownloadQueue(download);
						return new ClientAnswer(download.getFileName(), download.getFileId());
					} else {
						return new ClientAnswer("No file found with unique file ID '" + command.getFileId() + "'!", ClientAnswer.TYPE_FILE_NOK);
					}
				} else {
					LOG.error("A client requested a file, but file server is not running!");
					return new ClientAnswer("File server is not running!", ClientAnswer.TYPE_FILE_NOK);
				}			
			}
			// file receive request (for upload)
			if (command.getCommand().equals(Communicator.CMD_FILE_RECEIVE_REQUEST)) {
				
				if (startFileServer) {
					String user = null;
					if (command.getObject() != null)
						user = command.getObject().toString();
					
					String domain = "default";
					if (command.getDomain() != null)
						domain = command.getDomain();
					
					final Upload upload = new Upload(command.getId(), command.getEntity(), user, domain); 
					fileServer.addToUploadQueue(upload);
					return new ClientAnswer(command.getEntity(), "FILE", command.getId());
				}
			}
			// file delete
			if (command.getCommand().equals(Communicator.CMD_FILE_DELETE)) {
				
				boolean success = false;
				try {
					String domain = "default";
					if (command.getDomain() != null)
						domain = command.getDomain();
					
					if (fileStorage != null)
						success = fileStorage.delete(command.getFileId(), domain);
					else
						success = this.delete(command.getFileId(), domain);
				
				} catch (Exception e) {
					LOG.error("Delete file failed for file ID '"+command.getFileId()+"'!", e);
					System.err.println(BaseServer.ansiErrServerName + " Delete file failed for file ID '"+command.getFileId()+"'!");
					success = false;
				}
				
				if (success) 
					return new ClientAnswer("Success", command.getFileId());
				else
					return new ClientAnswer("Delete file failed for file ID '" + command.getFileId() + "'!", ClientAnswer.TYPE_FILE_NOK);
			}
		// --- 2. module/component dispatchers
		} else { 
			
			final String did = command.getDispatcherId();
			final Dispatcher dispatcher = dispatchers.get(did);
			if (dispatcher != null) {
				
				// dispatch server command
				return dispatcher.dispatch(command);
				
			} else {
				LOG.error("A client send a server command with invalid dispatcher ID '"+did+"'!");
			}
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
	 * Prints out the health status of this server.
	 * Overwrite if necessary.
	 */
	protected void printHealthStatus() {
		
		LOG.info("Server is running and healthy!");
		LOG.info("* Admin-Interface (Port: " + this.portAdminServer + "): Started");
		if (startFileServer)
			LOG.info("* File-Server (Port: " + fileServer.portFileServer + "): Started");
		if (startWebServer)
			LOG.info("* Web-Server (Port: " + this.portWebServer + "): Started");
		
		if (LOG.isErrorEnabled()) {
			System.out.println("");
			System.out.println("[" + this.name + "] Server is running and healthy!");
			System.out.println("[" + this.name + "] * Admin-Interface (Port: " + this.portAdminServer + "): Started");
			if (startFileServer)
				System.out.println("[" + this.name + "] * File-Server (Port: " + fileServer.portFileServer + "): Started");
			if (startWebServer)
				System.out.println("[" + this.name + "] * Web-Server (Port: " + this.portWebServer + "): Started");
		}
	}
	
	/**
	 * Admin server listener for operation signals.
	 */
	private final class AdminListener implements Runnable {
	
		private int listenerPort = -1;
		
		/**
		 * Create admin listener on specific port.
		 * 
		 * @param listenerPort listener port
		 */
		public AdminListener(int listenerPort) {
			
			this.listenerPort = listenerPort;
			
			// Communication is encrypted through the command message (cmd),
			// by SSL sockets (ssl) or it is not (none) 
			try {
				serverSocket = BaseServer.this.serverSocketFactory.create(this.listenerPort);
				if (serverTimeout > 0) // shouldn't be set, should be endless, just for testing purposes
					serverSocket.setSoTimeout(serverTimeout);
					
			} catch (IOException e) {
				LOG.error("Admin server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.ansiErrServerName + " Admin server listener cannot be created on port '" + this.listenerPort + "'!");
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
					
					String addr = null;
					if (clientSocket.getInetAddress() != null)
						addr = clientSocket.getInetAddress().toString();
					
					if (clientSocket != null) {
						final ClientHandler handler = new ClientHandler(clientSocket);
						final Thread threadForClient = new Thread(handler);
						if (addr == null)
							threadForClient.setName(BaseServer.this.name + "-Client");
						else
							threadForClient.setName(BaseServer.this.name + "-Client("+addr+")");
						threadForClient.start();
					}
					
		        } catch (IOException e) {
		        	
		        	if (!BaseServer.this.serverStop)
		        		LOG.error("Admin server connection listener failed! We recommend to restart the server!", e);
		        	
		        } finally {
		        	
		        	if (!BaseServer.this.serverStop) {
		        		if (serverSocket != null && serverSocket.isClosed()) {
		        			try {
		        				serverSocket = BaseServer.this.serverSocketFactory.create(this.listenerPort);
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
				LOG.error("  -> Either the secret key seed doesn't match on both sides ('msg' mode) or");
				LOG.error("     different encrypt modes have been defined on boths side, or the server's");
				LOG.error("     configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
				//LOG.error("  -> Exception: " + e);
				e.printStackTrace();
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
	        	Communicator.safeClose(clientSocket); //
				return;
			}
			// 1. correct server name?
			final String serverName = command.getServerName();
			if (!serverName.equals(BaseServer.this.getServerName())) {
				LOG.error("Server command: Wrong server name received, command is ignored!");
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket); //
				return;
			}
			
			// execute command
			final ClientAnswer answer = BaseServer.this.processServerCommand(command);
			
			// Health status request?
			if (answer instanceof HealthAnswer) {
				LOG.info("[HEALTH] signal received, printing server's health state to console.");

				// print info
				BaseServer.this.printHealthStatus();
				
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket); //
				return;
			}
			
			// shutdown received?
			if (answer instanceof StopAnswer) {
				LOG.info("[STOP] signal received! Shutting down...");
				if (LOG.isErrorEnabled()) {
					System.out.println("");
					System.out.println(BaseServer.ansiServerName + " " + Colors.darkRed("[STOP]") + " signal received! Shutting down...");
				}
				
				// only escape of this loop
				BaseServer.this.serverStop = true;
				
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket); //
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
				System.err.println(BaseServer.ansiErrServerName + " Admin server client response failed! We recommend to restart the server!");
	        } finally {
	        	Communicator.safeClose(out);
			}
			
        	Communicator.safeClose(in);
        	Communicator.safeClose(clientSocket);
		}
	}
	
	/**
	 * Help class for shell script.
	 */
	protected static final class Help {
		private static final String SHELL_EXT = SystemUtils.IS_OS_UNIX ? "sh" : "bat";
		private static final String TITLE = Colors.cyan("beetRoot Server");
		private static final String JAVA  = Colors.green("java");
		private static final String USAGE = Colors.yellow("beetroot."+SHELL_EXT+" start|stop|health");
		private static final String USAGE0 = Colors.yellow("beetroot."+SHELL_EXT+" -help");
		private static final String USAGE1 = Colors.yellow("beetroot."+SHELL_EXT+" -h");
		public static final String TEXT =
				"" 																						+ Utils.LINE_SEPARATOR +
				"" 																						+ Utils.LINE_SEPARATOR +
				TITLE									 												+ Utils.LINE_SEPARATOR +
				"---------------------" 																+ Utils.LINE_SEPARATOR +
    			"Usage:"																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Here's a detailed usage of the java-process, but you should use the server-script" 	+ Utils.LINE_SEPARATOR +
    			"  in the root-directory that accepts the commands 'health', 'start' or 'stop'."		+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    " + USAGE								 											+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"  Without script - the Java processes:" 												+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer <command>"		+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"      or" 																				+ Utils.LINE_SEPARATOR +
    			"" 																						+ Utils.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ Utils.LINE_SEPARATOR +
    			"         -Dlog4j.configuration=file:<log-cfg-path>/server-logging.cfg \\"		 		+ Utils.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer <command>"		+ Utils.LINE_SEPARATOR +
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
