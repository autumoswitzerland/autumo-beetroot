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
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.Error404Handler;
import ch.autumo.beetroot.handler.ErrorHandler;
import ch.autumo.beetroot.handler.tasks.TasksIndexHandler;
import ch.autumo.beetroot.handler.users.LoginHandler;
import ch.autumo.beetroot.handler.users.LogoutHandler;
import ch.autumo.beetroot.handler.users.OtpHandler;
import ch.autumo.beetroot.routing.Route;
import ch.autumo.beetroot.routing.Router;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.server.BaseServer;
import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;

/**
 * autumo ifaceX web server and template engine.
 */
public class BeetRootWebServer extends RouterNanoHTTPD implements BeetRootService {
	
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootWebServer.class.getName());
	
	private Class<?> defaultHandlerClass = TasksIndexHandler.class;
	private String defaultHandlerEntity = "tasks";
	
	private boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
	private boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
	private String cmdMode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_MODE, "sockets");
	
	private boolean csrf = true;
	private boolean isWebCmdMode = false;
	
    private boolean insertServletNameInTemplateRefs = false;
    private String servletName = null;
    
    private Map<String, String> parsedCss = new ConcurrentHashMap<String, String>();
    
    private String tmpFilePrefix = "beetrootweb-";
	
    private String apiKeyName = null;
    private String webApiKey = null;    
    
    // A reference to the base server; only outside servlet context! 
    private BaseServer baseServer = null;
    
    
    /**
     * Server.
     * 
     * @throws Exception
     */
	public BeetRootWebServer() throws Exception {
		this(-1);
	}

	/**
	 * Server.
	 * 
	 * @param port
	 * @throws Exception
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
			
			csrf = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_USE_CSRF_TOKENS);
			if (csrf)
		    	LOG.info("CSRF activated!");
			BeetRootConfigurationManager.getInstance().setCsrf(csrf);
			
			servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
			if (servletName != null && servletName.length() != 0)
				insertServletNameInTemplateRefs = true; 
			
			tmpFilePrefix = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_TMP_FILE_PREFIX);
			if (tmpFilePrefix == null || tmpFilePrefix.length() == 0)
				tmpFilePrefix = "beetrootweb-";
			
			// routes
			this.addMappings();
			
			// init any other modules
			this.initModules(BeetRootConfigurationManager.getInstance().runsWithinServletContext(), BeetRootConfigurationManager.getInstance().getFullConfigBasePath());
			
		} catch (Exception ex) {
			
			LOG.error("Couldn't initialize beetRoot Server or Service!", ex);
			throw ex;
		}
	}

	/**
	 * Initialize modules, etc.
	 * 
	 * @param isWithinServlet is within servlet?
	 * @param fullConfigBasePath full path where configuration files are located
	 * @throws Exception
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
     * @param daemon
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
     * @param timeout
     *            timeout to use for socket connections.
     * @param daemon
     *            start the thread daemon or not.
     * @throws IOException
     *             if the socket is in use.
     */
	@Override
    public void start(final int timeout, boolean daemon) throws IOException {
    	
		super.start(timeout, daemon);
		
		try {
			SessionManager.getInstance().load();
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
            SessionManager.getInstance().save();
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
		
		final String uri = Utils.normalizeUri(session.getUri());

		
		// Servlet magic :)
		String uriWithoutServlet = uri;
		if (insertServletNameInTemplateRefs && uri.startsWith(servletName+"/")) {
			uriWithoutServlet = uri.replaceFirst(servletName+"/", "");
		}
		
		
		// JSON server command without login, but with API key - only outside servlet context!
		if (!BeetRootConfigurationManager.getInstance().runsWithinServletContext()
				&& this.isWebCmdMode
				&& uriWithoutServlet.startsWith(Constants.URI_SRV_CMD)
				&& session.getHeaders().get("user-agent").equals(Communicator.USER_AGENT)) { 

			/*
			Map<String, String> m = session.getHeaders();
			Set<String> s = m.keySet();
			for (Iterator<String> iterator = s.iterator(); iterator.hasNext();) {
				String k = iterator.next();
				System.err.println("K:"+k+", V:"+m.get(k));
			}	
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
				String t = LanguageManager.getInstance().translate("base.err.srv.db.title", LanguageManager.DEFAULT_LANG);
				String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", LanguageManager.DEFAULT_LANG, e.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
			}
			
			if (dbApiKey != null)
				dbApiKey = dbApiKey.trim();
			
			if (dbApiKey != null && apiKey!= null && dbApiKey.equals(apiKey)) {
				return this.serveAtLast((BeetRootHTTPSession)session); // All good!
			}
			else {
				LOG.warn("JSON API (URI: '"+uriWithoutServlet+"'): Access with wrong JSON API Key!");
				String t = LanguageManager.getInstance().translate("base.err.srv.io.title", LanguageManager.DEFAULT_LANG);
				String m = LanguageManager.getInstance().translate("base.err.srv.io.msg", LanguageManager.DEFAULT_LANG, "Disperse, nothing to see here!");
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
			}
		}
		
		
	    final Session userSession = SessionManager.getInstance().findOrCreate(session);

	    // first try...
	    try {
	    	
	    	LanguageManager.getInstance();
	    	
	    } catch (Exception e) {
	    	
	    	String langs = BeetRootConfigurationManager.getInstance().getString("web_languages");
	    	
			LOG.warn("Language(s) '"+langs+"' has/have been configured, but the translations are missing!");
			String t = LanguageManager.getInstance().translate("base.err.lang.title", LanguageManager.DEFAULT_LANG);
			String m = LanguageManager.getInstance().translate("base.err.lang.msg", LanguageManager.DEFAULT_LANG, langs);
			return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
		}
	    
	    // Language
	    String userLang = LanguageManager.getInstance().parseLang(uri);
	    String dbUserLang = LanguageManager.getInstance().getLanguageFromDb(userSession);
	    if (userLang != null && userLang.length() != 0) {
	    	
	    	// user request another lang, update it in the db!
	    	if (!userLang.equals(dbUserLang))
	    		LanguageManager.getInstance().updateLanguage(userLang, userSession);
	    	
	    	userSession.setUserLang(userLang);
	    	
	    } else {
	    	
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
			if (uriWithoutServlet.startsWith("tmp/" + tmpFilePrefix)) {
				
				final String tmpDir = Utils.getTemporaryDirectory();
				
				final String fullTmpPath = tmpDir + requestedFile;
				final File f = new File(fullTmpPath);
				final String mimeType = Constants.MIME_TYPES_MAP.getContentType(requestedFile);
				try {
					return Response.newFixedLengthResponse(Status.OK, mimeType, new FileInputStream(f), f.length());
				} catch (FileNotFoundException e) {
					final String err = "Couldn't serve temporary file '" + fullTmpPath + "'!";
					LOG.error(err, e);
					String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
					String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, uriWithoutServlet);
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
        			filePath = Utils.getRealPath(context) + dir + uriWithoutServlet;
					fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
				} catch (IOException e) {
					LOG.info("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						filePath = "/" + dir + uriWithoutServlet;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
						isResource = true;
					} catch (IOException e1) {
						final String err = "Resource not found on server looking up with resource path '" + filePath + "'!";
						LOG.error(err, e);
						String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
						String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, uriWithoutServlet);
						return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
					}
				}
        	} else {  
		        try {
		        	filePath =  BeetRootConfigurationManager.getInstance().getRootPath() + dir + uriWithoutServlet;
	        		fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
		        } catch (IOException e) {
					LOG.info("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						filePath = "/" + dir + uriWithoutServlet;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
						isResource = true;
					} catch (IOException e1) {
						final String err = "Resource not found on server looking up with file path '" + filePath + "'!";
						LOG.error(err, e);
						String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
						String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, uriWithoutServlet);
						return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
					}
		        }
        	}
	        
			
	    	// this consults cached 'META-INF/mime.types' !
	        final String mimeType = Constants.MIME_TYPES_MAP.getContentType(uriWithoutServlet);
	        //LOG.trace("MIME: "+mimeType);

	        
	        // decide what to do with different mime types and requests
			try {
				//final String ext = uri.substring(uri.lastIndexOf("."), uri.length());
				
				// archives
				if (Utils.isMimeTypeArchive(mimeType)) {
					
					return fc.createResponse();
					
				// binaries
				} else if (Utils.isMimeTypeOctet(mimeType)) {
					
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
							css = css.replaceAll("url\\('", "url('/"+servletName); //test it: "url('" -> "url('/"+servletName
							parsedCss.put(filePath, css);
						} else {
							css = parsedCss.get(filePath);
						}
							
						return Response.newFixedLengthResponse(Status.OK, "text/css", css);
					}
					
					// Everything else: Text data !
					if (Utils.isMimeTypeText(mimeType))
						return fc.createResponse();
	
					
					// If we come here, a mime type has been requested that is not yet implemented
					final String err = "Mime type for web resource '" + filePath + "' not implemented yet!";
					LOG.warn(err);
					String t = LanguageManager.getInstance().translate("base.err.resource.mime.title", userSession);
					String m = LanguageManager.getInstance().translate("base.err.resource.mime.msg", userSession, filePath);
					return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
		        }	
				
	        } catch (IOException e) {
				final String err = "Couldn't parse css for pre-url replacements Resource Not found! - Web resource '" + filePath + "'.";
				LOG.error(err, e);
				String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
				String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, filePath);
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
				
				String t = LanguageManager.getInstance().translate("base.err.srv.io.title", userSession);
				String m = LanguageManager.getInstance().translate("base.err.srv.io.msg", userSession, ioe.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
				
	        } catch (ResponseException re) {

				final String err = "Server Internal Error - Response Exception (Status: "+re.getStatus().getDescription()+"): " + re.getMessage();
				LOG.error(err, re);
				
				String t = LanguageManager.getInstance().translate("base.err.srv.re.title", userSession);
				String m = LanguageManager.getInstance().translate("base.err.srv.re.msg", userSession, re.getStatus().getRequestStatus(), re.getMessage());
				return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
	        }
	    }

	    
		final String postParamUsername = session.getParms().get("username");
	    	    
		
		// Within the ifaceX server, we have to take care of user session timeouts
		// In servlet containers this is done by the container
		if (context == null && userSession.isOlderThanSessionTimeout() ) {
			
			loggedIn = false;
			session.getParameters().clear();
			session.getHeaders().put("Connection", "close");
			
			final Response end = serverResponse(session, LogoutHandler.class, "logout", LanguageManager.getInstance().translate("base.info.session.timeout", userSession));
			
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
			
			final Response end = serverResponse(session, LogoutHandler.class, "logout", LanguageManager.getInstance().translate("base.info.logout.msg", userSession));
			
			userSession.deleteAllParameters();
			userSession.destroy(session.getCookies());
			
			return end;
		}
		
        // User logged in to session?
	    // Settings
		final String sessionUser = userSession.getUserName();
        if (sessionUser != null && !userSession.isTwoFaLoginOk()) {
        	
			//LOG.debug("Logged in (Session), User: " + sessionUser);

    		loggedIn = true;
    		
    		if (uriWithoutServlet.endsWith("/users/login")) {
	            return serverResponse(session, getDefaultHandlerClass(), getDefaultHandlerEntity());
    		}
    		
		    try {
				Utils.loadUserSettings(userSession);
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
		            
		            // Finish all necessary steps and give response
		            return postLogin(session, userSession, userSession.getUserId().intValue(), userSession.getUserName());
		            
        		} else {
        			
        			userSession.clearUserData();
        			
					String m = LanguageManager.getInstance().translate("base.err.login.msg", userSession, postParamUsername);
					return serverResponse(session, LoginHandler.class, "Login", m);
        		}
        		
        	}        	
        	// login from login page?
        	else if (postParamUsername != null && postParamUsername.length() != 0) {
        	
        		String postParamPass = session.getParms().get("password");
                int dbId = -1;
                String dbPass = null;
                String dbRole = null;
                String dbFirstName = null;
                String dbLastName = null;
                String dbEmail = null;
                String dbSecKey = null;
                boolean dbTwoFa = false;
                
            	if (postParamPass != null && postParamPass.length() != 0) {
            		
            		Connection conn = null;
            		Statement stmt = null;
            		ResultSet rs = null;
            		
					try {
						
						conn = BeetRootDatabaseManager.getInstance().getConnection();
	            		stmt = conn.createStatement();
						//NO SEMICOLON
	            		rs = stmt.executeQuery("select id, password, role, firstname, lastname, email, secretkey, two_fa from users where username='"+postParamUsername+"'");
	            		
	            		if (rs.next()) {
	            			dbId = rs.getInt("id");
	            			dbPass = rs.getString("password");
	            			dbRole = rs.getString("role");
	            			dbFirstName = rs.getString("firstname");
	            			dbLastName = rs.getString("lastname");
	            			dbEmail = rs.getString("email");
	            			dbSecKey = rs.getString("secretkey");
	            			dbTwoFa = rs.getBoolean("two_fa");
	            		}
	            		
					} catch (SQLException e) {
						
						final String err = "Server Internal Error - DB is possibly not reachable, check DB configuration - DB Exception: " + e.getMessage();
						LOG.error(err, e);

            			userSession.clearUserData();
						
						String t = LanguageManager.getInstance().translate("base.err.srv.db.title", userSession);
						String m = LanguageManager.getInstance().translate("base.err.srv.db.msg", userSession, e.getMessage());
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
					
					if (dbPwEnc) {
						
						try {
							
							postParamPass = Utils.hashPw(postParamPass, SecureApplicationHolder.getInstance().getSecApp());
							
						} catch (UtilsException e) {
							
							final String err = "Server Internal Error - Exception: " + e.getMessage();
							LOG.error(err, e);
							
	            			userSession.clearUserData();
							
							String t = LanguageManager.getInstance().translate("base.err.srv.ex.title", userSession);
							String m = LanguageManager.getInstance().translate("base.err.srv.ex.msg", userSession, e.getMessage());
							return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
						}
					}
					
					// LOGIN
            		if (postParamPass.equals(dbPass)) { 
            			
            			userSession.setUserData(dbId, postParamUsername, dbRole, dbFirstName, dbLastName, dbEmail, dbSecKey, dbTwoFa);
            			userSession.createIdPair(dbId, "users");

					    try {
	            			dbUserLang = BeetRootDatabaseManager.getInstance().getLanguage(dbId);
	            	    	userSession.setUserLang(dbUserLang);
						} catch (Exception e) {
							LOG.error("Couldn't load user language from DB!", e);
						}
					    
					    
					    // 2FA enabled?: 1st Step!
					    if (dbTwoFa) {
					    	
			        		final String genCode = Utils.create6DigitTOTPCode(userSession.getUserSecretKey());
			        		userSession.setInternalTOTPCode(genCode);
					    	userSession.setTwoFaLogin(); 
					    	
							return serverResponse(session, OtpHandler.class, "2FA Code");
					    }
            			
					    
			            loggedIn = true;
			            
			            // Finish all necessary steps and give response
			            return postLogin(session, userSession, dbId, postParamUsername);
			            
            		} else {
            			
            			userSession.clearUserData();
            			
						//LOG.debug("Wrong login with user User: " + postParamUsername);
						// serve login page!
						String m = LanguageManager.getInstance().translate("base.err.login.msg", userSession, postParamUsername);
						return serverResponse(session, LoginHandler.class, "Login", m);
            		}
            	}
        	}
        }

		// Nope, no login after all.
		if (!loggedIn) {
			
			userSession.clearUserData();
			
			if (uriWithoutServlet.endsWith("/users/reset") || uriWithoutServlet.endsWith("/users/change")) {
				
	            return this.serveAtLast((BeetRootHTTPSession)session);
				
			} else {
				
				// serve login page!
				return serverResponse(session, LoginHandler.class);
			}
			
		} else { // start parsing app, logged in !
			
		    // use CSRF tokens ?
		    if (csrf) {
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
			
			// Finally, must be a route for html templates -> GO!
            return this.serveAtLast((BeetRootHTTPSession)session);
		}
	}

	private Response postLogin(BeetRootHTTPSession session, Session userSession, int userId, String username) {
		
	    try {
			Utils.loadUserSettings(userSession);
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
	    if (csrf) {
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
        
		//LOG.debug("Logged in (Form), User: " + postParamUsername);
		String m = LanguageManager.getInstance().translate("base.info.welcome.msg", userSession, userSession.getUserFullNameOrUserName());
		return serverResponse(session, getDefaultHandlerClass(), getDefaultHandlerEntity(), m);		
	}
	
	private boolean csrf(IHTTPSession session, Session userSession) throws UtilsException {
		
		// only check relevant POST methods!
		final String method = session.getParms().get("_method");
		
		if (method == null || method.length() == 0) {
			
			// make a new CSRF token
		    final String formCsrfToken = Utils.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
		    userSession.setFormCsrfToken(formCsrfToken);
			return true;
		}
		
		if (method != null && method.length() != 0) {
			if (!method.equals("POST")) {
				// make a new CSRF token
			    final String formCsrfToken = Utils.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
			    userSession.setFormCsrfToken(formCsrfToken);
				return true;
			}
		}
		
		// CSRF POST method
		
		final String lastCsrfToken = session.getParms().get("_csrfToken");
		final String storedFormCsrfToken = userSession.getFormCsrfToken();
		if (storedFormCsrfToken != null && storedFormCsrfToken.length() != 0) {
			if (!lastCsrfToken.equals(storedFormCsrfToken)) {
				return false;
			}
		}

		// make a new CSRF token
	    final String formCsrfToken = Utils.generateCSRFToken(SecureApplicationHolder.getInstance().getSecApp());
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
		
		Session userSession = SessionManager.getInstance().findOrCreate(session);
		
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
	 * 
	 * @return default handler class
	 * @throws ClassNotFoundException 
	 */
	public void addMappings() {
		
		Router router = null; 
		final String webRouter = BeetRootConfigurationManager.getInstance().getString("web_router");
		try {
			final Class<?> clz = Class.forName(webRouter);
			router = (Router) clz.getDeclaredConstructor().newInstance();
			
		} catch (Exception e) {
	    	LOG.error("No router found! Your web app will definitely NOT work!", e);
			return;
		}
		
		super.setNotImplementedHandler(NotImplementedHandler.class);
		super.setNotFoundHandler(Error404Handler.class);
		
		/** Default Routes */
		final Route defRoutes[] = router.getDefaultRoutes();
		for (int i = 0; i < defRoutes.length; i++) {
			addRoute(defRoutes[i].getRoute(), defRoutes[i].getPriority(), getDefaultHandlerClass(), getDefaultHandlerEntity());
		}

		/** Routes */
		final Route routes[] = router.getRoutes();
		for (int i = 0; i < routes.length; i++) {
			addRoute(routes[i].getRoute(), routes[i].getPriority(), routes[i].getHandler(), routes[i].getInitParameter());
		}
	}
	
}
