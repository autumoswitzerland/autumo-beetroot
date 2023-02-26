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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.security.SecureApplication;
import ch.autumo.beetroot.utils.Utils;



/**
 * Configuration manager.
 */
public class BeetRootConfigurationManager {

	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootConfigurationManager.class.getName());
	
	private static BeetRootConfigurationManager manager = null;
	private static String rootPath = null;

	private static boolean isInitialized = false;
	
	private ServletContext servletContext = null;
	protected boolean isWithinDesktop = false;
	
	private String fullConfigBasePath = null;
	
	private Properties generalProps = null;
	private boolean csrf = true;
	
	static {
    	
    	rootPath = System.getProperty("ROOTPATH");
    	
    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Utils.FILE_SEPARATOR;
    	
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
    }
	
	private BeetRootConfigurationManager() {
	}
	
	/**
	 * Get configuration manager.
	 * 
	 * @return manager
	 */
	public static BeetRootConfigurationManager getInstance() {
		
		if (manager == null) {
			manager = new BeetRootConfigurationManager();
		}
		return manager;
	}

	/**
	 * Has this configuration manager been initialized?
	 *  
	 * @return true if so, otherwise false
	 */
	public boolean isInitialized() {
		return isInitialized;
	}
	
	/**
	 * Return true, if this configuration manager runs within a servlet
	 * otherwise false.
	 * 
	 * @return true is is within servlet context
	 */
	public boolean runsWithinServletContext() {
		return servletContext != null;
	}
	
	/**
	 * Return true, if this configuration manager runs within a desktop
	 * otherwise false.
	 * 
	 * @return true is is within desktop context
	 */
	public boolean runsWithinDesktopContext() {
		return this.isWithinDesktop;
	}

	/**
	 * Returns true if it doesn't run server-side.
	 * 
	 * @return false if server-side
	 */
	public boolean isRemote() {
		return this.runsWithinDesktopContext() || runsWithinServletContext();
	}
	
	/**
	 * Update or add a value to the internal properties.
	 * 
	 * @param key key
	 * @param value vane
	 * @return old value if any or null
	 */
	public String updateProperty(String key, String value) {
		if (this.generalProps == null || !isInitialized) {
			LOG.error("Internal properties or configuration manager not initialised!");
			throw new RuntimeException("Internal properties or configuration manager not initialised!");
		}
		return (String) this.generalProps.put(key, value);
	}
	
	/**
	 * Initialize with path 'ROOTPATH/<given-path-and-file>'.
	 * No resource paths!
	 * 
	 * @param relativePath relative path
	 * @throws Exception
	 */
	public void initialize(String relativePath) throws Exception {
		this.initializeWithFullPath(rootPath + relativePath);
	}
	
	/**
	 * Initialize with standard configuration path 'ROOTPATH/cfg/beetroot.cfg'.
	 * No resource paths!
	 * 
	 * @throws Exception
	 */
	public void initialize() throws Exception {
		this.initializeWithFullPath(rootPath + Constants.CONFIG_PATH + Constants.GENERAL_SRV_CFG_FILE);
	}

	/**
	 * Initialize with absolute path.
	 * Resource path works too!
	 * 
	 * @param absolutePath absolute path
	 * @param servletContext true, if it runs in a servlet context
	 * @throws Exception
	 */
	public void initializeWithFullPath(String absolutePath, ServletContext servletContext) throws Exception {
		this.initializeWithFullPath(absolutePath);
		this.servletContext = servletContext;
	}
	
	/**
	 * Customized initialization with specific full configuration file path.
	 * Resource path works too!
	 * 
	 * @param configFilePath full path to specific full configuration file path
	 * @throws Exception
	 */
	public synchronized void initializeWithFullPath(String configFilePath) throws Exception {
		
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of configuration manager is called more than once!");
    		return;
		}
		
		if (servletContext == null) {
		
	    	if (rootPath == null || rootPath.length() == 0) {
	    		
	    		LOG.error("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
	    	}
		    	
			// check root path
	    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
	    		rootPath += Utils.FILE_SEPARATOR;
		    
			final File dir = new File(rootPath);
			if (!dir.exists() || !dir.isDirectory()) {
				
				LOG.error("Specified '-DROOTPATH' is invalid! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
			}		
		}
		
		// read general config
		generalProps = new Properties();
		// read general config
		final String file = configFilePath;
		
		final File f = new File(file);
		if (f.exists()) {
			// Get path only
			fullConfigBasePath = f.getParent();
			if (!fullConfigBasePath.endsWith(Utils.FILE_SEPARATOR))
				fullConfigBasePath += Utils.FILE_SEPARATOR;
		} else {
			fullConfigBasePath = file; // resource: don't add any file separators! Could mix things up, e.g., "/\"
		}
		
		try {
			
			if (f.exists())
				generalProps.load(new FileInputStream(file));
			else
				generalProps.load(BeetRootConfigurationManager.class.getResourceAsStream(file));
			
		} catch (IOException e) {
			
			LOG.error("Couldn't read general server configuration '" + file + "' !", e);
			throw new Exception("Couldn't read general server configuration '" + file + "' !");
		}
		
		isInitialized = true;
	}
	
	/**
	 * Initialize with desktop configuration which must have been created
	 * beforehand by the desktop application!
	 *  
	 * @param desktopCfgFile only the file name without path, e.g. 'myapp.cfg'
	 * @param appName application name
	 * @throws Exception
	 */
	public synchronized void initializeDesktop(String desktopCfgFile, String appName) throws Exception {
		
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of configuration manager is called more than once!");
    		return;
		}
		
		this.isWithinDesktop = true;
		
		final String path = Utils.getDesktopPropertiesPath(appName);
		final String filePath = path + desktopCfgFile;
        final File f = new File(filePath);
        Properties p = null;
        if (f.exists()) {
            p = new Properties();
            try {
                final FileInputStream fis = new FileInputStream(f);
                p.load(fis);
                fis.close();
            } catch (IOException ex) {
    			LOG.error("Couldn't read general desktop configuration '" + path + "' !", ex);
    			throw new Exception("Couldn't read general desktop configuration '" + path + "' !");
            }
            this.generalProps = p;
        } else {
			LOG.error("Couldn't read general desktop configuration '" + path + "', file doesn't exist !");
			throw new Exception("Couldn't read general desktop configuration '" + path + "', file doesn't exist !");
        }
        
		// set full path
		fullConfigBasePath = path;
        
		// At last
		isInitialized = true;
	}
	
	/**
	 * Returns the full base path, where the base configuration is.
	 * 
	 * @return base path
	 */
	public String getFullConfigBasePath() {
		return fullConfigBasePath;
	}
	
	/**
	 * Set if CSRF should be used.
	 * 
	 * @param csrf true if CSRF should be used
	 */
	public void setCsrf(boolean csrf) {
		this.csrf = csrf;
	}
	
	/**
	 * Use CSRF?
	 * @return true true if CSRF should be used
	 */
	public boolean useCsrf() {
		return this.csrf;
	}
	
	/**
	 * Get app root path.
	 * 
	 * @return root path
	 */
	
	public String getRootPath() {
		return rootPath;
	}
	
	/**
	 * Get a string value.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public String getString(String key) {
		
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();

		if (v == null)
			LOG.warn("Value for key '"+key+"' doesn't exist in beetroot configuration!");
		
		return v;
	}

	/**
	 * Get a string value or default value if non-existent.
	 * 
	 * @param key key
	 * @param defaultVal default value
	 * @return value
	 * @throws Exception
	 */
	public String getString(String key, String defaultVal) {
		
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		if (v == null)
			return defaultVal;
		return v;
	}
	
	/**
	 * Get a string value; no warning if value is not available.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public String getStringNoWarn(String key) {
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		return v;
	}
	
	/**
	 * Get all keys starting with a specific key-prefix.
	 * 
	 * @param keyPrefix key prefix, e.g. 'dispatcher_'
	 * @return collected values
	 */
	public String[] getKeys(String keyPrefix) {
		
		final List<String> collectedKeys = new ArrayList<>();
		final Set<Object> keys = generalProps.keySet();
		for (Iterator<Object> iterator = keys.iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			if (key.startsWith(keyPrefix))
				collectedKeys.add(key);
		}
		return collectedKeys.toArray(new String[collectedKeys.size()]);
	}
	
	/**
	 * Get all values starting with a specific key-prefix.
	 * 
	 * @param keyPrefix key prefix, e.g. 'dispatcher_'
	 * @return collected values
	 */
	public String[] getValues(String keyPrefix) {
		
		final List<String> collectedVals = new ArrayList<>();
		final Set<Object> keys = generalProps.keySet();
		for (Iterator<Object> iterator = keys.iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			if (key.startsWith(keyPrefix))
				collectedVals.add(generalProps.getProperty(key));
		}
		return collectedVals.toArray(new String[collectedVals.size()]);
	}
	
	/**
	 * Get integer value.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public int getInt(String key) {
		
		String v = generalProps.getProperty(key);
		
		if (v == null || v.length() == 0) {
			LOG.warn("Value for key '"+key+"' doesn't exist in beetroot configuration!");
			return -1;
		}
		
		return Integer.valueOf(v);
	}

	/**
	 * Get integer value.
	 * 
	 * @param key key
	 * @param defaultVal default value or default value if non-existent.
	 * @return value
	 * @throws Exception
	 */
	public int getInt(String key, int defaultVal) {
		String v = generalProps.getProperty(key);
		if (v == null || v.length() == 0)
			return defaultVal;
		return Integer.valueOf(v);
	}
	
	/**
	 * Get integer value; no warning if value is not available.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public int getIntNoWarn(String key) {
		String v = generalProps.getProperty(key);
		if (v == null || v.length() == 0) {
			return -1;
		}
		return Integer.valueOf(v);
	}
	
	/**
	 * Get yes (true) or no (false), if the configuration is messed up false
	 * is returned.
	 * 
	 * @param key key
	 * @return true or false
	 */
	public boolean getYesOrNo(String key) {
		String val = generalProps.getProperty(key);
		if (val == null || val.length() == 0) {
			LOG.warn("Value for yes/no key '"+key+"' doesn't exist in beetroot configuration!");
			return false;
		}
		val = val.trim();
		return val.toLowerCase().equals(Constants.YES);
	}	

	/**
	 * Get yes (true) or no (false), if the configuration is messed up false
	 * is returned. No warning if key is missing.
	 * 
	 * @param key key
	 * @return true or false
	 */
	public boolean getYesOrNoNoWarn(String key) {
		String val = generalProps.getProperty(key);
		if (val == null || val.length() == 0)
			return false;
		val = val.trim();
		return val.toLowerCase().equals(Constants.YES);
	}	
	
	/**
	 * Decode encrypted value, if it is encrypted by ifacex standards!
	 * See 'encoder.sh'.
	 * 
	 * @param key key
	 * @param app ifacex secure application
	 * @return encrypted value
	 * @throws Exception
	 */
	public String getDecodedString(String key, SecureApplication app) throws Exception {
		
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		
		return Utils.decode(v, app);
	}
	
	/**
	 * Get web app roles.
	 * @return web app roles
	 */
	public String[] getAppRoles() {

		return getSepValues("web_roles");
	}

	/**
	 * Get comma-separated values, e.g. 'a,b,c'.
	 * @param key key
	 * @return values
	 */
	public String[] getSepValues(String key) {
		
		String v1 = generalProps.getProperty(key);
		
		if (v1 == null || v1.length() == 0) {
			
			LOG.warn("There are no separated values (or fields configured) for key '" + key + "' !");
			return new String[0];
		}
		
		String v2[] = v1.split(",");
		String res[] = new String[v2.length];
		for (int j = 0; j < v2.length; j++) {
			res[j] = v2[j].trim();
		}
		return res;
	}

	/**
	 * Get allowed mime types.
	 * @param key mime type key
	 * @return allowed mime types
	 */
	public List<String> getMimeTypes(String key) {
		
		final String mimes = generalProps.getProperty(key);
		if (mimes == null || mimes.trim().length() == 0) {
			LOG.warn("There are no mime types for key '" + key + "' ! This will create errors...");
			return Collections.emptyList();
		}
		final String arr[] = mimes.trim().split(" ");
		/**
		for (int i = 0; i < arr.length; i++) {
			System.err.println(key+": "+arr[i]);
		}*/
		return Arrays.asList(arr);
	}
	
	/**
	 * Get servlet context
	 * @return servlet context or null
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}

	/**
	 * Get servlet name if any (only in servlet context)
	 * @return servlet name
	 */
	public String getServletName() {
		
		String servletName = generalProps.getProperty("web_html_ref_pre_url_part");
		if (servletName != null)
			servletName = servletName.trim();
		return servletName;
	}
	
}
