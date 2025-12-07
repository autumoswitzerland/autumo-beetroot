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
package ch.autumo.beetroot.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootWebServer;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.logging.LogBuffer;
import ch.autumo.beetroot.logging.LogEventAppender;
import ch.autumo.beetroot.logging.LoggingFactory;
import ch.autumo.beetroot.server.action.Download;
import ch.autumo.beetroot.server.action.Upload;
import ch.autumo.beetroot.server.communication.ClientCommunicator;
import ch.autumo.beetroot.server.communication.ClientFileTransfer;
import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.HealthAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.message.StopAnswer;
import ch.autumo.beetroot.server.message.file.PingDownloadRequest;
import ch.autumo.beetroot.server.message.file.PingUploadRequest;
import ch.autumo.beetroot.server.modules.Dispatcher;
import ch.autumo.beetroot.server.modules.FileStorage;
import ch.autumo.beetroot.transport.DefaultServerSocketFactory;
import ch.autumo.beetroot.transport.SecureServerSocketFactory;
import ch.autumo.beetroot.transport.ServerSocketFactory;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.common.Colors;
import ch.autumo.beetroot.utils.security.SSL;
import ch.autumo.beetroot.utils.system.OS;
import ch.autumo.beetroot.utils.web.Web;

/**
 * Base server.
 */
public abstract class BaseServer {

	protected static final Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());


	private static String rootPath = null;

	private BeetRootConfigurationManager configMan = null;

	/** Modules/Dispatchers. */
	private Map<String, Dispatcher> dispatchers = new HashMap<>();

	/** Default or SSL. */
	protected ServerSocketFactory serverSocketFactory = null;
	/** Administration interface listener. */
	private AdminListener adminListener = null;
	/** Administration server socket. */
	private ServerSocket serverSocket = null;
	/** Administration executor service. */
	private ExecutorService clientExecutorService = Executors.newCachedThreadPool();

	/** Filer server. */
    private FileServer fileServer = null;
	protected boolean startFileServer = false;

	/** Filer storage interface. */
	private FileStorage fileStorage = null;

	private int portAdminServer = -1;
	private boolean serverStop = false;

    private BeetRootWebServer webServer = null;
	protected boolean startWebServer = true;
	private int portWebServer = -1;

	private boolean sslSockets = false;
	private boolean sha3Com = false;

	protected String name = null;

	private int serverTimeout = -1;

	// start time
	private long beetRootStart = 0;

	private boolean hookShutdown = false;

	// colored text strings
	protected static String ansiServerName = null;
	protected static String ansiErrServerName = null;

	// custom operations if any
	private final List<String> customOperations = new ArrayList<>();

	static {

    	rootPath = System.getProperty("ROOTPATH");

    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Helper.FILE_SEPARATOR;

    	if (!rootPath.endsWith(Helper.FILE_SEPARATOR))
    		rootPath += Helper.FILE_SEPARATOR;
    }

	/**
	 * Create a base server.
	 *
	 * @param params start or stop
	 */
	public BaseServer(String params[]) {

		// start stop-watch
		beetRootStart = System.currentTimeMillis();


    	// If not already defined
		if (System.getProperty("https.protocols") == null)
			System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");

		// Read general configuration
		configMan = BeetRootConfigurationManager.getInstance();
		// Must !
		try {
			// A sub-classed server might already have been initializing the configuration manager
			if (!BeetRootConfigurationManager.isInitialized())
				configMan.initialize();
		} catch (Exception e) {
			System.err.println("Configuration initialization failed !");
			e.printStackTrace();
			Helper.fatalExit();
		}

		//------------------------------------------------------------------------------
		// Scheme:
		//      beetroot.sh <operation>
		// E.g  beetroot.sh start|stop|health
		//------------------------------------------------------------------------------

		if (params.length == 0 || (params[0].equals("-help") || params[0].equals("-h"))) {
			System.out.println(this.getHelpText());
			Helper.normalExit();
		}

		// check args length
		if (params.length < 1) {
			System.out.println(this.getHelpText());
			Helper.invalidArgumentsExit();
		}


		// OPERATION
		final String operation = params[0].trim().toLowerCase();

		// Check custom operations
		final String cops[] = this.getValidCustomOperations();
		if (cops != null && cops.length > 0) {
			for (int i = 0; i < cops.length; i++)
				customOperations.add(cops[i].trim().toLowerCase());
		}

		// validate given operation with all possible operations (standard and custom operations)
		if (! (params[0].equals("start") || params[0].equals("stop") || params[0].equals("health") || customOperations.contains(params[0])) ) {

			System.out.println(this.getHelpText());
			System.out.println("Valid server operations are 'health', 'start' or 'stop'!");

			if (customOperations.size() > 0) {
				String strCops = "";
				for (Iterator<String> iterator = customOperations.iterator(); iterator.hasNext();) {
					String c = iterator.next();
					if (iterator.hasNext())
						strCops += "'"+c+"', ";
					else
						strCops += "'"+c+"'";
				}
				System.out.println("Valid custom operations are also "+strCops+".");
			}
			System.out.println("");
			Helper.invalidArgumentsExit();
		}

		//------------------------------------------------------------------------------

    	if (rootPath == null || rootPath.length() == 0) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Helper.invalidArgumentsExit();
    	}

		// check root path
    	if (!rootPath.endsWith(Helper.FILE_SEPARATOR))
    		rootPath += Helper.FILE_SEPARATOR;

		final File dir = new File(rootPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("ERROR: Specified '<root-path>' is invalid! See 'beetroot.sh -h'.");
			Helper.invalidArgumentsExit();
		}

		//------------------------------------------------------------------------------

		this.name = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_SERVER_NAME);
		ansiServerName = Colors.darkCyan("["+ name +"]");
		ansiErrServerName = Colors.red("["+ name +"]");

		Thread.currentThread().setName(this.name + "-MainThread");


		//------------------------------------------------------------------------------

		// configure logging
		String logCfgFile = System.getProperty("log4j2.configurationFile");
		// check if it has been overwritten with the 'log4j.configuration' parameter
		if (logCfgFile == null)
			logCfgFile = configMan.getString("ws_log_cfg");

		this.initializeLogging(logCfgFile);

		//------------------------------------------------------------------------------

		// Some infos
		if (operation.equalsIgnoreCase("start")) {
			LOG.info("Server starting...");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Server starting...");
		} else if (operation.equalsIgnoreCase("stop")) {
			LOG.info("Server stopping...");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Server stopping...");
		}

		//------------------------------------------------------------------------------

		// Flush the messages that have been collected before log-system initialization
		LogBuffer.flushToLogger(LOG);

		//------------------------------------------------------------------------------

		// DB manager initialization if not yet done!
		try {

			final BeetRootDatabaseManager dbMan = BeetRootDatabaseManager.getInstance();
			if (!dbMan.isInitialized()) {
				BeetRootDatabaseManager.getInstance().initialize();
			}
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			System.err.println(ansiErrServerName + " Couldn't decrypt DB password!");
			Helper.fatalExit();
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			System.err.println(ansiErrServerName + " Couldn't create DB manager!");
			Helper.fatalExit();
		}

		// Log H2 features if any
		if (BeetRootDatabaseManager.getInstance().isH2Db()) {
			final String h2Features = BeetRootDatabaseManager.getInstance().getH2Url().getFeatures();
			LOG.info("H2 features: '{}'.", h2Features);
		}


		//------------------------------------------------------------------------------

		// Initialize the log event appender
		LogEventAppender.initializeAppender();

		//------------------------------------------------------------------------------

		// read some undocumented settings if available
		serverTimeout = configMan.getIntNoWarn("server_timeout"); // in seconds !


		//------------------------------------------------------------------------------

		// SSL sockets?
		final String mode = configMan.getString(Constants.KEY_ADMIN_COM_ENC);
		if (mode != null && mode.equalsIgnoreCase("ssl"))
			sslSockets = true;
		if (mode != null && mode.equalsIgnoreCase("sha3"))
			sha3Com = true;

		// read config params
		String v = null;
		try {

			portAdminServer = configMan.getInt(Constants.KEY_ADMIN_PORT);

			if (portAdminServer == -1) {
				LOG.error("Admin server port not specified!");
				System.err.println(ansiErrServerName + " Admin server port not specified!");
				Helper.fatalExit();
			}
		} catch (Exception e) {
			LOG.error("Admin server port has an invalid value: '" + v + "' !", e);
			System.err.println(ansiErrServerName + " Admin server port has an invalid value: '" + v + "' !");
			Helper.fatalExit();
		}

		try {

			portWebServer = configMan.getInt(Constants.KEY_WS_PORT);

		} catch (Exception e) {
			LOG.error("Web server port has an invalid value: '" + v + "'!", e);
			System.err.println(ansiErrServerName + " Web server port has an invalid value: '" + v + "'!");
			Helper.fatalExit();
		}

		startWebServer = configMan.getYesOrNo(Constants.KEY_WS_START);


		//------------------------------------------------------------------------------


		// Evaluate operation

		if (operation.equalsIgnoreCase("health")) {

			this.sendServerCommand(Communicator.CMD_HEALTH);

		} else if (operation.equalsIgnoreCase("start")) {

			this.startServer();

			// SHUTDOWN HOOK (Ctrl-C)
			final Runtime runtime = Runtime.getRuntime();
			runtime.addShutdownHook(this.getShutDownHook());

		} else if (operation.equalsIgnoreCase("stop")) {

			this.sendServerCommand(Communicator.CMD_STOP);

			LOG.info("STOP command sent.");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " STOP command sent.");

		} else {
			// custom operation called, what to do?
			if (customOperations.size() > 0) { // if no custom ops, no call!
				this.customOperation(operation, params);
			}
		}
	}

	/**
	 * Initialize logging. Can be overwritten.
	 *
	 * @param logCfgFile logging config file
	 */
	protected void initializeLogging(String logCfgFile) {
		String path = rootPath + "cfg/logging.xml";
		if (logCfgFile != null && logCfgFile.length() != 0)
			path = rootPath + logCfgFile;
		try {
			LoggingFactory.getInstance().initialize(path, "BeetRootConfig");
		} catch (Exception e) {
			System.err.println(ansiErrServerName + " Logging configuration initialization failed!");
			e.printStackTrace();
			Helper.fatalExit();
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
	public String getServerName() {
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
		if (!start) {
			LOG.warn("Server stopped by before-start hook!");
			if (!LOG.isWarnEnabled())
				System.out.println(ansiServerName + " Server stopped by before-start hook!");
			return;
		}

		// Server dispatcher initialization
		this.initializeDispatchers();

		// Protocol if web server is used
		final boolean https = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);

		// Start web server
		if (startWebServer) {
			try {

				LOG.info("Starting internal web server...");
				if (!LOG.isInfoEnabled())
					System.out.println(ansiServerName + " Starting internal web server...");

				try {
					Class.forName("jakarta.servlet.ServletOutputStream");
				} catch (ClassNotFoundException e1) {
					LOG.error("Cannot start standalone web-server without Jakarta Servlet API! Check documentation for installing the Jakarta servlet libs.");
					System.err.println(ansiErrServerName + " Cannot start stand-alone web-server without Jakarta Servlet API! Check documentation for installing the Jakarta servlet libs.");
					System.err.println(ansiErrServerName + " Shutting down!");
					Helper.fatalExit();
				}

				try {
					Class.forName("jakarta.mail.Authenticator");
				} catch (ClassNotFoundException e1) {
					LOG.warn("NOTE: It seems you haven't installed Jakarta Mail; you'll not be able to reset your password!");
					LOG.warn("      Check documentation for installing the Jakarta Mail libs.");
				}

				webServer = new BeetRootWebServer(portWebServer);
				webServer.setBaseServer(this);

				if (https) {
					webServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(SSL.getKeystoreFile(), SSL.getKeystorePw()), null);
					LOG.info("Web-Server communication is SSL (TLS) secured!");
					if (!LOG.isInfoEnabled())
						System.out.println(ansiServerName + " Web-Server communication is SSL (TLS) secured!");
				}

				webServer.start(false);

				if (https)
					LOG.info("HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
				else
					LOG.info("HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");

				if (!LOG.isInfoEnabled())
					if (https)
						System.out.println(ansiServerName + " HTTP web-server started on port "+portWebServer+" (https://localhost:" + portWebServer +")");
					else
						System.out.println(ansiServerName + " HTTP web-server started on port "+portWebServer+" (http://localhost:" + portWebServer +")");

			} catch (Exception e) {

				LOG.error("Cannot start web-server on port "+portWebServer+" - Already running? Stopping.", e);
				System.err.println(ansiErrServerName + " Cannot start web-server on port "+portWebServer+" - Already running? Stopping.");
				Helper.fatalExit();
			}
		}

		if (sslSockets) { // For C/S-communication
			try {
		        this.serverSocketFactory = new SecureServerSocketFactory(SSL.makeSSLServerSocketFactory(), null);
				LOG.info("Client-Server communication (with file transfers) is SSL secured!");
				if (!LOG.isInfoEnabled())
					System.out.println(ansiServerName + " Client-Server communication (with file transfers) is SSL secured!");

			} catch (Exception e) {
				LOG.error("Cannot make server secure (SSL)! Stopping.", e);
				System.err.println(ansiErrServerName + " Cannot make server secure (SSL)! Stopping.");
				Helper.fatalExit();
			}
		} else {
	        this.serverSocketFactory = new DefaultServerSocketFactory();
		}

		final String comMode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_MODE, "sockets");
		if (comMode.equalsIgnoreCase("web")) {
			final String prot = https ? "HTTPS" : "HTTP";
			if (startWebServer) { // good
				LOG.info("Client-Server communication is tunneled through "+prot+".");
				if (!LOG.isInfoEnabled())
					System.out.println(ansiServerName + " Client-Server communication is tunneled through "+prot+".");
			} else {
				LOG.error("You have specified to tunnel Client-Server communication through "+prot+" (admin_com_mode=web),");
				LOG.error("but you don't start the web-server; this will NOT work!");
				System.err.println(ansiErrServerName + " You have specified to tunnel Client-Server communication through "+prot+" (admin_com_mode=web),");
				System.err.println(ansiErrServerName + " but you don't start the web-server; this will NOT work!");
			}
		}

		// SHA3 communication encryption?
		if (sha3Com) {
			LOG.info("Client-Server communication is SHA3-256 encrypted.");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Client-Server communication is SHA3-256 encrypted.");
		}

		// Start file server?
		startFileServer = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_FILE_SERVER, Constants.NO);
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
					LOG.error("File server is not started, because configured file storage couldn't be created!", e);
					System.out.println(ansiErrServerName + " File server is not started, because configured file storage couldn't be created!");
					startFileServer = false;
				}
			} else {
				LOG.info("No file storage has been configured, try using internal methods!");
				if (!LOG.isInfoEnabled())
					System.out.println(ansiServerName + " No file storage has been configured, try using internal methods!");
				startFileServer = true;
				fileStorage = null;
			}
            if (startFileServer) { // if it is still a GO...
				// File listener and server thread
				fileServer = new FileServer(this, fileStorage);
				fileServer.start();
				LOG.info("File server started. Ports: " + fileServer.portFileServer + ", " + fileServer.portFileReceiver + ".");
				if (!LOG.isInfoEnabled())
					System.out.println(ansiServerName + " File server started. Ports: " + fileServer.portFileServer + ", " + fileServer.portFileReceiver + ".");
            }
		}

		// Admin listener and server thread
		adminListener = new AdminListener(portAdminServer);
		final Thread server = new Thread(adminListener);
		server.setName(this.name+"-Server");
		server.start();

		LOG.info("Admin listener started on port "+portAdminServer+".");
		if (!LOG.isInfoEnabled())
			System.out.println(ansiServerName + " Admin listener started on port "+portAdminServer+".");

		this.afterStart();

		// Processing time
		final long beetRoot = System.currentTimeMillis();
		final long duration = beetRoot - beetRootStart;
		final String startup = OS.getReadableDuration(duration, TimeUnit.SECONDS);

		LOG.info("Server started - startup time: " + startup + ".");
		if (!LOG.isInfoEnabled())
			System.out.println(ansiServerName + " Server started - startup time: " + startup + ".");
	}

	/**
	 * Stop server and web server if configured.
	 */
	protected void stopServer() {
		this.beforeStop();
		if (startFileServer) {
			LOG.info("Stopping internal file server...");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Stopping internal file server...");
			fileServer.stop();
			LOG.info("Internal file server stopped.");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Internal file server stopped.");
		}
		if (startWebServer) {
			LOG.info("Stopping internal web server...");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Stopping internal web server...");
			webServer.stop();
			LOG.info("Internal web server stopped.");
			if (!LOG.isInfoEnabled())
				System.out.println(ansiServerName + " Internal web server stopped.");
		}
		// Release database resources
		try {
			BeetRootDatabaseManager.getInstance().release();
		} catch (Exception e) {
			LOG.error("Couldn't release database manager!", e);
		}
		this.afterStop();
		LOG.info(name + " server stopped.");
		if (!LOG.isInfoEnabled())
			System.out.println(ansiServerName + " Server stopped.");
	}

	/**
	 * Internal delete method if no file-storage has been configured.
	 * -&gt; Must be overwritten if this internal module is used.
	 *
	 * @param uniqueFileId unique file ID
	 * @param domain domain or null
     * @return true if at least one (of all versions) has been found and deleted
	 * @throws Exception exception
	 */
	protected boolean delete(String uniqueFileId, String domain) throws Exception {
		throw new IllegalAccessError("Can't delete files, since no file-storage has been configured and neither an implementation (delete) has been provided!");
	}

	/**
	 * Internal find-file method if no file-storage has been configured.
	 * -&gt; Must be overwritten if this internal module is used.
	 *
	 * @param uniqueFileId unique file ID
	 * @param domain domain or null
	 * @return Download for the server to queue
	 * @throws Exception exception
	 */
	protected Download findFile(String uniqueFileId, String domain) throws Exception {
		throw new IllegalAccessError("Can't find files, since no file-storage has been configured and neither an implementation (findFile) has been provided!");
	}

	/**
	 * Internal file-store method if no file-storage has been configured.
	 * -&gt; Must be overwritten if this internal module is used.
	 *
	 * @param file file
	 * @param name file name
	 * @param user user or null
	 * @param domain domain or  null (default)
	 * @return unique file ID
	 * @throws Exception exception
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
					if (!LOG.isInfoEnabled()) {
						System.out.println("");
						System.out.println(BaseServer.ansiServerName + " " + Colors.darkYellow("[CTRL-C]") + " signal received! Shutting down...");
					}
					BaseServer.this.serverStop = true;
					Communicator.safeClose(serverSocket);
					// shutdown server
					stopServer(); // alternative: sendStopServer();
					// shutdown thread pool
					shutDownExecutorService();
				}
			}
		};
	}

	/**
	 * Shutdown thread pool.
	 */
	private void shutDownExecutorService() {
		clientExecutorService.shutdown();
	    try {
	        // Wait for tasks to complete for up to 60 seconds
	        if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
	            clientExecutorService.shutdownNow(); // Force shutdown if timeout occurs
	            if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
	                LOG.error("Client executor service did not terminate.");
	            }
	        }
	    } catch (InterruptedException ie) {
	        // If interrupted, force shutdown immediately
	        clientExecutorService.shutdownNow();
	        Thread.currentThread().interrupt(); // Restore interrupt status
	    }
	}

	/**
	 * Initialize dispacther-modules.
	 */
	private void initializeDispatchers() {
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
	            LOG.info("Module: Dispatcher '"+currDisp+"' added.");
	            if (!LOG.isInfoEnabled())
		            System.out.println(ansiServerName + " Module: Dispatcher '"+currDisp+"' added.");
			} catch (Exception ex) {
				LOG.error("Cannot create server dispatcher '"+currDisp+"'! Stopping.", ex);
				System.err.println(ansiErrServerName + " Cannot create server dispatcher '"+currDisp+"'! Stopping.");
				Helper.fatalExit();
			}
		}
	}

    /**
     * You can send internal server commands to be send to the running server!
     * Internal server commands are always send over sockets.
     *
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
	 * You can send local server commands to be send to the running server
	 * for possible custom operations. You might want to force them over sockets,
	 * see {@link ServerCommand#forceSockets()}.
	 *
	 * @param command server command
	 */
	protected void sendServerCommand(ServerCommand command) {
		try {
			ClientCommunicator.sendServerCommand(command);
		} catch (Exception e) {
			LOG.error("Send "+command.getCommand()+" server command failed!", e);
		}
	}

	/**
	 * Return a list of valid custom operations
	 *
	 * @return custom operations possible
	 */
	protected String[] getValidCustomOperations() {
		return null;
	}

	/**
	 * Custom operation has been defined when starting the server,
	 * we possibly want to do something else. Overwrite if needed.
	 * Of you overwrite, you have to implement
	 * {@link #customOperation(String, String[])}!
	 *
	 * @param operation the operation = params[0], 1st argument
	 * 			of all program arguments
	 * @param params all program arguments
	 */
	protected void customOperation(String operation, String params[]) {
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
	public ClientAnswer processServerCommand(ServerCommand command) {

		// --- 1. Internal commands without components/modules
		if (command.getDispatcherId().equals(ServerCommand.DISPATCHER_ID_INTERNAL)) {

			// Shutdown
			if (command.getCommand().equals(Communicator.CMD_STOP)) {
				return new StopAnswer();
			}
			// Health request
			if (command.getCommand().equals(Communicator.CMD_HEALTH)) {
				return new HealthAnswer();
			}
			// File request (for download)
			if (command.getCommand().equals(Communicator.CMD_FILE_REQUEST)) {

				if (startFileServer) {

					final String fileId = command.getFileId();
					Download download = null;

					String domain = "default";
					if (command.getDomain() != null)
						domain = command.getDomain();

					if (fileId.equals(PingDownloadRequest.PING_FILE_ID)) {

						try {
							final Path path = OS.createTemporaryFile(PingDownloadRequest.PING_FILE_PREFIX);
							download = new Download(fileId, path.getFileName().toString(), path.toFile(), domain);
						} catch (Exception e) {
							LOG.error("Ping file request failed for file ID '"+fileId+"'!", e);
							System.err.println(BaseServer.ansiErrServerName + " Find file request failed for file ID '"+fileId+"'!");
							download = null;
						}

					} else { // Default case, really find a file!

						try {
							if (fileStorage != null)
								download = fileStorage.findFile(fileId, domain);
							else
								download = this.findFile(fileId, domain);

						} catch (Exception e) {
							LOG.error("Find file request failed for file ID '"+fileId+"'!", e);
							System.err.println(BaseServer.ansiErrServerName + " Find file request failed for file ID '"+fileId+"'!");
							download = null;
						}
					}

					if (download != null) {
						fileServer.addToDownloadQueue(download);
						return new ClientAnswer(download.getFileName(), download.getFileId());
					} else {
						return new ClientAnswer("No file found with unique file ID '" + fileId + "'!", ClientAnswer.TYPE_FILE_NOK);
					}

				} else {
					LOG.error("A client requested a file, but file server is not running!");
					return new ClientAnswer("File server is not running!", ClientAnswer.TYPE_FILE_NOK);
				}
			}
			// File receive request (for upload)
			if (command.getCommand().equals(Communicator.CMD_FILE_RECEIVE_REQUEST)) {
				if (startFileServer) {
					String user = null;
					if (command.getObject() != null)
						user = command.getObject().toString();

					String domain = "default";
					if (command.getDomain() != null)
						domain = command.getDomain();

					final String fileNameCheckSum[] = command.getEntity().split(Upload.ENTITY_DIVIDER_FILENAME_CHECKSUM);
					final String fileName = fileNameCheckSum[0];
					final String checkSum = fileNameCheckSum[1];
					final Upload upload = new Upload(command.getId(), fileName, checkSum, user, domain);
					fileServer.addToUploadQueue(upload);
					return new ClientAnswer(command.getEntity(), "FILE", command.getId());
				}
			}
			// File delete
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
		// --- 2. Module/component dispatchers
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
	 *
	 * @param hasNoIssues any pre-existing issues for the overall state;
	 * 			false if there are issues!
	 * @return overall state, true if good
	 */
	protected boolean printHealthStatus(boolean hasNoIssues) {
		// If the call made it here, the admin-port is listening!
		boolean isAdminPortListening = true;
		hasNoIssues = hasNoIssues && isAdminPortListening;
		boolean isFileDownloadPortListening = false;
		boolean isFileUploadPortListening = false;
		boolean isWebServerPortListening = false;
		if (startFileServer) {
			ClientAnswer answer = null;
			// File server (Download)
			final PingDownloadRequest pingDownloadRequest = new PingDownloadRequest();
			try {
				answer = ClientCommunicator.sendServerCommand(pingDownloadRequest);
			} catch (Exception e) {
			}
			try {
				final File file = ClientFileTransfer.getFile(pingDownloadRequest.getFileId(), PingDownloadRequest.PING_FILE_PREFIX + "del.tmp");
				isFileDownloadPortListening = file != null && file.exists();
				file.delete();
			} catch (Exception e) {
				isFileDownloadPortListening = false;
			}
			// File server (Upload)
			PingUploadRequest pingUploadRequest = null;
			try {
				pingUploadRequest = new PingUploadRequest();
				answer = ClientCommunicator.sendServerCommand(pingUploadRequest);
			} catch (Exception e) {
			}
			try {
				answer = ClientFileTransfer.sendFile(pingUploadRequest.getFile());
				isFileUploadPortListening = answer != null && answer.getType() > 0;
			} catch (Exception e) {
				isFileUploadPortListening = false;
			}
		} else {
			isFileDownloadPortListening = true;
			isFileUploadPortListening = true;
		}
		hasNoIssues = hasNoIssues && isFileDownloadPortListening;
		hasNoIssues = hasNoIssues && isFileUploadPortListening;

		if (startWebServer) {
			final boolean https = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);
			final String url = ((https) ? "https" : "http") + "://localhost:" + this.portWebServer;
			isWebServerPortListening = Web.pingWebsite(url);
		} else {
			isWebServerPortListening = true;
		}

		hasNoIssues = hasNoIssues && isWebServerPortListening;
		if (hasNoIssues)
			LOG.info("Server is running and healthy.");
		else
			LOG.info("Server has issues, see below!");

		if (isAdminPortListening)
			LOG.info("* Admin-Interface (Port: " + this.portAdminServer + "): Started");
		else
			LOG.info("* Admin-Interface (Port: " + this.portAdminServer + "): ERROR; Port not listening!");

		if (startFileServer) {
			if (isFileDownloadPortListening)
				LOG.info("* File-Server [Download] (Port: " + fileServer.portFileServer + "): Started");
			else
				LOG.info("* File-Server [Download] (Port: " + fileServer.portFileServer + "): ERROR; Port not listening!");
			if (isFileUploadPortListening)
				LOG.info("* File-Server [Upload]   (Port: " + fileServer.portFileReceiver + "): Started");
			else
				LOG.info("* File-Server [Upload]   (Port: " + fileServer.portFileReceiver + "): ERROR; Port not listening!");
		}
		if (startWebServer) {
			if (isWebServerPortListening)
				LOG.info("* Web-Server (Port: " + this.portWebServer + "): Started");
			else
				LOG.info("* Web-Server (Port: " + this.portWebServer + "): ERROR; Port not listening!");
		}


		// No output coloring here, because we don't want to risk if unparsed ASCII coloring
		// messes up the logging of a probe, e.g. Windows service manager logs.
		if (!LOG.isInfoEnabled()) {
			System.out.println("");
			if (hasNoIssues)
				System.out.println(ansiServerName + " " + Colors.green("Server is running and healthy!"));
			else
				System.out.println(ansiErrServerName + " " + Colors.red("Server has issues, see below!"));
			if (isAdminPortListening)
				System.out.println(ansiServerName + " * Admin-Interface (Port: " + this.portAdminServer + "): Started");
			else
				System.out.println(ansiErrServerName + " * Admin-Interface (Port: " + this.portAdminServer + "): "+Colors.red("ERROR")+"; Port not listening!");
			if (startFileServer) {
				if (isFileDownloadPortListening)
					System.out.println(ansiServerName + " * File-Server [Download] (Port: " + fileServer.portFileServer + "): Started");
				else
					System.out.println(ansiErrServerName + " * File-Server [Download] (Port: " + fileServer.portFileServer + "): "+Colors.red("ERROR")+"; Port not listening!");
				if (isFileUploadPortListening)
					System.out.println(ansiServerName + " * File-Server [Upload]   (Port: " + fileServer.portFileReceiver + "): Started");
				else
					System.out.println(ansiErrServerName + " * File-Server [Upload]   (Port: " + fileServer.portFileReceiver + "): "+Colors.red("ERROR")+"; Port not listening!");
			}
			if (startWebServer) {
				if (isWebServerPortListening)
					System.out.println(ansiServerName + " * Web-Server (Port: " + this.portWebServer + "): Started");
				else
					System.out.println(ansiErrServerName + " * Web-Server (Port: " + this.portWebServer + "): "+Colors.red("ERROR")+"; Port not listening!");
			}
		}
		return hasNoIssues;
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
					serverSocket.setSoTimeout(serverTimeout * 1000);
			} catch (IOException e) {
				LOG.error("Admin server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.ansiErrServerName + " Admin server listener cannot be created on port '" + this.listenerPort + "'!");
				Helper.fatalExit();
			}
		}

		@Override
		public void run() {
			// Server main loop
			while (!BaseServer.this.serverStop) {
				Socket clientSocket = null;
				try {
					// it waits for a connection
					clientSocket = serverSocket.accept();
					final String threadName;
					final InetAddress addr = clientSocket.getInetAddress();
					if (addr != null) {
						threadName = BaseServer.this.name + "-Client(" + addr.toString() + ")";
					} else {
						threadName = BaseServer.this.name + "-Client";
					}
					if (clientSocket != null) {
						final ClientHandler handler = new ClientHandler(clientSocket);
                        clientExecutorService.execute(() -> {
                            Thread.currentThread().setName(threadName);
                            try {
                            	handler.run();
							} catch (Exception e) {
								LOG.error("Error handling client: " + threadName, e);
				                throw new RuntimeException("Error handling client: " + threadName, e);
							}
                        });
					}
		        } catch (IOException e) {
		        	if (!BaseServer.this.serverStop) {
		        		LOG.error("Admin server connection listener failed! We recommend to restart the server!", e);
		        	}
		        } finally {
		        	if (!BaseServer.this.serverStop && serverSocket != null && serverSocket.isClosed()) {
	        			try {
	        				serverSocket = BaseServer.this.serverSocketFactory.create(this.listenerPort);
		    				if (serverTimeout > 0)
		    					serverSocket.setSoTimeout(serverTimeout);
	                    } catch (IOException e) {
	                        LOG.error("Failed to recreate server socket after it was closed.", e);
	                    }
		        	}
	            }
            }
			if (!hookShutdown) {
				// loop has been broken by STOP command.
				Communicator.safeClose(serverSocket);
				// shutdown server
				stopServer();
				// shutdown thread pool
				shutDownExecutorService();
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

				// Correct server name?
				final String serverName = command.getServerName();
				if (!serverName.equals(BaseServer.this.getServerName())) {
					LOG.error("Server command: Wrong server name received, command is ignored!");
					return;
				}

				// execute command
				final ClientAnswer answer = BaseServer.this.processServerCommand(command);

				// Health status request?
				if (answer instanceof HealthAnswer) {
					LOG.info("[HEALTH] signal received, printing server's health state to console.");
					// print info
					BaseServer.this.printHealthStatus(true);
					return;
				}

				// Shutdown received?
				if (answer instanceof StopAnswer) {
					LOG.info("[STOP] signal received! Shutting down...");
					if (!LOG.isInfoEnabled()) {
						System.out.println("");
						System.out.println(BaseServer.ansiServerName + " " + Colors.darkRed("[STOP]") + " signal received! Shutting down...");
					}
					// only escape of this loop
					BaseServer.this.serverStop = true;
					Communicator.safeClose(serverSocket);
					return;
				}

				// We have to answer -> get output-stream to client
	            sendResponse(answer);

			} catch (UtilsException e) {
				LOG.error("Admin server couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the secret key seed doesn't match or different encrypt modes");
				LOG.error("     have been defined within client/server-configuration, or the server's");
				LOG.error("     configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both sides!");
				e.printStackTrace();
	        } catch (IOException e) {
				LOG.error("Admin server listener failed! Possible invalid messages from '{}' received.", clientSocket.getRemoteSocketAddress(), e);
			} finally {
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket);
	        }
		}

	    /**
	     * Sends the answer to the client using the output stream.
	     */
	    private void sendResponse(ClientAnswer answer) {
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
	    }
	}

	/**
	 * Help class for shell script.
	 */
	protected static final class Help {
		private static final String BEETROOT_PREFIX = "beetroot.";
		private static final String SHELL_EXT = SystemUtils.IS_OS_UNIX ? "sh" : "bat";
		private static final String TITLE = Colors.darkCyan("beetRoot Server");
		private static final String JAVA  = Colors.green("java");
		private static final String USAGE = Colors.darkYellow(BEETROOT_PREFIX+SHELL_EXT+" start|stop|health");
		private static final String USAGE0 = Colors.darkYellow(BEETROOT_PREFIX+SHELL_EXT+" -help");
		private static final String USAGE1 = Colors.darkYellow(BEETROOT_PREFIX+SHELL_EXT+" -h");
		public static final String TEXT =
				"" 																						+ OS.LINE_SEPARATOR +
				"" 																						+ OS.LINE_SEPARATOR +
				TITLE									 												+ OS.LINE_SEPARATOR +
				"---------------------" 																+ OS.LINE_SEPARATOR +
    			"Usage:"																				+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"  Here's a detailed usage of the java-process, but you should use the server-script" 	+ OS.LINE_SEPARATOR +
    			"  in the root-directory that accepts the commands 'health', 'start' or 'stop'."		+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"    " + USAGE								 											+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"  Without script - the Java processes:" 												+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ OS.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer <command>"		+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"      or" 																				+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"    "+JAVA+" -DROOTPATH=\"<root-path>\" \\"											+ OS.LINE_SEPARATOR +
    			"         -Dlog4j.configuration=file:<log-cfg-path>/server-logging.cfg \\"		 		+ OS.LINE_SEPARATOR +
    			"         -cp \"<classpath>\" ch.autumo.beetroot.server.BeetRootServer <command>"		+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"      <root-path>      :  Root directory where beetRoot is installed." 				+ OS.LINE_SEPARATOR +
    			"                          Defined in run-script (Variable ROOT)."						+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"      <classpath>      :  The Java classpath."								 			+ OS.LINE_SEPARATOR +
    			"                          Is build by the run-script."									+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"  or" 																					+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"    " + USAGE0					 														+ OS.LINE_SEPARATOR +
    			"    " + USAGE1					 														+ OS.LINE_SEPARATOR +
    			"" 																						+ OS.LINE_SEPARATOR +
    			"";
	}

}
