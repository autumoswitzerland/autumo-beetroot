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
import ch.autumo.beetroot.utils.web.Web;


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

		
		// 1. Read general configuration
		final BeetRootConfigurationManager configMan = BeetRootConfigurationManager.getInstance();
		try {
			configMan.initializeWithFullPath(webAppRoot + configFilePath, getServletContext());
		} catch (Exception e) {
			LOG.error("Configuration initialization failed !", e);
			throw new ServletException("Configuration initialization failed !", e);
		}
		
		
		// 2. Logging configuration
		final String logCfgFile = config.getInitParameter("beetRootLogConfig");
		if (logCfgFile != null && !logCfgFile.isEmpty()) {
			// 2.1 Apache Tomcat.
			try {
				LoggingFactory.getInstance().initialize(webAppRoot + logCfgFile, configMan.getServletName());
			} catch (Exception ioex) {
				throw new ServletException("Logging configuration initialization failed !", ioex);
			}
		}
		// logCfgFile = null ->
		// 2.2 For WebLogic, log4j2-logging will be initialized
		//     by the log4j-web-jar and the listener defined in web.xml.
		// 2.3 Jetty uses simpler logging that can be bridged with slf4j-simple.
		
		
		// 3. DB connection manager
		try {
			BeetRootDatabaseManager.getInstance().initialize(webAppRoot);
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			throw new ServletException("Couldn't decrypt DB password!", e);
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			throw new ServletException("Couldn't create DB manager!", e);
		}

		
		// 4. Create the beetRoot server running in a passive server mode,
		//    basically only parsing and sending the body
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
		
		// Clear sessions from memory
		sessions.clear(); // all we need to do here
		// Free service resource, etc.
		beetRootService.destroy();
		// Release database resources
		BeetRootDatabaseManager.getInstance().release();
		// No threads need to be stopped, no streams closed,
		// servlet-container does it all for us here.
		// Just call the standard servlet-destroy-method.  
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
		// Servlet-container session ID
		final String sessionID = request.getSession().getId();
		BeetRootHTTPSession session = null; 
		if (sessions.containsKey(sessionID))
			return sessions.get(sessionID); // found !
        // Create a temporary file manager that handles the uploads within NANO-Httpd API
        final ITempFileManager tempFileManager = beetRootService.newTempFileManager();
		// If there isn't a session yet, create one and deliver the input stream to it for parsing the body
        session = new BeetRootHTTPSession(sessionID, tempFileManager, request.getInputStream());
        // Store it
        sessions.put(sessionID, session);
        return session;		
	}	

}
