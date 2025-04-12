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
package ch.autumo.beetroot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.nanohttpd.protocols.http.ClientHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.nanohttpd.protocols.http.threading.IAsyncRunner;
import org.nanohttpd.router.RouterNanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.cache.FileCache;
import ch.autumo.beetroot.cache.FileCacheManager;
import ch.autumo.beetroot.crud.DeleteListener;
import ch.autumo.beetroot.crud.EventHandler;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.Error404Handler;
import ch.autumo.beetroot.handler.ErrorHandler;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.tasks.TasksIndexHandler;
import ch.autumo.beetroot.handler.users.User;
import ch.autumo.beetroot.handler.usersroles.UserRole;
import ch.autumo.beetroot.mailing.MailerFactory;
import ch.autumo.beetroot.routing.Route;
import ch.autumo.beetroot.routing.Router;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.server.BaseServer;
import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.sms.MessengerFactory;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.common.MIME;
import ch.autumo.beetroot.utils.database.DB;
import ch.autumo.beetroot.utils.security.Security;
import ch.autumo.beetroot.utils.system.OS;
import ch.autumo.beetroot.utils.web.TwoFA;
import ch.autumo.beetroot.utils.web.Web;

/**
 * The beetRoot Web Server and Template Engine.
 */
public class BeetRootWebServer extends RouterNanoHTTPD implements BeetRootService {
	
	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootWebServer.class.getName());

	/**
	 * Virtual relative path for temporary stored files to 
	 * server (e.g. generated images such as the 2FA QR code.
	 */ 
    public static final String VIRTUAL_TMP_PATH = "tmp/";
	
	private Class<?> defaultHandlerClass = TasksIndexHandler.class;
	private String defaultHandlerEntity = "tasks";
	
	private boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
	private boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
	private String cmdMode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_MODE, "sockets");
	
	private boolean isWebCmdMode = false;
	
    private boolean insertServletNameInTemplateRefs = false;
    private String servletName = null;
    
    private Map<String, String> parsedCss = new ConcurrentHashMap<>();
	
    private String apiKeyName = null;
    private String webApiKey = null;    
    
    // A reference to the base server; only outside servlet context! 
    private BaseServer baseServer = null;

    // The router
	private Router beetRootrouter = null;
	
	// The routes (without default routes)
	private List<Route> routes = null;
	
	
    /**
     * Server.
     * 
     * @throws Exception exception
     */
	public BeetRootWebServer() throws Exception {
		this(-1);
	}

	/**
	 * Server.
	 * 
	 * @param port port
	 * @throws Exception exception
	 */
	public BeetRootWebServer(int port) throws Exception {

		super(port);
		
		apiKeyName = BeetRootConfigurationManager.getInstance().getString("web_api_key_name"); // may be used or not
		
		// Server commands tunneled over HTTP/HTTPS?
		this.isWebCmdMode = cmdMode.equalsIgnoreCase("web");
		if (this.isWebCmdMode) {
			if (pwEncoded)
				webApiKey = BeetRootConfigurationManager.getInstance().getDecodedString("admin_com_web_api_key", SecureApplicationHolder.getInstance().getSecApp());
			else
				webApiKey = BeetRootConfigurationManager.getInstance().getString("admin_com_web_api_key");
		}
		
		try {
			
			// Aren't we allowed to delete admin-role? If so, install listener for prevention
			if (!BeetRootConfigurationManager.getInstance().getYesOrNo("web_admin_role_delete", Constants.NO)) {
				EventHandler.getInstance().addDeleteListener(Role.class, new DeleteListener() {
					@Override
					public boolean beforeDelete(Model bean) {
						return ((Role) bean).getName().equalsIgnoreCase("Administrator");
					}
				});
			}
			
			servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
			if (servletName != null && servletName.length() != 0)
				insertServletNameInTemplateRefs = true; 
			
			// Create the router
			final String webRouter = BeetRootConfigurationManager.getInstance().getString("web_router");
			try {
				final Class<?> clz = Class.forName(webRouter);
				beetRootrouter = (Router) clz.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
		    	LOG.error("No router found! Your web app will definitely NOT work!", e);
				throw e;
			}
			
			// Add routes and mappings
			this.addMappings();
			
			// Make configured routes available to all handlers 
			BaseHandler.registerRoutes(this.routes);
			
			// init any other modules
			this.initModules(BeetRootConfigurationManager.getInstance().runsWithinServletContext(), BeetRootConfigurationManager.getInstance().getFullConfigBasePath());
			
		} catch (Exception ex) {
			
			LOG.error("Couldn't initialize beetRoot Server or Service!", ex);
			throw ex;
		}
	}

	/**
	 * Initialize additional modules, etc.
	 * 
	 * @param isWithinServlet is within servlet?
	 * @param fullConfigBasePath full path where configuration files are located
	 * @throws Exception exception
	 */
	private void initModules(boolean isWithinServlet, String fullConfigBasePath) throws Exception {
		Initializer initializer = null;
		final String clz = BeetRootConfigurationManager.getInstance().getString("ws_init_class");
		if (clz != null && clz.length() != 0) {
			Class<?> clazz = Class.forName(clz);
			final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            initializer = (Initializer) constructor.newInstance();
            initializer.initModules(isWithinServlet, fullConfigBasePath);
		}
	}
	
	@Override
    protected ClientHandler createClientHandler(final Socket finalAccept, final InputStream inputStream) {
        return new BeetRootClientHandler(this, inputStream, finalAccept);
    }
	
	/**
	 * Get async runner.
	 * @return async runner
	 */
	public IAsyncRunner getAsyncRunner() {
		return super.asyncRunner;
	}
	
	/**
	 * Set base server.
	 * @param baseServer base server
	 */
	public void setBaseServer(BaseServer baseServer) {
		this.baseServer = baseServer;
	}
	
    /**
     * Start the server.
     */
	@Override
	public void start() throws IOException {
        start(false);
    }
	
    /**
     * Starts the server (in setDaemon(true) mode).
     */
	@Override
    public void start(final int timeout) throws IOException {
        start(timeout, false);
    }
	
    /**
     * Start the server.
     * 
     * @param daemon start the web server as a daemon thread?
     * @throws IOException IO exception
     */
	public void start(boolean daemon) throws IOException {
		int timeout = - 1;
		try {
	        timeout = BeetRootConfigurationManager.getInstance().getInt("connection_timeout");
	        if (timeout == -1) {
				timeout = 5000;
				LOG.error("Using 5 seconds for client connection timeout.");
	        }
	        timeout = timeout * 1000;
		} catch (Exception e) {
			timeout = 5000;
			LOG.error("Couldn't read 'connection_timeout' from configuration. Using 5 seconds!");
		}
		start(timeout, daemon);
	}

    /**
     * Start the server.
     * 
     * @param timeout timeout to use for socket connections.
     * @param daemon start the thread daemon or not.
     * @throws IOException if the socket is in use.
     */
	@Override
    public void start(final int timeout, boolean daemon) throws IOException {
		super.start(timeout, daemon);
		try {
			SessionManager.load();
	    } catch (Exception e) {
	    	LOG.warn("Couldn't load user sessions!", e);
	    }    	
    }
    
    /**
     * Stop the server.
     */
	@Override
    public void stop() {
		super.stop();
        try {
            SessionManager.save();
        } catch (Exception e) {
        	LOG.warn("Couldn't save current user sessions!", e);
        }
		// Clear cache
		FileCacheManager.getInstance().clear();
    }	
  
	/**
	 * Main serve method for the beetRoot-engine.
	 * 
	 * @param session nano session.
	 * @return response response
	 */
	@Override
	public Response serve(IHTTPSession session) {
		return this.serve((BeetRootHTTPSession) session, null);
	}
	
	@Override
	public ITempFileManager newTempFileManager() {
		return this.getTempFileManagerFactory().create();
	}

	@Override
	public void destroy() {
		// Clear cache
		FileCacheManager.getInstance().clear();
	}

	/**
	 * Main serve method for the beetroot-engine in a servlet context
	 * 
	 * @param session session.
	 * @param request servlet request
	 * @return response response
	 */
	public Response serve(BeetRootHTTPSession session, HttpServletRequest request) {
		
		final String uri = Web.normalizeUri(session.getUri());

		
		// URI reduction for internal resources
		String uriWithoutServlet = uri;
		if (insertServletNameInTemplateRefs && uri.startsWith(servletName+"/")) {
			uriWithoutServlet = uri.replaceFirst(servletName+"/", "");
		}
		
		// JSON server command without login, but with API key - only outside servlet context,
		// we do not send server commands to web containers, the only serve pages or or less;
		// commands are addressed to the beetRoot server always! 
		if (!BeetRootConfigurationManager.getInstance().runsWithinServletContext()
				&& this.isWebCmdMode
				&& uriWithoutServlet.startsWith(Constants.URI_SRV_CMD)
				&& session.getHeaders().get("user-agent").equals(Communicator.USER_AGENT)) { 

			/*
			 * Map<String, String> m = session.getHeaders();
			 */	
			
			// check API key
			final String webApiKeyInReq = session.getParms().get(apiKeyName);
			if (!webApiKeyInReq.equalsIgnoreCase(webApiKey)) {
				LOG.warn("Server command: HTTP/HTTPS Server Command: Web API key invalid!");
				return serverCommandResponse(session, new ClientAnswer("HTTP/HTTPS Server Command: Web API key invalid!", ClientAnswer.TYPE_ERROR));
			}

			// parse server command
			ServerCommand command;
			try {
				command = Communicator.readJsonCommand(session.getInputStream(), (int) session.getBodySize());
			} catch (IOException e) {
				LOG.error("Server command: Couldn't parse server command from HTTP/HTTPS request!", e);
				return serverCommandResponse(session, new ClientAnswer("Couldn't parse server command from HTTP/HTTPS request!", ClientAnswer.TYPE_ERROR));
			}
			
			// Correct server name?
			final String serverName = command.getServerName();
			if (!serverName.equals(baseServer.getServerName())) {
				LOG.error("Server command: Wrong server name received, command is ignored!");
				return serverCommandResponse(session, new ClientAnswer("Wrong server name received, command is ignored!", ClientAnswer.TYPE_ERROR));
			}

			// Internal server commands never arrive here, they are forced to be served over sockets!
			// Here we only process dispatcher commands!
			
			// process command for module dispatchers
			final ClientAnswer answer = baseServer.processServerCommand(command);
			
			// answer !
			return serverCommandResponse(session, answer);
		}

		
		// JSON
		if (uriWithoutServlet.endsWith(Constants.JSON_EXT)) { // JSON serve without login, but with API key
			
			final String apiKey = session.getParms().get(apiKeyName);
			String dbApiKey = null;
			try {
				dbApiKey = BeetRootDatabaseManager.getInstance().getProperty("web.json.api.key");
			} catch (Exception e) {
				LOG.warn("Couldn't read property from DB!", e);
				final String t = LanguageManager.getInstance().translate("base.err.srv.db.title", LanguageManager.DEFAULT_LANG);
				final String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", LanguageManager.DEFAULT_LANG, e.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
			}
			
			if (dbApiKey != null) {
				dbApiKey = dbApiKey.trim();
			}
			
			if (dbApiKey != null && apiKey!= null && dbApiKey.equals(apiKey)) {
				return this.serveAtLast((BeetRootHTTPSession)session); // All good!
			} else {
				LOG.warn("JSON API (URI: '{}'): Access with wrong JSON API Key!", uriWithoutServlet);
				final String t = LanguageManager.getInstance().translate("base.err.srv.auth.title", LanguageManager.DEFAULT_LANG);
				final String m = LanguageManager.getInstance().translate("base.err.srv.auth.msg", LanguageManager.DEFAULT_LANG);
				return serverResponse(session, ErrorHandler.class, Status.UNAUTHORIZED, t, m);
			}
		}
		
		
	    final Session userSession = SessionManager.getInstance().findOrCreate(session);

	    
	    // first try...
	    try {
	    	if (!LanguageManager.isInitialized())
	    		LanguageManager.getInstance();
	    } catch (Exception e) {
			LOG.warn("No default translation file 'lang_default.properties' or 'tmpl_lang_default.properties' (if 'web_translations switched' on) found! That is not desirable!");
			final String t = "Language configuration error";
			final String m = "No default translation file found! That is not desirable! This Message is NOT translated!";
			return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
		}

	    
	    // Language; don't use LanguageManager.retrieveLanguage here,
	    // we need to cover the special case if the user requests
	    // another language
	    User user = userSession.getUser();
	    String userLang = LanguageManager.getInstance().parseLang(uri);
	    
		if (userLang == null && user == null) {
			// From HTTP header!
			userLang = LanguageManager.getInstance().getLanguageFromHttpSession(session);
		}
	    String dbUserLang = null;
	    if (user != null) {
	    	dbUserLang = user.getLang();
	    	// Special/initial case: We have a DB user, but no language, 
	    	// so initially set the detected language for him!
	    	if (dbUserLang == null) {
		    	LanguageManager.getInstance().updateLanguage(userLang, userSession);
		    	user.setLang(userLang);
		    	dbUserLang = userLang;
	    	}
	    } else {
	    	dbUserLang = null; // we don't know what the preference of the user is, because he is not logged in yet
	    }

	    if (userLang != null && userLang.length() != 0) {
	    	if (dbUserLang != null && !userLang.equals(dbUserLang)) {
		    	// User request another language, update it in the DB!
	    		LanguageManager.getInstance().updateLanguage(userLang, userSession);
	    	}
	    	userSession.setUserLang(userLang);
	    } else {
	    	if (dbUserLang == null)
	    		userSession.setUserLang(LanguageManager.DEFAULT_LANG);
	    	else
	    		userSession.setUserLang(dbUserLang);
	    }
	    
	    
	    // Are we running in a servlet context?
    	final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();

    	
		boolean loggedIn = false;
		final String dir = "web/";
    	
		// web resources except html templates
		if (uriWithoutServlet.contains(".") && !uriWithoutServlet.endsWith(".html")) { // Note: template request have no extension at all!

			final String requestedFile = uriWithoutServlet.substring(uriWithoutServlet.lastIndexOf("/") + 1, uriWithoutServlet.length()).toLowerCase();
			
			// temporary file?
			if (uriWithoutServlet.startsWith("tmp/")) {
				
				final String tmpDir = OS.getTemporaryDirectory();
				
				final String fullTmpPath = tmpDir + requestedFile;
				final File f = new File(fullTmpPath);
				final String mimeType = Constants.MIME_TYPES_MAP.getContentType(requestedFile);
				try {
					return Response.newFixedLengthResponse(Status.OK, mimeType, new FileInputStream(f), f.length());
				} catch (FileNotFoundException e) {
					final String err = "Couldn't serve temporary file '" + fullTmpPath + "'!";
					LOG.error(err, e);
					final String t = LanguageManager.getInstance().translate("base.err.resource.title", userLang);
					final String m = LanguageManager.getInstance().translate("base.err.resource.msg", userLang, uriWithoutServlet);
					return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
				}
			}
			
			
			// Theme default CSS: nothing to serve
			if (requestedFile.equals("theme-default.css")) // virtual CSS one could say...
				return Response.newFixedLengthResponse(Status.OK, "text/css", "");
			
			
			// We force-cache these CSS, because we want to change URLs within these cached CSSs
			// --> servlet-magic within servlet container!
			// base.css and jquery-ui.min.css are not cached!
			boolean isSpecialCss = requestedFile.equals("refs.css") || requestedFile.equals("default.css");
			isSpecialCss = isSpecialCss || (requestedFile.contains("theme-") && requestedFile.endsWith(".css"));
	        
			
	    	FileCache fc = null;
	    	String filePath = null;
	    	boolean isResource = false;
        	if (context != null) {

        		try {
        			filePath = Web.getRealPath(context) + dir + uriWithoutServlet;
					fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
				} catch (IOException e) {
					LOG.info("File '{}'not found on server, looking further within archives...", filePath);
					try {
						filePath = "/" + dir + uriWithoutServlet;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
						isResource = true;
					} catch (IOException e1) {
						final String err = "Resource not found on server looking up with resource path '" + filePath + "'!";
						LOG.error(err, e);
						final String t = LanguageManager.getInstance().translate("base.err.resource.title", userLang);
						final String m = LanguageManager.getInstance().translate("base.err.resource.msg", userLang, uriWithoutServlet);
						return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
					}
				}
        	} else {  
		        try {
		        	filePath =  BeetRootConfigurationManager.getInstance().getRootPath() + dir + uriWithoutServlet;
	        		fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
		        } catch (IOException e) {
					LOG.info("File '{}'not found on server, looking further within archives...", filePath);
					try {
						filePath = "/" + dir + uriWithoutServlet;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
						isResource = true;
					} catch (IOException e1) {
						final String err = "Resource not found on server looking up with file path '" + filePath + "'!";
						LOG.error(err, e);
						final String t = LanguageManager.getInstance().translate("base.err.resource.title", userLang);
						final String m = LanguageManager.getInstance().translate("base.err.resource.msg", userLang, uriWithoutServlet);
						return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
					}
		        }
        	}
	        
			
	    	// this consults cached 'META-INF/mime.types' !
	        final String mimeType = Constants.MIME_TYPES_MAP.getContentType(uriWithoutServlet);

	        
	        // Decide what to do with different mime types and requests
			try {
				// archives
				if (MIME.isMimeTypeArchive(mimeType)) {
					return fc.createResponse();
				// binaries
				} else if (MIME.isMimeTypeOctet(mimeType)) {
					return fc.createResponse();
				// text
				} else {
					// Special case: URL-parsed CSS within servlet context
					if (insertServletNameInTemplateRefs && isSpecialCss) {
						// NOTICE: we need to add the servlet-name to "url('" too for specific css!
						String css = null;
						FileCache cache = null;
							 cache = isResource ? 
									FileCacheManager.getInstance().findOrCreateByResource(filePath) : 
										FileCacheManager.getInstance().findOrCreate(filePath);
						if (!parsedCss.containsKey(filePath)) {
							// special CSS is cached, no further checks necessary with a 
							// certainty of 99.99% (first files, max. cache isn't reached at all)
							// So we can getTextData !
							css = cache.getTextData();
							//css = css.replaceAll("url\\('", "url('/"+servletName);
							css = css.replace("url('", "url('/"+servletName); //test it: "url('" -> "url('/"+servletName
							parsedCss.put(filePath, css);
						} else {
							css = parsedCss.get(filePath);
						}
						return Response.newFixedLengthResponse(Status.OK, "text/css", css);
					}
					// Parse password strength java-script file for translations
					if (requestedFile.equals("password-strength.js")) {
						String l = session.getParms().get("lang");
						if (l == null)
							l = userSession.getUserLang();
						final String js = PwsParser.parseAll(fc.getTextData(), l);
						return Response.newFixedLengthResponse(Status.OK, "application/javascript", js);
					} else if (requestedFile.endsWith("search.js")) {
						// Parse all scripts ending with search for servlet-name when in servlet-context
						String js = fc.getTextData();
						if (insertServletNameInTemplateRefs) {
							js = js.replace("{$servletName}", "/"+servletName);
						} else {
							js = js.replace("{$servletName}", "");
						}
						return Response.newFixedLengthResponse(Status.OK, "application/javascript", js);
					}
					// Everything else: Text data !
					if (MIME.isMimeTypeText(mimeType))
						return fc.createResponse();
					// If we come here, a mime type has been requested that is not yet implemented
					final String err = "Mime type for web resource '" + filePath + "' not implemented yet!";
					LOG.warn(err);
					final String t = LanguageManager.getInstance().translate("base.err.resource.mime.title", userLang);
					final String m = LanguageManager.getInstance().translate("base.err.resource.mime.msg", userLang, filePath);
					return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
		        }	
	        } catch (IOException e) {
				final String err = "Couldn't parse css for pre-url replacements Resource Not found! - Web resource '" + filePath + "'.";
				LOG.error(err, e);
				final String t = LanguageManager.getInstance().translate("base.err.resource.title", userLang);
				final String m = LanguageManager.getInstance().translate("base.err.resource.msg", userLang, filePath);
				return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
	        }
		}
		
        
		// Continue with HTML engine requests !
		final Map<String, String> files = new HashMap<String, String>();
	    final Method method = session.getMethod();
	    
	    if (Method.PUT.equals(method) || Method.POST.equals(method)) {
	        try {
	        	if (context != null) {
	        		session.parseBodyForServlet(files, request);
	        	}
	        	else {
	        		session.parseBody(files); // parameter key and temporary path value!
	        	}
	            userSession.addFiles(files);
	        } catch (IOException ioe) {
				final String err = "Server Internal Error - I/O Exception: " + ioe.getMessage();
				LOG.error(err, ioe);
				final String t = LanguageManager.getInstance().translate("base.err.srv.io.title", userLang);
				final String m = LanguageManager.getInstance().translate("base.err.srv.io.msg", userLang, ioe.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
				
	        } catch (ResponseException re) {
				final String err = "Server Internal Error - Response Exception (Status: "+re.getStatus().getDescription()+"): " + re.getMessage();
				LOG.error(err, re);
				final String t = LanguageManager.getInstance().translate("base.err.srv.re.title", userLang);
				final String m = LanguageManager.getInstance().translate("base.err.srv.re.msg", userLang, re.getStatus().getRequestStatus(), re.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
	        }
	    }

	    
		final String postParamUsername = session.getParms().get("username");
	    	    
		
		// Within the web server, we have to take care of user session timeouts
		// In servlet containers this is done by the container
		if (context == null && userSession.isOlderThanSessionTimeout() ) {
			loggedIn = false;
			session.getParameters().clear();
			session.getHeaders().put("Connection", "close");
			if (userSession.getUserSettings() != null)
				userSession.addOrUpdateUserSetting("theme", "dark"); // Logout should go back to dark theme
			final Response end = serverResponse(session, this.getHandlerClass("LogoutHandler"), "logout", LanguageManager.getInstance().translate("base.info.session.timeout", userLang));
			userSession.deleteAllParameters();
			userSession.destroy(session.getCookies());
			return end;
		}
		
		// No session timeout happened, but a request has been made, so refresh the session!
		if (context == null)
			userSession.refresh();
		
	    // logout
		if (uriWithoutServlet.endsWith("/users/logout")) {
			loggedIn = false;
			session.getParameters().clear();
			session.getHeaders().put("Connection", "close");
			if (userSession.getUserSettings() != null)
				userSession.addOrUpdateUserSetting("theme", "dark"); // Logout should go back to dark theme
			final Response end = serverResponse(session, this.getHandlerClass("LogoutHandler"), "logout", LanguageManager.getInstance().translate("base.info.logout.msg", userLang));
			userSession.deleteAllParameters();
			userSession.destroyDelete(session.getCookies());
			return end;
		}
		
        // User logged in to session?
	    // Settings
		final String sessionUser = userSession.getUserName();
        if (sessionUser != null && !userSession.isTwoFaLoginOk()) {
    		loggedIn = true;
    		if (uriWithoutServlet.endsWith("/users/login")) {
	            return serverResponse(session, getDefaultHandlerClass(), getDefaultHandlerEntity());
    		}
		    try {
				DB.loadUserSettings(userSession);
			} catch (SQLException e) {
				LOG.error("Couldn't load user settings!", e);
			}
        }
        
        // Still not logged in...
        if (!loggedIn) {
    		final String twoFaCode = session.getParms().get("code");
        	// 2FA step login: 2nd Step !
        	if (userSession.isTwoFaLoginOk() && twoFaCode != null && twoFaCode.length() != 0) {
        		if (userSession.getInternalTOTPCode().equals(twoFaCode)) {
		            loggedIn = true;
        			userSession.resetTwoFaLogin();
        			userSession.clearInternalTOTPCode();
        			userSession.refresh();
        			userSession.removeAllIds();
		            // Finish all necessary steps and give response
		            return postLogin(session, userSession, userSession.getUserId(), userSession.getUserName());
        		} else {
        			userSession.clearUserDataExceptLanguage();
					String m = LanguageManager.getInstance().translate("base.err.login.msg", userLang, postParamUsername);
					return serverResponse(session, this.getHandlerClass("LoginHandler"), "Login", m);
        		}
        	}        	
        	// login from login page?
        	else if (postParamUsername != null && postParamUsername.length() != 0) {
        		
        		String postParamPass = session.getParms().get("password");
        		String dbRoles = "";
        		String dbPermissions = "";
        		
                boolean dbTwoFa = false;
            	if (postParamPass != null && postParamPass.length() != 0) {
            		Connection conn = null;
            		Statement stmt = null;
            		ResultSet rs = null;
					try {
						
						// Get user from DB
	            		user = (User) Model.findFirst(User.class, "username = ?", postParamUsername);
	            		if (user != null) {
	            			if (user.getLang() == null) { // still can be null here!
	            				user.setLang(userLang);
	            			}
	            			dbTwoFa = user.getTwoFa();
	            		
		            		// Roles
		        			final List<Model> usersRoles = UserRole.where(UserRole.class, "user_id = ?", Integer.valueOf(user.getId()));
		        			if (usersRoles == null)
		        				throw new SQLException("no roles data!");
		        			for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
		        				final UserRole userRole = (UserRole) iterator.next();
		        				final Role role = (Role) userRole.getAssociatedReference(Role.class);
		        				dbRoles += role.getName() + ",";
		        				dbPermissions += role.getPermissions()+",";
		        			}
		        			if (usersRoles.size() > 0) {
		        				dbRoles = dbRoles.substring(0, dbRoles.length() - 1);
		        				if (dbPermissions.endsWith(","))
		        					dbPermissions = dbPermissions.substring(0, dbPermissions.length() - 1);
		        			}
		        			dbRoles = dbRoles.toLowerCase();
		        			dbPermissions = dbPermissions.toLowerCase();
	            		}

					} catch (SQLException e) {
						final String err = "Server Internal Error - DB is possibly not reachable, Check DB configuration - DB Exception: " + e.getMessage();
						LOG.error(err, e);
            			userSession.clearUserDataExceptLanguage();
						String t = LanguageManager.getInstance().translate("base.err.srv.db.title", userLang);
						String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", userLang, e.getMessage());
						return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
					} finally {
						try {
							if (rs != null)
								rs.close();
							if (stmt != null)
								stmt.close();
							if (conn != null)
								conn.close();
						} catch (SQLException e) {
							// no need
						}
					}


					// 0. LOGIN
					boolean loginSuccess = false;
					if (user != null) {
						if (dbPwEnc) {
							// A) Hashed password check
							try {
								loginSuccess = Security.verifyPw(postParamPass, user.getPassword());
							} catch (UtilsException e) {
								final String err = "Server Internal Error - Exception: " + e.getMessage();
								LOG.error(err, e);
		            			userSession.clearUserDataExceptLanguage();
								String t = LanguageManager.getInstance().translate("base.err.srv.ex.title", userLang);
								String m = LanguageManager.getInstance().translate("base.err.srv.ex.msg", userLang, e.getMessage());
								return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
							}
						} else {
							// B) Clear password check
							loginSuccess = postParamPass.equals(user.getPassword());
						}
					}
					
					// 1. GO !
            		if (loginSuccess) { 
            			// Store user data to session
            			userSession.setUserData(user, dbRoles, dbPermissions);
            			userSession.createIdPair(user.getId(), "users");

					    // 2FA enabled?: 1st Step!
					    if (dbTwoFa) {
					    	
			        		final String genCode = TwoFA.create6DigitTOTPCode(userSession.getUserSecretKey());
			        		userSession.setInternalTOTPCode(genCode);
					    	userSession.setTwoFaLogin(); 
					    	
					    	// Email 2FA code?
					    	boolean codeEmailOn = false;
							try {
								codeEmailOn = BeetRootDatabaseManager.getInstance().onOrOff("security.2fa.code.email");
							} catch (Exception e) {
								final String err = "Server Internal Error - DB is possibly not reachable, check DB configuration - DB Exception: " + e.getMessage();
								LOG.error(err, e);
								final String t = LanguageManager.getInstance().translate("base.err.srv.db.title", userSession);
								final String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", userSession, e.getMessage());
								return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
							}
							if (codeEmailOn) {
								final Map<String, String> variables = new HashMap<>();
								variables.put("title", LanguageManager.getInstance().translate("base.mail.code.title", session));
								variables.put("subtitle", LanguageManager.getInstance().translate("base.mail.code.subtitle", session));
								variables.put("code", genCode);
								variables.put("message", LanguageManager.getInstance().translate("base.mail.code.msg", session));
								try {
									// Mail it!
									MailerFactory.getInstance().mail(
											new String[] {user.getEmail()},
											LanguageManager.getInstance().translate("base.mail.code.title", session),
											variables,
											"code",
											session
										);	
						        } catch (Exception me) {
									final String err = "Server Internal Error - Mail Exception: " + me.getMessage();
									LOG.error(err, me);
									final String t = LanguageManager.getInstance().translate("base.err.srv.mail.title", userSession);
									final String m = LanguageManager.getInstance().translate("base.err.srv.mail.msg", userSession, me.getMessage());
									return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
						        }
							}
							
					    	// SMS 2FA code?
					    	boolean codeSmSOn = false;
							try {
								codeSmSOn = BeetRootDatabaseManager.getInstance().onOrOff("security.2fa.code.sms");
							} catch (Exception e) {
								final String err = "Server Internal Error - DB is possibly not reachable, check DB configuration - DB Exception: " + e.getMessage();
								LOG.error(err, e);
								String t = LanguageManager.getInstance().translate("base.err.srv.db.title", userSession);
								String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", userSession, e.getMessage());
								return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
							}
							if (codeSmSOn) {
								final String info = LanguageManager.getInstance().translate("base.sms.code.info", userSession);
								final String note = LanguageManager.getInstance().translate("base.sms.code.note", userSession);
								try {
									// SMS it!
									MessengerFactory.getInstance().sms(user.getPhone(), info + ": " + genCode + "(" + note + ")");
						        } catch (Exception me) {
									final String err = "Server Internal Error - SMS Exception: " + me.getMessage();
									LOG.error(err, me);
									String t = LanguageManager.getInstance().translate("base.err.srv.sms.title", userSession);
									String m = LanguageManager.getInstance().translate("base.err.srv.sms.msg", userSession, me.getMessage());
									return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
						        }
							}							

							// Go to OTP handler
							return serverResponse(session, this.getHandlerClass("OtpHandler"), "2FA Code");
					    }
            			

					    // LOGGED IN!
			            loggedIn = true;
	        			userSession.refresh();
	        			userSession.removeAllIds();

			            // Finish all necessary steps and give response
			            return postLogin(session, userSession, user.getId(), postParamUsername);
			            
            		} else {
            			userSession.clearUserDataExceptLanguage();
						// serve login page!
						String m = LanguageManager.getInstance().translate("base.err.login.msg", userLang, postParamUsername);
						return serverResponse(session, this.getHandlerClass("LoginHandler"), "Login", m);
            		}
            	}
        	}
        }

		// Nope, no login after all.
		if (!loggedIn) {
			userSession.clearUserDataExceptLanguage();
			if (uriWithoutServlet.endsWith("/users/reset") || uriWithoutServlet.endsWith("/users/change")) {
	            return this.serveAtLast(session);
			} else {
				// serve login page!
				return serverResponse(session, this.getHandlerClass("LoginHandler"));
			}
		} else { // start parsing app, logged in !
		    // use CSRF tokens ?
		    if (BeetRootConfigurationManager.getInstance().useCsrf()) {
		    	try {
			    	if (!csrf(session, userSession)) {
						String t = LanguageManager.getInstance().translate("base.err.csrf.inv.title", userSession);
						String m = LanguageManager.getInstance().translate("base.err.csrf.inv.msg", userSession);
						return serverResponse(session, ErrorHandler.class, Status.BAD_REQUEST, t, m);
			    	}
				} catch (UtilsException e) {
					String t = LanguageManager.getInstance().translate("base.err.csrf.gen.title", userSession);
					String m = LanguageManager.getInstance().translate("base.err.csrf.gen.msg", userSession, e.getMessage());
					return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);							
				}
		    }
			// Finally, must be a route for HTML templates -> GO!
            return this.serveAtLast(session);
		}
	}

	private Response postLogin(BeetRootHTTPSession session, Session userSession, int userId, String username) {
	    try {
			DB.loadUserSettings(userSession);
		} catch (SQLException e) {
			LOG.error("Couldn't load user settings from DB!", e);
		}
        try {
			BeetRootDatabaseManager.getInstance().resetToken(userId);
		} catch (Exception e1) {
			final String err = "Couldn't reset last token for user '"+username+"' after login!";
			LOG.warn(err, e1);
		}
	    // use CSRF tokens ?
	    if (BeetRootConfigurationManager.getInstance().useCsrf()) {
	    	try {
				if (!csrf(session, userSession)) {
					String t = LanguageManager.getInstance().translate("base.err.csrf.inv.title", userSession);
					String m = LanguageManager.getInstance().translate("base.err.csrf.inv.msg", userSession);
					return serverResponse(session, ErrorHandler.class, Status.BAD_REQUEST, t, m);
				}
			} catch (UtilsException e) {
				String t = LanguageManager.getInstance().translate("base.err.csrf.gen.title", userSession);
				String m = LanguageManager.getInstance().translate("base.err.csrf.gen.msg", userSession, e.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);							
			}
	    }
		String m = LanguageManager.getInstance().translate("base.info.welcome.msg", userSession, userSession.getUserFullNameOrUserName());
		return serverResponse(session, getDefaultHandlerClass(), getDefaultHandlerEntity(), m);		
	}
	
	private boolean csrf(IHTTPSession session, Session userSession) throws UtilsException {
		// only check relevant POST methods!
		final String method = session.getParms().get("_method");
		if (method == null || method.length() == 0) {
			// make a new CSRF token
		    final String formCsrfToken = Security.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
		    userSession.setFormCsrfToken(formCsrfToken);
			return true;
		}
		if (method != null && method.length() != 0) {
			if (!method.equals("POST")) {
				// make a new CSRF token
			    final String formCsrfToken = Security.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
			    userSession.setFormCsrfToken(formCsrfToken);
				return true;
			}
		}
		// CSRF POST method
		final String lastCsrfToken = session.getParms().get("_csrfToken");
		final String storedFormCsrfToken = userSession.getFormCsrfToken();
		if (storedFormCsrfToken != null && storedFormCsrfToken.length() != 0 && !lastCsrfToken.equals(storedFormCsrfToken)) {
			return false;
		}
		// make a new CSRF token
	    final String formCsrfToken = Security.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
	    userSession.setFormCsrfToken(formCsrfToken);
	    return true;
	}
	
	public static Response serverCommandResponse(BeetRootHTTPSession session, ClientAnswer answer) {
		Status stat = Status.OK;
		if (answer.getType() == ClientAnswer.TYPE_ERROR)
			stat = Status.BAD_REQUEST;
		try {
			return Response.newFixedLengthResponse(stat, Communicator.HTTP_HEADER_CONTENTTYPE_JSON_UTF8[1], answer.getJsonTransferString());
		} catch (IOException e) {
			String err = "Server comand error! - Couldn't create JSON transfer string!";
			LOG.error(err, e);
			final ClientAnswer errAnswer = new ClientAnswer("Couldn't create JSON transfer string!", ClientAnswer.TYPE_ERROR);
			try {
				return Response.newFixedLengthResponse(Status.BAD_REQUEST, Communicator.HTTP_HEADER_ACCEPT_JSON[1], errAnswer.getJsonTransferString());
			} catch (IOException e1) {
				// NADA!
				return null;
			}
		}
	}
	
	public static Response serverResponse(
			BeetRootHTTPSession session, 
			Class<?> handlerClass, 
			Object... initParameter) {
		final Session userSession = session.getUserSession();
		Constructor<?> constructor = null;
        final Constructor<?> constructors[] = handlerClass.getDeclaredConstructors();
		int ip = initParameter.length;
        for (int i = 0; i < constructors.length; i++) {
			int pc = constructors[i].getParameterCount();
			if (pc == ip) {
				constructor = constructors[i];
				break;
			}
		}
        try {
            constructor.setAccessible(true);
		} catch (Exception e) {
			String err = "Handler constructor error! - No implementation found for handler class '"+handlerClass.toString()+"' with "+ip+" parameters.";
			LOG.error(err, e);
			String t = "<h1>"+LanguageManager.getInstance().translate("base.err.handler.construct.title", userSession)+"</h1>";
			String m = "<p>"+LanguageManager.getInstance().translate("base.err.handler.construct.msg", userSession, handlerClass.toString(), ip, e.getMessage())+"</p>";
			return Response.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/html", t+m);
		}
        BaseHandler handler = null;
        
        try {
            handler = (BaseHandler) constructor.newInstance(initParameter);
		} catch (Exception e) {
			String err = "Handler error! - No implementation found for handler class '"+handlerClass.toString()+"'!";
			LOG.error(err, e);
			String t = "<h1>"+LanguageManager.getInstance().translate("base.err.handler.impl.title", userSession)+"</h1>";
			String m = "<p>"+LanguageManager.getInstance().translate("base.err.handler.impl.msg", userSession, handlerClass.toString(), e.getMessage())+"</p>";
			return Response.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/html", t+m);
		}
        // initialize !
        handler.initialize(session);
        UriResource ur = null;
        if (initParameter.length > 0)
        	ur = new UriResource(null, handlerClass, initParameter[0]);
        else
        	ur = new UriResource(null, handlerClass);
		return ((UriResponder) handler).get(ur, session.getParms(), session);
	}
	
	/**
	 * Get a handler class by handler name.
	 * @param handlerName handler name
	 * @return handler class or null if not found
	 */
	public final Class<?> getHandlerClass(String handlerName) {
		Class<?> clz = null;
		for (Iterator<Route> iterator = routes.iterator(); iterator.hasNext();) {
			final Route route = iterator.next();
			clz = route.getHandler();
			if (clz.getName().endsWith(handlerName))
				return clz;
		}
		LOG.error("No router for handler name '{}' found!", handlerName);
		return null;
	}
	
	/**
	 * Get default handler class. 
	 * Overwrite for customization.
	 * 
	 * @return default handler class
	 */
	public Class<?> getDefaultHandlerClass() {
		String handler;
		try {
			handler = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_HANDLER);
			return Class.forName(handler);
		} catch (Exception e) {
	    	LOG.warn("Couldn't load default handler class sessions, using tasks handler!", e);
			return defaultHandlerClass;
		}
	}
	
	/**
	 * Get default handler entity. 
	 * Overwrite for customization.
	 * 
	 * @return default handler entity
	 */
	public String getDefaultHandlerEntity() {
		String entity;
		try {
			entity = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_ENTITY);
			return entity;
		} catch (Exception e) {
	    	LOG.warn("Couldn't load default handler class sessions, using tasks handler!", e);
			return defaultHandlerEntity;
		}
	}
	
	/**
	 * Last call before the routed website is served.
	 * At this point, templates have been parsed and compiled.
	 * Overwrite this method, if you still need to do something.
	 * In any case and at the end, it must call
	 * {@link RouterNanoHTTPD#serve(IHTTPSession)}
	 *  
	 * @param session HTTP sessiom
	 * @return response
	 */
	public Response serveAtLast(BeetRootHTTPSession session) {
        return super.serve(session);
	}
	
    /**
     * New public method to add routes with priority.
     * Beetroot has an own generic router where priorities 
     * are pre-defined.
     * 
     * @param url url
     * @param priority priority
     * @param handler handler
     * @param initParameter init parameter for handler
     */
    public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
        router.addRoute(url, priority, handler, initParameter);
    }
	
	/**
	 * Add mappings respectively set web routes. 
	 */
	public final void addMappings() {
		super.setNotImplementedHandler(NotImplementedHandler.class);
		super.setNotFoundHandler(Error404Handler.class);
		/** Default Routes */
		final List<Route> defRoutes = beetRootrouter.getDefaultRoutes();
		for (Iterator<Route> iterator = defRoutes.iterator(); iterator.hasNext();) {
			final Route route = iterator.next();
			addRoute(route.getRoute(), route.getPriority(), getDefaultHandlerClass(), getDefaultHandlerEntity());
		}
		/** Routes */
		routes = beetRootrouter.getRoutes();
		for (Iterator<Route> iterator = routes.iterator(); iterator.hasNext();) {
			final Route route = iterator.next();
			addRoute(route.getRoute(), route.getPriority(), route.getHandler(), route.getInitParameter());
		}
	}

	/**
	 * Parser for 'password-strength.js'.
	 */
	private static class PwsParser {

		public static final String PW_INFO = "{$pw.info}";
		public static final String PW_HIDE = "{$pw.hide}";
		public static final String PW_SHOW = "{$pw.show}";
		public static final String PW_CHARS = "{$pw.chars}";
		public static final String PW_CAPITAL = "{$pw.capital}";
		public static final String PW_NUMBER = "{$pw.number}";
		public static final String PW_SPECIAL = "{$pw.special}";
		public static final String PW_LETTER = "{$pw.letter}";
		
		/**
		 * Replace all variables.
		 * 
		 * @param script java-script file contents
		 * @param lang language
		 * @return replaced java-script contents
		 */
		public static String parseAll(String script, String lang) {
			script = script.replace(PW_INFO, LanguageManager.getInstance().translate("pw.info", lang));
			script = script.replace(PW_HIDE, LanguageManager.getInstance().translate("pw.hide", lang));
			script = script.replace(PW_SHOW, LanguageManager.getInstance().translate("pw.show", lang));
			script = script.replace(PW_CHARS, LanguageManager.getInstance().translate("pw.chars", lang));
			script = script.replace(PW_CAPITAL, LanguageManager.getInstance().translate("pw.capital", lang));
			script = script.replace(PW_NUMBER, LanguageManager.getInstance().translate("pw.number", lang));
			script = script.replace(PW_SPECIAL, LanguageManager.getInstance().translate("pw.special", lang));
			script = script.replace(PW_LETTER, LanguageManager.getInstance().translate("pw.letter", lang));
			return script;
		}
	}

}
