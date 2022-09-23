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

/**
 * autumo ifaceX web server and template engine.
 */
public class BeetRootWebServer extends RouterNanoHTTPD implements BeetRootService {
	
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootWebServer.class.getName());
	
	private Class<?> defaultHandlerClass = TasksIndexHandler.class;
	private String defaultHandlerEntity = "tasks";
	
	private boolean dbPwEnc = ConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
	
	private boolean csrf = true;

    private boolean insertServletNameInTemplateRefs = false;
    private String servletName = null;
    
    private Map<String, String> parsedCss = new ConcurrentHashMap<String, String>();
    
	
	public BeetRootWebServer() throws Exception {
		this(-1);
	}
	
	public BeetRootWebServer(int port) throws Exception {

		super(port);
		
		csrf = ConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_USE_CSRF_TOKNES);
		if (csrf)
	    	LOG.info("CSRF activated!");
		ConfigurationManager.getInstance().setCsrf(csrf);
		
		servletName = ConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
		if (servletName != null && servletName.length() != 0)
			insertServletNameInTemplateRefs = true; 
		
		this.addMappings();
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
			
	        timeout = ConfigurationManager.getInstance().getInt("ws_connection_timeout");
	        
	        if (timeout == -1) {
	        	
				timeout = 5000;
				LOG.error("Using 5 seconds for client connection timeout.");
	        }
	        
	        timeout = timeout * 1000;
	        
		} catch (Exception e) {
			
			timeout = 5000;
			LOG.error("Couldn't read 'ws_connection_timeout' from configuration. Using 5 seconds!");
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
	 * Main serve method for the beetroot-engine.
	 * 
	 * @param session nano session.
	 * @return response response
	 */
	@Override
	public Response serve(IHTTPSession session) {
		return this.serve((BeetRootHTTPSession) session, null);
	}
	
	/**
	 * Main serve method for the beetroot-engine in a servlet context
	 * 
	 * @param session session.
	 * @param request servlet request
	 * @return response response
	 */
	public Response serve(BeetRootHTTPSession session, HttpServletRequest request) {

		boolean loggedIn = false;
		
		final String dir = "web/";
		String uri = Utils.normalizeUri(session.getUri());
		//String uri = session.getUri();

		
		// servlet magic :)
		if (insertServletNameInTemplateRefs && uri.startsWith(servletName+"/")) {
			uri = uri.replaceFirst(servletName+"/", "");
		}
		
		
	    final Session userSession = SessionManager.getInstance().findOrCreate(session);

	    // first try...
	    try {
	    	
	    	LanguageManager.getInstance();
	    	
	    } catch (Exception e) {
	    	
	    	String langs = ConfigurationManager.getInstance().getString("web_languages");
	    	
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
    	final ServletContext context = ConfigurationManager.getInstance().getServletContext();
    	
        
		// web resources except html templates
		if (uri.contains(".") && !uri.endsWith(".html")) { // Note: template request have no extension at all!

			final String requestedFile = uri.substring(uri.lastIndexOf("/") + 1, uri.length()).toLowerCase();
			
			// nothing to serve
			if (requestedFile.equals("theme-default.css"))
				return Response.newFixedLengthResponse(Status.OK, "text/css", "");
			
			boolean isSpecialCss = requestedFile.equals("refs.css") || requestedFile.equals("default.css");
			isSpecialCss = isSpecialCss || (requestedFile.contains("theme-") && requestedFile.endsWith(".css"));
	        
	    	FileCache fc = null;
	    	String filePath = null;
	    	boolean isResource = false;
        	if (context != null) {

        		try {
        			filePath = Utils.getRealPath(context) + dir + uri;
					fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
				} catch (IOException e) {
					LOG.info("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						filePath = "/" + dir + uri;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
						isResource = true;
					} catch (IOException e1) {
						final String err = "Resource not found on server looking up with resource path '" + filePath + "'!";
						LOG.error(err, e);
						String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
						String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, uri);
						return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
					}
				}
        	} else {  
		        try {
		        	filePath = dir + uri;
	        		fc = FileCacheManager.getInstance().findOrCreate(filePath, isSpecialCss);
		        } catch (IOException e) {
		        	
					final String err = "Resource not found on server looking up with file path '" + filePath + "'!";
					LOG.error(err, e);
					String t = LanguageManager.getInstance().translate("base.err.resource.title", userSession);
					String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, uri);
					return serverResponse(session, ErrorHandler.class, Status.NOT_FOUND, t, m);
		        }
        	}
	        
			
	    	// this consults cached 'META-INF/mime.types' !
	        final String mimeType = Constants.MIME_TYPES_MAP.getContentType(uri);
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
		
        
		// Continue with HTML engie requests !
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
	    	    
	    // logout
		if (uri.endsWith("/users/logout")) {
			
			loggedIn = false;
			
			session.getParameters().clear();
			session.getHeaders().put("Connection", "close");
			
			final Response end = serverResponse(session, LogoutHandler.class, "logout", LanguageManager.getInstance().translate("base.info.logout.msg", userSession));
			
			userSession.destroy(session.getCookies());
			
			return end;
		}
		
        // User logged in to session?
	    // Settings
		final String sessionUser = userSession.getUserName();
        if (sessionUser != null) {
        	
			//LOG.debug("Logged in (Session), User: " + sessionUser);

    		loggedIn = true;
    		
    		if (uri.endsWith("/users/login")) {
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
        	
        	// login from login page?
        	if (postParamUsername != null && postParamUsername.length() != 0) {
        	
        		String postParamPass = session.getParms().get("password");
                int dbId = -1;
                String dbPass = null;
                String dbRole = null;
                String dbFirstName = null;
                String dbLastName = null;
                
            	if (postParamPass != null && postParamPass.length() != 0) {
            		
            		Connection conn = null;
            		Statement stmt = null;
            		ResultSet rs = null;
            		
					try {
						
						conn = DatabaseManager.getInstance().getConnection();
	            		stmt = conn.createStatement();
						//NO SEMICOLON
	            		rs = stmt.executeQuery("select id, password, role, firstname, lastname from users where username='"+postParamUsername+"'");
	            		
	            		if (rs.next()) {
	            			dbId = rs.getInt("id");
	            			dbPass = rs.getString("password");
	            			dbRole = rs.getString("role");
	            			dbFirstName = rs.getString("firstname");
	            			dbLastName = rs.getString("lastname");
	            		}
	            		
					} catch (SQLException e) {
						
						final String err = "Server Internal Error - DB is possibly not reachable, check DB configuration - DB Exception: " + e.getMessage();
						LOG.error(err, e);
						
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
							
							postParamPass = Utils.encode(postParamPass, SecureApplicationHolder.getInstance().getSecApp());
							
						} catch (UtilsException e) {
							
							final String err = "Server Internal Error - Exception: " + e.getMessage();
							LOG.error(err, e);
							
							String t = LanguageManager.getInstance().translate("base.err.srv.ex.title", userSession);
							String m = LanguageManager.getInstance().translate("base.err.srv.ex.msg", userSession, e.getMessage());
							return serverResponse(session, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
						}
					}
					
            		if (postParamPass.equals(dbPass)) { 
            			
            			userSession.setUserData(dbId, postParamUsername, dbRole, dbFirstName, dbLastName);
            			userSession.createIdPair(dbId, "users");
            			
			            loggedIn = true;
			            
					    try {
							Utils.loadUserSettings(userSession);
						} catch (SQLException e) {
							LOG.error("Couldn't load user settings!", e);
						}
			            
			            try {
			            	
							DatabaseManager.resetToken(dbId);
							
						} catch (Exception e1) {
							final String err = "Couldn't reset last token for user '"+postParamUsername+"' after login!";
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
						
						String m = LanguageManager.getInstance().translate("base.info.welcome.msg", userSession, postParamUsername);
						return serverResponse(session, getDefaultHandlerClass(), getDefaultHandlerEntity(), m);
			            
            		} else {
            			
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
			
			if (uri.endsWith("/users/reset") || uri.endsWith("/users/change")) {
				
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
	@Override
	public Class<?> getDefaultHandlerClass() {

		String handler;
		try {
			handler = ConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_HANDLER);
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
	@Override
	public String getDefaultHandlerEntity() {
		
		String entity;
		try {
			entity = ConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_ENTITY);
			return entity;
		} catch (Exception e) {
	    	LOG.warn("Couldn't load default handler class sessions, using tasks handler!", e);
			return defaultHandlerEntity;
		}
	}
	
	/**
	 * Last call before the routed website is served.
	 * At this point, templates have been parsed and compiled.
	 * Overwrite this methid, if you still need to do something.
	 * In any case and at the end, it must call
	 * {@link RouterNanoHTTPD#serve(IHTTPSession)}
	 *  
	 * @param session HTTP sessiom
	 * @return response
	 */
	@Override
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
	@Override
	public void addMappings() {
		
		Router router = null; 
		final String webRouter = ConfigurationManager.getInstance().getString("web_router");
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
