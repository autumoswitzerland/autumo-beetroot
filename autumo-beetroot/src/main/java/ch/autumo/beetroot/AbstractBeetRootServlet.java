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
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;


/**
 * Base beetRoot servlet.
 */
public class AbstractBeetRootServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected final static Logger LOG = LoggerFactory.getLogger(AbstractBeetRootServlet.class.getName());
	
	private BeetRootService beetRootService = null;
	private Map<String, BeetRootHTTPSession> sessions = new ConcurrentHashMap<String, BeetRootHTTPSession>();

	
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		
		final String webAppRoot = Utils.getRealPath(config.getServletContext());
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
	 * @param request
	 * @return beetRoot session
	 * @throws IOException
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
