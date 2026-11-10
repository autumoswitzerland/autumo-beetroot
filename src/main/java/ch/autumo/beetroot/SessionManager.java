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
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.nanohttpd.protocols.http.content.CookieHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.utils.Helper;


/**
 * User session manager.
 */
public class SessionManager {
	
	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(SessionManager.class.getName());
	
	// Singleton instance
	private static SessionManager instance = null;	

	// Lock for loading/saving sessions.
	private static final ReentrantLock lock = new ReentrantLock();
	
	private static final char[] HEX = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static final File SESSION_DATA = new File(Helper.USER_HOME + Helper.FILE_SEPARATOR + BeetRootConfigurationManager.getInstance().getString("ws_user_sessions"));
	private static final Random RANDOM = new Random();
	private static final int TOKEN_SIZE = 24;
	
	private static final String DEFAULT_TOKEN_COOKIE_NAME = "__SESSION_ID__";
	private static final int DEFAULT_USER_SESSION_EXPIRATION = 1; // days
	private static final int DEFAULT_USER_SESSION_TIMEOUT = 1800; // seconds
	
	private static Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	/** Listeners that are notified when a session is destroyed. */
	private static final List<SessionDestroyListener> destroyListeners = new CopyOnWriteArrayList<SessionDestroyListener>();
	
	/** Web container session id name / name of the session cookie, some java web containers use 'JSESSIONID'. */
	private static String webContainerSessionIdName = DEFAULT_TOKEN_COOKIE_NAME;
	/** How many days until the user cookie expires. */
	private static int userSessionExpirationDays = DEFAULT_USER_SESSION_EXPIRATION;
	/** Session timeout. */
	private static int userSessionTimeout = DEFAULT_USER_SESSION_TIMEOUT;
    
	private static long sessionTimeoutInMillis = -1;
	
	
	/**
	 * Private constructor.
	 */
	private SessionManager() {
	}

	/**
	 * Listener that is notified when a user session is destroyed.
	 */
	public interface SessionDestroyListener {

		/**
		 * Called after a session has been removed from the session store.
		 *
		 * @param token the destroyed session token
		 * @param session the destroyed session
		 */
		void sessionDestroyed(String token, Session session);
	}

	/**
	 * Add a session destroy listener.
	 *
	 * @param listener listener
	 */
	public static void addDestroyListener(SessionDestroyListener listener) {
		if (listener != null && !destroyListeners.contains(listener)) {
			destroyListeners.add(listener);
		}
	}

	/**
	 * Remove a session destroy listener.
	 *
	 * @param listener listener
	 */
	public static void removeDestroyListener(SessionDestroyListener listener) {
		if (listener != null) {
			destroyListeners.remove(listener);
		}
	}
	
	/**
	 * Access session manager.
	 * 
	 * @return session manager
	 */
	public static synchronized SessionManager getInstance() {
		if (instance == null) {
        	instance = new SessionManager();
	        String idname = BeetRootConfigurationManager.getInstance().getString("ws_session_id_name");
	        if (idname != null && idname.length() != 0)
	        	webContainerSessionIdName = idname;
	        userSessionExpirationDays = BeetRootConfigurationManager.getInstance().getInt("ws_session_expiration");
	        if (userSessionExpirationDays < 1)
	        	userSessionExpirationDays = DEFAULT_USER_SESSION_EXPIRATION;
	        userSessionTimeout = BeetRootConfigurationManager.getInstance().getInt("ws_session_timeout");
	        if (userSessionTimeout < 600)
	        	userSessionTimeout = 600;
	        sessionTimeoutInMillis = userSessionTimeout * 1000;
        }
        return instance;
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
	 * @param session HTTP session
	 * @return session user session
	 */
	public synchronized Session findOrCreate(BeetRootHTTPSession session) {
		final CookieHandler cookies = session.getCookies();
		String token = null;
		if (session.getExternalSessionId() != null) {
			token = session.getExternalSessionId();
		}
		else {
			token = cookies.read(webContainerSessionIdName);
		}		
		if (token == null) {
			token = this.newSessionToken();
			cookies.set(webContainerSessionIdName, token, userSessionExpirationDays);
		}
		if (!sessions.containsKey(token)) {
			sessions.put(token, new Session(token));
			//LOG.debug("New session token: "+ token);
		}
		return sessions.get(token);
	}
	
	/**
	 * Get an existing user session or null if it doesn't exist for the
	 * nano cookie. In contrast to {@link #findOrCreate(BeetRootHTTPSession)},
	 * this method never creates a session.
	 *
	 * @param session HTTP session
	 * @return session user session or null
	 */
	public Session find(BeetRootHTTPSession session) {
		String token = null;
		if (session.getExternalSessionId() != null) {
			token = session.getExternalSessionId();
		} else {
			token = session.getCookies().read(webContainerSessionIdName);
		}
		if (token == null) {
			return null;
		}
		return sessions.get(token);
	}

	/**
	 * Find a session by external session ID (token).
	 *
	 * @param token the external session ID/token
	 * @return the session or null if not found
	 */
	public Session findByExternalSessionId(String token) {
		return sessions.get(token);
	}

	/**
	 * Destroy session, though not deleted in storage file!
	 * 
	 * @param token token to destroy
	 * @param cookies nano cookie handler
	 */
	public void destroy(String token, CookieHandler cookies) {
		final Session session = sessions.remove(token);
		cookies.delete(webContainerSessionIdName);
		if (session != null) {
			for (SessionDestroyListener listener : destroyListeners) {
				try {
					listener.sessionDestroyed(token, session);
				} catch (Exception e) {
					LOG.warn("Session destroy listener failed for token '{}'.", token, e);
				}
			}
		}
	}
	
	/**
	 * Destroy session and delete in storage file!
	 * 
	 * @param token token to destroy
	 * @param cookies nano cookie handler
	 */
	public void destroyDelete(String token, CookieHandler cookies) {
		this.destroy(token, cookies);
		// session is remove from memory, write them out
		try {
			save();
		} catch (Exception e) {
			LOG.warn("Couldn't save sessions after deleting session with token: {}.", token);
		}
	}
	
	/**
	 * Load user sessions from file storage.
	 * 
	 * @throws Exception exception
	 */
	@SuppressWarnings("unchecked")
	public static void load() throws Exception {
	    lock.lock();
	    try {
	        if (!SESSION_DATA.exists())
	            return;
	        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SESSION_DATA))) {
	            sessions = (Map<String, Session>) ois.readObject();
	        } catch (Exception e) {
	            LOG.warn("Failed to load sessions from file storage; try deleting '{}' and restart the server.", SESSION_DATA, e);
	            throw e;
	        }
	    } finally {
	        lock.unlock();
	    }
	}
	
	/**
	 * Save user sessions to file storage.
	 * 
	 * @throws Exception exception
	 */
	public static void save() throws Exception {
	    lock.lock();
	    try {
	        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_DATA))) {
	        	final Map<String, Session> filtered = sessions.entrySet()
		                .stream()
		                .filter(entry -> entry.getValue().getUser() != null) // Only session that have an active user!
		                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	            oos.writeObject(filtered);
	        }
	    } finally {
	        lock.unlock();
	    }		
    }
	
	/**
	 * Get timeout in millis.
	 * 
	 * @return timeout in millis
	 */
	protected long getSessionTimeoutInMillis() {
		return sessionTimeoutInMillis;
	}
	
}
