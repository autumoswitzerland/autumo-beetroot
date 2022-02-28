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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.nanohttpd.protocols.http.content.CookieHandler;

/**
 * User session.
 */
public class Session implements Serializable {
	
	/**
	 * Session serial version UID
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, Serializable> data = new ConcurrentHashMap<String, Serializable>();
	private final String sessionID;
	
	// User settings are stored in DB, not serialized in session
	private transient Map<String, String> settingsMap = null;
	
	
	/**
	 * New session with given session id '__SESSION_ID__'.
	 * 
	 * @param sessionID session id
	 */
	public Session(String sessionID) {
		
		this.sessionID = sessionID;
	}
	
	/**
	 * Crate a new ID pair. Remove old ID's if a apir si found associated to
	 * the original ID given. 2 ID pairs are 2 ID key/value pairs.
	 * Modify ID is the protected web gui id.
	 * 
	 * @param origId original id (database id)
	 * @param entity entity
	 */
	public void createIdPair(int origId, String entity){
	
		String oldMod = getModifyId(origId, entity);
		if (oldMod != null) {
			removeIds(oldMod, entity);
		}
		
		String modifyId = GUIDGenerator.generate();
		
		data.put("origId-" + entity + "-" + modifyId, Integer.valueOf(origId));
		data.put("modifyId-" + entity + "-"+ origId, modifyId);
	}
	
	public String getModifyId(int origId, String entity) {
		return (String) data.get("modifyId-" + entity + "-" + origId);
	}

	public int getOrigId(String modifyId, String entity) {
		
		Object oid = data.get("origId-" + entity + "-"+ modifyId);
		if (oid == null)
			return -1;
		
		return ((Integer) oid).intValue();
	}
	
	/**
	 * Remove an ID pair by given modify ID and entity
	 * @param modifyId modify ID
	 * @param entity entity
	 */
	public void removeIds(String modifyId, String entity) {
		final Integer origId = (Integer) data.remove("origId-" + entity + "-" + modifyId);
		if (origId != null)
			data.remove("modifyId-" + entity + "-" + origId);
	}
	
	/**
	 * Clean session from all ID pairs!
	 */
	public synchronized void removeAllIds() {
		
		final List<String> remKeys = new ArrayList<String>();
		final Set<String> keys = data.keySet();
		
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			if (key.startsWith("modifyId-"))
				remKeys.add(key);
			if (key.startsWith("origId-"))
				remKeys.add(key);
		}
		
		// delete
		for (Iterator<String> iterator = remKeys.iterator(); iterator.hasNext();) {
			final String remKey = (String) iterator.next();
			data.remove(remKey);
		}
	}

	/**
	 * Set user data.
	 * 
	 * @param id user DB id
	 * @param name user name
	 * @param role user role
	 * @param firstname first name
	 * @param lastname last name
	 */
	public void setUserData(int id, String name, String role, String firstname, String lastname) {
		
		this.set("userid", Integer.valueOf(id));
		this.set("username", name);
		this.set("userrole", role);
		this.set("firstname", firstname);
		this.set("lastname", lastname);
	}

	/**
	 * Set user settings map.
	 * @param settingsMap user settings map
	 */
	public void setUserSettings(Map<String, String> settingsMap) {
		this.settingsMap = settingsMap;
	}

	/**
	 * Get user settings map.
	 * @return user settings map
	 */
	public Map<String, String> getUserSettings() {
		return this.settingsMap;
	}

	/**
	 * Get one user setting.
	 * @return user setting
	 */
	public String getUserSetting(String key) {
		
		if (this.settingsMap == null)
			return null;
		
		return this.settingsMap.get(key);
	}
	
	/**
	 * Add or update user setting.
	 * @param key key
	 * @param value value
	 */
	public void addOrUpdateUserSetting(String key, String value) {
		this.settingsMap.put(key, value);
	}
	
	/**
	 * Delete user setting.
	 * @param key key
	 * @return old value of deleted entry
	 */
	public String deleteUserSetting(String key) {
		return this.settingsMap.remove(key);
	}
	
	/**
	 * Get user DB id
	 * @return user DB id
	 */
	public Integer getUserId() {
		return (Integer) this.get("userid");
	}
	
	/**
	 * Get user role
	 * @return user role
	 */
	public String getUserRole() {
		return (String) this.get("userrole");
	}
	
	/**
	 * Get user name
	 * @return user name
	 */
	public String getUserName() {
		return (String) this.get("username");
	}

	/**
	 * Get user language
	 * @return user language
	 */
	public String getUserLang() {
		return (String) this.get("userlang");
	}

	/**
	 * Set user language
	 * @param user language (ISO code 2 length)
	 */
	public void setUserLang(String lang) {
		this.set("userlang", lang);
	}
	
	/**
	 * Add uploaded files info .
	 * @param files uploaded file info
	 */
	public void addFiles(Map<String, String> files) {
		data.put("_uploadfiles", (Serializable)files);
	}
	
	/**
	 * Get uploaded files info
	 * @return uploaded files info
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> consumeFiles() {
		final Map<String, String> result = (Map<String, String>) data.get("_uploadfiles");
		data.remove("_uploadfiles");
		return result;
	}
	
	/**
	 * Get full user name, can be first name or last name or both
	 * and first or last name is not present, the user name.
	 *  
	 * @return full name or user name
	 */
	public String getUserFullNameOrUserName() {
		
		String fn = (String) this.get("firstname");
		String ln = (String) this.get("lastname");
		
		if (fn == null)
			fn = "";
		if (ln == null)
			ln = "";
		fn = fn.trim();
		ln = ln.trim();
		
		String full = "";
		if (fn.length() != 0)
			full += fn;
		if (ln.length() != 0) {
			if (full.length() == 0)
				full += ln;
			else
				full += " " + ln;
		}
		
		if (full.length() != 0)
			return full;
		else
			return this.getUserName();
	}
	
	/**
	 * Get CSRF token.
	 * 
	 * @return CSRF token
	 */
	public String getFormCsrfToken() {
		
		return (String) data.get("_csrfToken");
	}

	/**
	 * Set CSRF token.
	 * @param CSRF token
	 */
	public void setFormCsrfToken(String token){
		
		data.put("_csrfToken", token);
	}
	
	/**
	 * Get value.
	 * @param key key
	 * @return value
	 */
	public Serializable get(String key) {
		
		return data.get(key);
	}

	/**
	 * Set a key/value pair
	 * @param key key
	 * @param value value
	 */
	public void set(String key, Serializable value){
		
		data.put(key, value);
	}

	/**
	 * Remove a specific key.
	 * @param key key
	 */
	public void remove(String key){
		
		data.remove(key);
	}
	
	/**
	 * Destroy session by given cookie handler holding the '__SESSION_UD__'.
	 * @param cookies cookie handler
	 */
	public void destroy(CookieHandler cookies) {
		
		SessionManager.getInstance().destroy(sessionID, cookies);
	}

}
