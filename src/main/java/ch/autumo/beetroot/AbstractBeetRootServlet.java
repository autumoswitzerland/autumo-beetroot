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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.logging.LoggingFactory;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.Web;


/**
 * Base beetRoot servlet.
 */
public class AbstractBeetRootServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractBeetRootServlet.class.getName());
	
	private BeetRootService beetRootService = null;
	private Map<String, BeetRootHTTPSession> sessions = new ConcurrentHashMap<String, BeetRootHTTPSession>();

	
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		
		final String webAppRoot = Web.getRealPath(config.getServletContext());
		final String configFilePath = config.getInitParameter("beetRootConfig");
		final String beetRootServiceClass = config.getInitParameter("beetRootServiceClass");


		// Read general config
		final BeetRootConfigurationManager configMan = BeetRootConfigurationManager.getInstance();
		try {
			configMan.initializeWithFullPath(webAppRoot + configFilePath, getServletContext());
		} catch (Exception e) {
			LOG.error("Configuration initialization failed !", e);
			throw new ServletException("Configuration initialization failed !", e);
		}		

		
		// Logging configuration
		final String servletContainer = config.getInitParameter("servletContainer");
		if (servletContainer == null || !servletContainer.equals("jetty")) {
			// configure logging
			final String logCfgFile = config.getInitParameter("beetRootLogConfig");
			try {
				LoggingFactory.getInstance().initialize(webAppRoot + logCfgFile);
			} catch (Exception ioex) {
				
				LOG.error("Logging configuration initialization failed !", ioex);
				throw new ServletException("Logging configuration initialization failed !", ioex);
			}
		}

		
		// DB connection manager
		try {
			BeetRootDatabaseManager.getInstance().initialize(webAppRoot);
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			throw new ServletException("Couldn't decrypt DB password!", e);
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			throw new ServletException("Couldn't create DB manager!", e);
		}

		
		// Create the beetRoot server running in a passive server mode,
		// basically only parsing and sending the body
		try {
			final Class<?> clz = Class.forName(beetRootServiceClass);
			beetRootService = (BeetRootService) clz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			LOG.error("Couldn't create beetroot service from class '"+beetRootServiceClass+"'!", e);
			throw new ServletException("Couldn't create beetroot service from class '"+beetRootServiceClass+"'!", e);
		}

		/** Servlet's life-cycle doesn't allow this.
		// Finally load user sessions
		try {
			SessionManager.getInstance().load();
	    } catch (Exception e) {
	    	LOG.warn("Couldn't load user sessions!", e);
	    }
	    */
	}	
	
	@Override
	public void destroy() {
		
		/** Servlet's life-cycle doesn't allow this.
		// save user session
		try {
			SessionManager.getInstance().save();
		} catch (Exception e) {
			LOG.error("Couldn't store user sessions!", e);
		}
		*/
		
		// clear sessions from memory
		sessions.clear(); // all we need to do here
		
		// free service resource, etc.
		beetRootService.destroy();
		
		// release database resources
		BeetRootDatabaseManager.getInstance().release();
		
		// no threads need to be stopped, no streams closed,
		// servlet container does it all for us here.,
		// just call the standard servlet destroy-method  
		super.destroy();
	}
	
	/**
	 * Get the beetRoot service.
	 * 
	 * @return beetRoot service
	 */
	protected BeetRootService getBeetRootService() {
		return beetRootService;
	}
		
	/**
	 * Lookup an existing or a new session for the request given.
	 * 
	 * @param request servlet request
	 * @return beetRoot session
	 * @throws IOException IO Exception
	 */
	protected BeetRootHTTPSession findOrCreateHttpSession(HttpServletRequest request) throws IOException {

		// servlet container session ID
		final String sessionID = request.getSession().getId();
		
		BeetRootHTTPSession session = null; 
		if (sessions.containsKey(sessionID))
			return sessions.get(sessionID); // found !

        // Create a temp file manager that handles the uploads within nano mechanics
        final ITempFileManager tempFileManager = beetRootService.newTempFileManager();
        
		// If there isn't a session yet, create one and deliver the input stream to it for parsing the body
        session = new BeetRootHTTPSession(sessionID, tempFileManager, request.getInputStream());
        
        // store it.
        sessions.put(sessionID, session);
        
        return session;		
	}	

}
