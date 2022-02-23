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
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.nanohttpd.protocols.http.content.CookieHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User session manager.
 */
public class SessionManager {
	
	protected final static Logger LOG = LoggerFactory.getLogger(SessionManager.class.getName());
	
	private static SessionManager instance = null;	
	
	private static final char[] HEX = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static final File SESSION_DATA = new File(Utils.USER_HOME + Utils.FILE_SEPARATOR +ConfigurationManager.getInstance().getString("ws_user_sessions"));
	private static final Random RANDOM = new Random();
	private static final String TOKEN_COOKIE = "__SESSION_ID__";
	private static final int TOKEN_SIZE = 24;
	
	private static Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
	
	/**
	 * Access session manager.
	 * 
	 * @return session manager
	 */
	public static SessionManager getInstance() {
        if (instance == null)
        	instance = new SessionManager();
 
        return instance;
    }
	
	private SessionManager() {		
	}
	
	/**
	 * Generate a new session token ID.
	 * 
	 * @return session token ID
	 */
	private String genSessionToken() {
		
		final StringBuilder sb = new StringBuilder(TOKEN_SIZE);
		for (int i = 0; i < TOKEN_SIZE; i++){
			sb.append(HEX[RANDOM.nextInt(HEX.length)]);
		}
		return sb.toString();
		
	}
	
	/**
	 * Get new user sesion token.
	 * 
	 * @return user session token
	 */
	private String newSessionToken() {
		
		String token;
		
		do {
			token = this.genSessionToken();
		} while(sessions.containsKey(token));
		
		return token;
	}
	
	/**
	 * Get an existing user session or create a new one if it 
	 * doesn't exist for the nano cookie.
	 * 
	 * @param HTTP session
	 * @return user session
	 */
	public synchronized Session findOrCreate(BeetRootHTTPSession session) {
		
		final CookieHandler cookies = session.getCookies();
		
		String token = null;
		if (session.getExternalSessionId() != null)
			token = session.getExternalSessionId();
		else {
			token = cookies.read(TOKEN_COOKIE);
		}		
		if (token == null) {
			
			token = this.newSessionToken();
			cookies.set(TOKEN_COOKIE, token, 1); // RODO 1 days! longer/configurable?
		}
		
		if (!sessions.containsKey(token)) {
			
			sessions.put(token, new Session(token));
			//LOG.debug("New session token: "+ token);
		}
		
		return sessions.get(token);
	}
	
	/**
	 * Destroy session, though not deleted in sorage file!
	 * 
	 * @param token token to destroy
	 * @param cookies nano cookie handler
	 */
	public void destroy(String token, CookieHandler cookies) {
		
		sessions.remove(token);
		cookies.delete(TOKEN_COOKIE);
	}
	
	/**
	 * Load user sessions from file storage.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void load() throws Exception {
		
		if (!SESSION_DATA.exists())
			return;
		
		final FileInputStream input = new FileInputStream(SESSION_DATA);
		sessions = (HashMap<String, Session>) new ObjectInputStream(input).readObject();
		input.close();
	}
	
	/**
	 * Save user sessions to file storage.
	 * 
	 * @throws Exception
	 */
	public void save() throws Exception {
		
		final FileOutputStream output = new FileOutputStream(SESSION_DATA);
		new ObjectOutputStream(output).writeObject(sessions);
		output.close();
	}
	
}
