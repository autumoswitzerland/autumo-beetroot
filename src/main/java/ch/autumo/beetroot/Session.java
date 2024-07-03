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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.nanohttpd.protocols.http.content.CookieHandler;

import ch.autumo.beetroot.utils.GUIDGenerator;
import ch.autumo.beetroot.utils.LowerCaseList;

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
	
	private final Date created;
	private long sessionRefreshTime;
	
	/**
	 * New session with given session id '__SESSION_ID__' or what is configured.
	 * 
	 * @param sessionID session id
	 */
	public Session(String sessionID) {
		
		this.sessionID = sessionID;
		this.sessionRefreshTime = System.currentTimeMillis();
		this.created = new Date(this.sessionRefreshTime);
	}
	
	/**
	 * Date and time when the session has been created.
	 * 
	 * @return creation date/time
	 */
	public Date getCreated() {
		return this.created;
	}
	
	/**
	 * Refresh session to save from timeout.
	 */
	public void refresh() {
		this.sessionRefreshTime = System.currentTimeMillis();
	}
	
	/**
	 * Checks if this session is older than the timeout.
	 * 
	 * @return true if so
	 */
	public boolean isOlderThanSessionTimeout() {
		return this.sessionRefreshTime + SessionManager.getInstance().getSessionTimeoutInMillis() < System.currentTimeMillis();
	}
	
	/**
	 * Crate a new ID pair. Remove old ID's if a pair is found associated to
	 * the original ID given. 2 ID pairs are 2 ID key/value pairs.
	 * Modify ID is the protected web GUI id.
	 * 
	 * @param origId original id (database id)
	 * @param entity entity
	 * @return newly created modify ID
	 */
	public String createIdPair(int origId, String entity) {
	
		String oldMod = getModifyId(origId, entity);
		if (oldMod != null) {
			removeIds(oldMod, entity);
		}
		
		String modifyId = GUIDGenerator.generate();
		
		data.put("origId-" + entity + "-" + modifyId, Integer.valueOf(origId));
		data.put("modifyId-" + entity + "-"+ origId, modifyId);
		
		return modifyId;
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
	 * Remove an ID pair by given modify ID and entity.
	 * 
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
	 * Use user or or user roles depending if you use the simple role 
	 * management or the full User-Role ACL. 
	 * 
	 * @param id user DB id
	 * @param name user name
	 * @param role user role
	 * @param roles user roles (comma-separated roles)
	 * @param permissions user permissions (comma-separated permissions)
	 * @param firstname first name
	 * @param lastname last name
	 * @param email email
	 * @param secretKey secret key
	 * @param twoFa 2FA?
	 */
	public void setUserData(
			int id, 
			String name, 
			String role, 
			String roles, 
			String permissions, 
			String firstname, 
			String lastname, 
			String email, 
			String secretKey, 
			boolean twoFa) {
		
		this.set("userid", Integer.valueOf(id));
		this.set("username", name);
		this.set("userrole", role);
		this.set("userroles", roles);
		this.set("userpermissions", permissions);
		
		if (firstname != null && firstname.length() != 0)
			this.set("firstname", firstname);
		if (lastname != null && lastname.length() != 0)
			this.set("lastname", lastname);

		this.set("email", email);
		this.set("secretkey", secretKey);
		this.set("two_fa", twoFa ? "1" : "0");
	}
	
	/**
	 * Clear all user data within session.
	 */
	public void clearUserData() {
		this.remove("userid");
		this.remove("username");
		this.remove("userrole");
		this.remove("userroles");
		this.remove("userpermissions");
		this.remove("firstname");
		this.remove("lastname");
		this.remove("email");
		this.remove("secretkey");
		this.remove("two_fa");
		
		this.remove("two_fa_login");
		this.remove("_2facode");
	}
	
	/**
	 * Delete all parameters in this session
	 */
	public void deleteAllParameters() {
		data.clear();
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
	 * 
	 * @param key key
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
	 * Get user roles (DB role table).
	 * @return user roles
	 */
	public List<String> getUserRoles() {
		if (BeetRootConfigurationManager.getInstance().useExtendedRoles()) {
			final String urs = (String) this.get("userroles");
			if (urs != null && urs.length() != 0) {
				final String parts[] = urs.toString().split(",");
				return LowerCaseList.asList(parts);
			} else {
				return LowerCaseList.asList(new String[] {});
			}
		} else {
			return LowerCaseList.asList(new String[] {(String) this.get("userrole")});
		}
	}
	
	/**
	 * Check if user has a role (DB role table).
	 * @paran role user role
	 * @return true, if so
	 */
	public boolean hasUserRole(String role) {
		if (BeetRootConfigurationManager.getInstance().useExtendedRoles())
			return this.getUserRoles().contains(role.toLowerCase());
		else
			return ((String)this.get("userrole")).equals(role.toLowerCase());
	}
		
	/**
	 * Get user permissions.
	 * @return user permissions
	 */
	public List<String> getUserPermissions() {
		final String up = (String) this.get("userpermissions");
		if (up != null && up.length() != 0) {
			final String parts[] = up.toString().split(",");
			return LowerCaseList.asList(parts);
		}
		return LowerCaseList.asList(new String[] {});
	}

	/**
	 * Check if user has a permission (DB role table).
	 * @paran perm user permission
	 * @return true, if so
	 */
	public boolean hasUserPermission(String perm) {
		return this.getUserPermissions().contains(perm.toLowerCase());
	}
	
	/**
	 * Get user name.
	 * @return user name
	 */
	public String getUserName() {
		return (String) this.get("username");
	}

	/**
	 * Get user email.
	 * @return user email
	 */
	public String getUserEmail() {
		return (String) this.get("email");
	}

	/**
	 * Get user secret key.
	 * @return user secret key
	 */
	public String getUserSecretKey() {
		return (String) this.get("secretkey");
	}
	
	/**
	 * User has 2FA?
	 * @return user has 2FA?
	 */
	public boolean getUTwoFa() {
		return this.get("two_fa").equals("1");
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
	 * 
	 * @param lang user language (ISO code 2 length)
	 */
	public void setUserLang(String lang) {
		this.set("userlang", lang);
	}
	
	/**
	 * Set 2FA state to <code>true</code>.
	 */
	public void setTwoFaLogin() {
		this.set("two_fa_login", "true");
	}
	
	/**
	 * Is 2FA state set?
	 * @return true is so
	 */
	public boolean isTwoFaLoginOk() {
		
		final Object ok = this.get("two_fa_login");
		if (ok == null)
			return false;
		
		return Boolean.valueOf(ok.toString());
	}
	
	/**
	 * Reset 2FA state.
	 */
	public void resetTwoFaLogin() {
		this.remove("two_fa_login");
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
	 * 
	 * @param token CSRF token
	 */
	public void setFormCsrfToken(String token){
		
		data.put("_csrfToken", token);
	}

	/**
	 * Get map value.
	 * @param mapKey map key
	 * @param key key
	 * @return value
	 */
	@SuppressWarnings("unchecked")
	public Serializable getMapValue(String mapKey, String key) {
		
		final Serializable obj = data.get("map."+mapKey);
		if (obj instanceof ConcurrentHashMap && obj != null) {
			final ConcurrentHashMap<String, Serializable> map = (ConcurrentHashMap<String, Serializable>) obj;
			return map.get(key);
		}
		
		return null;
	}
	
	/**
	 * Set a key/value pair to a map.
	 * This will internally create the map first if non-existent.
	 * @param mapKey map key
	 * @param key key
	 * @param value add a value to the map and key
	 */
	@SuppressWarnings("unchecked")
	public void setMapValue(String mapKey, String key, String value){
		
		ConcurrentHashMap<String, Serializable> map = null;
		
		final Serializable obj = data.get("map."+mapKey);
		if (obj instanceof ConcurrentHashMap) {
			map = (ConcurrentHashMap<String, Serializable>) obj;
		} else if (obj == null) {
			map = new ConcurrentHashMap<String, Serializable>();
			data.put("map."+mapKey, map);
		}
		map.put(key, value);
	}

	/**
	 * Remove a specific map key.
	 * @param mapKey map key
	 */
	public void removeMap(String mapKey){
		data.remove("map."+mapKey);
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

	/**
	 * Destroy session by given cookie handler holding the '__SESSION_UD__'
	 * and delete stored session.
	 * @param cookies cookie handler
	 */
	public void destroyDelete(CookieHandler cookies) {
		SessionManager.getInstance().destroyDelete(sessionID, cookies);
	}
	
	/**
	 * Set internal generated 2FA code.
	 * 
	 * @param genCode generated 2FA code
	 */
	public void setInternalTOTPCode(String genCode) {
		data.put("_2facode", genCode);
	}

	/**
	 * Get internal generated 2FA code.
	 * 
	 * @return internal generated 2FA code
	 */
	public String getInternalTOTPCode() {
		return (String) data.get("_2facode");
	}
	
	/**
	 * Delete internal generated 2FA code from session.
	 */
	public void clearInternalTOTPCode() {
		data.remove("_2facode");
	}
	
}
