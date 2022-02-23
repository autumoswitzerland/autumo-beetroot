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

import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.cache.FileCacheManager;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * autumo beetRoot servlet allowing the beetRoot template
 * engine running inside apache tomcat, etc.
 */
public class BeetRootServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final static Logger LOG = LoggerFactory.getLogger(BeetRootServlet.class.getName());
	
	private BeetRootService beetRootService = null;
	private BeetRootWebServer beetRootWebServer = null;
	
	private Map<String, BeetRootHTTPSession> sessions = new ConcurrentHashMap<String, BeetRootHTTPSession>();
	
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		
		String webAppRoot = config.getServletContext().getRealPath("/");
		if (!webAppRoot.endsWith("/")) {
			webAppRoot += "/";
		}
		String webAppRootWithoutSlash = webAppRoot.substring(0, webAppRoot.length() - 1);
		
		
		final String beetRootServiceClass = config.getInitParameter("beetRootServiceClass");
		final String configFilePath = config.getInitParameter("beetRootConfig");


		final String servletContainer = config.getInitParameter("servletContainer");
		if (servletContainer == null || !servletContainer.equals("jetty")) {
			// configure logging
			final String logCfgFile = config.getInitParameter("beetRootLogConfig");
			try {
				
				Utils.configureLog4j2(webAppRoot + logCfgFile);
				
			} catch (IOException ioex) {
				
				LOG.error("Logging configuration initialization failed !", ioex);
				throw new ServletException("Logging configuration initialization failed !", ioex);
			}
		}
		
		// Read general config
		final ConfigurationManager configMan = ConfigurationManager.getInstance();
		try {
			
			configMan.initializeWithFullPath(webAppRoot + configFilePath, getServletContext());
			
		} catch (Exception e) {
			
			LOG.error("Configuration initialization failed !", e);
			throw new ServletException("Configuration initialization failed !", e);
		}		
		
		// Are pw's in config encoded?
		boolean pwEncoded = configMan.getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
		
		String dbUrl = configMan.getString("db_url");
		if (dbUrl.contains("[WEB-CONTEXT-PATH]")) {
			dbUrl = dbUrl.replace("[WEB-CONTEXT-PATH]", webAppRootWithoutSlash);
		}
		
		// DB connection manager
		try {
			DatabaseManager.getInstance().initialize(
					dbUrl,
					configMan.getString("db_user"),
					pwEncoded ? 
							configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password")
				);
		} catch (UtilsException e) {
			LOG.error("Couldn't decrypt DB password!", e);
			throw new ServletException("Couldn't decrypt DB password!", e);
		} catch (Exception e) {
			LOG.error("Couldn't create DB manager!", e);
			throw new ServletException("Couldn't create DB manager!", e);
		}

		// create the beetRoot server running in a passive serve mode,
		// basically only parsing and sending the body
		try {
			
			final Class<?> clz = Class.forName(beetRootServiceClass);
			beetRootService = (BeetRootService) clz.getDeclaredConstructor().newInstance();
			
		} catch (Exception e) {
			
			LOG.error("Couldn't create beetroot service from class '"+beetRootServiceClass+"'!", e);
		}
		
		beetRootWebServer = (BeetRootWebServer) beetRootService;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.service(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.service(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.service(req, resp);
	}
	
	@Override
	public synchronized void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;
		
		// session management; get the right session for this service call
		final BeetRootHTTPSession currSession = this.findOrCreateHttpSession(request, response);
		
		currSession.executeForServlet(beetRootWebServer, request, response);
	}

	@Override
	public void destroy() {
		
		// save user session
		try {
			SessionManager.getInstance().save();
		} catch (Exception e) {
			LOG.error("Couldn't store user sessions!", e);
		}
		
		// clear sessions from memory
		sessions.clear(); // all we need to do here
		
		// Clear cache
		FileCacheManager.getInstance().clear();
		
		// no threads need to be stopped, no streams closed,
		// servlet container does it all for us here.
		
		super.destroy();
	}

	private BeetRootHTTPSession findOrCreateHttpSession(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// servlet container session ID
		final String sessionID = request.getSession().getId();
		
		BeetRootHTTPSession session = null; 
		if (sessions.containsKey(sessionID))
			return sessions.get(sessionID); // found !
		
        final BeetRootWebServer nano = (BeetRootWebServer) beetRootService;
        // Create a temp file manager that handles the uploads within nano mechanics
        final ITempFileManager tempFileManager = nano.getTempFileManagerFactory().create();
		// If there isn't a session yet, create one and deliver the input stream to it for parsing the body
        session = new BeetRootHTTPSession(sessionID, tempFileManager, request.getInputStream());
        
        // store it.
        sessions.put(sessionID, session);
        
        return session;		
	}
	
}
