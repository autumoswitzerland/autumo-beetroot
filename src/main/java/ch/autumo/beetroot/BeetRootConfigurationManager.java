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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ch.autumo.beetroot.logging.LogBuffer;
import ch.autumo.beetroot.logging.LogBuffer.LogLevel;
import ch.autumo.beetroot.security.SecureApplication;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.security.Security;
import ch.autumo.beetroot.utils.system.OS;


/**
 * Configuration manager.
 */
public class BeetRootConfigurationManager {

	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootConfigurationManager.class.getName());

	/** Application version. */
	public static String appVersion = "x.y.z";
	
	private static BeetRootConfigurationManager manager = null;
	private static String rootPath = null;

	private static boolean isInitialized = false;
	
	private ServletContext servletContext = null;
	protected boolean isWithinDesktop = false;
	
	private String fullConfigBasePath = null;
	
	private Properties generalProps = null;
	private Properties htmlInputMap = null;
	private Properties languageMap = null;
	private boolean translateTemplates = false;
	private boolean extendedRoles = true;
	private boolean csrf = true;
	
	
	static {
		// Root-path.
    	rootPath = System.getProperty("ROOTPATH");
    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Helper.FILE_SEPARATOR;
    	if (!rootPath.endsWith(Helper.FILE_SEPARATOR))
    		rootPath += Helper.FILE_SEPARATOR;
    	
		// App-Version
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
    		InputStream inputStream = classLoader.getResourceAsStream("VERSION.txt");
    		if (inputStream != null) {
    			final byte buffer[] = new byte[5];
    			final int length = inputStream.read(buffer);
    			appVersion = new String(buffer, 0, length);
    		}
		} catch (Exception e) {
			appVersion = "x.y.z";
		}
    }
	
	/**
	 * Private constructor.
	 */
	private BeetRootConfigurationManager() {
	}
	
	/**
	 * Get configuration manager.
	 * 
	 * @return manager
	 */
	public static synchronized BeetRootConfigurationManager getInstance() {
		if (manager == null) {
			manager = new BeetRootConfigurationManager();
		}
		return manager;
	}

	/**
	 * Get application version.
	 *  
	 * @return application version
	 */
	public static String getAppVersion() {
		return appVersion;
	}
	
	/**
	 * Has this configuration manager been initialized?
	 *  
	 * @return true if so, otherwise false
	 */
	public static boolean isInitialized() {
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
	 * Initialize with path 'ROOTPATH/&lt;given-path-and-file&gt;'.
	 * No resource paths!
	 * 
	 * @param relativePath relative path
	 * @throws Exception exception
	 */
	public void initialize(String relativePath) throws Exception {
		this.initializeWithFullPath(rootPath + relativePath);
	}
	
	/**
	 * Initialize with standard configuration path 'ROOTPATH/cfg/beetroot.cfg'.
	 * No resource paths!
	 * 
	 * @throws Exception exception
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
	 * @throws Exception exception
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
	 * @throws Exception exception
	 */
	public synchronized void initializeWithFullPath(String configFilePath) throws Exception {
		
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of configuration manager is called more than once!");
    		return;
		}
		
		if (servletContext == null) {
		
	    	if (rootPath == null || rootPath.length() == 0) {
	    		LogBuffer.log(LogLevel.ERROR, "Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
	    	}
		    	
			// check root path
	    	if (!rootPath.endsWith(Helper.FILE_SEPARATOR))
	    		rootPath += Helper.FILE_SEPARATOR;
		    
			final File dir = new File(rootPath);
			if (!dir.exists() || !dir.isDirectory()) {
	    		LogBuffer.log(LogLevel.ERROR, "Specified '-DROOTPATH' is invalid! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
			}		
		}
		
		generalProps = new Properties();
		String file = configFilePath;
		File f = new File(file);
		
		if (f.exists()) {
			// Get path only
			fullConfigBasePath = f.getParent();
			if (!fullConfigBasePath.endsWith(Helper.FILE_SEPARATOR))
				fullConfigBasePath += Helper.FILE_SEPARATOR;
		} else {
			int i = file.lastIndexOf("/");
			if (i != -1)
				fullConfigBasePath = file.substring(0, i + 1);
			else 
				fullConfigBasePath = file;
		}
		// fullConfigBasePath always ends with a '/'
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			if (f.exists())
				generalProps.load(fis);
			else
				generalProps.load(BeetRootConfigurationManager.class.getResourceAsStream(file));
			
		} catch (IOException e) {
    		LogBuffer.log(LogLevel.ERROR, "Couldn't read general server configuration '{}' !", file, e);
			throw new Exception("Couldn't read general server configuration '" + file + "' !");
		} finally {
			if (fis != null)
				fis.close();
		}
		
		
		// load some main props separately
		this.csrf = getYesOrNo(Constants.KEY_WS_USE_CSRF_TOKENS, Constants.YES);
		if (this.csrf)
    		LogBuffer.log(LogLevel.INFO, "CSRF activated.");
		
		this.extendedRoles = getYesOrNo(Constants.KEY_WS_USE_EXT_ROLES, Constants.YES);
		this.translateTemplates = getYesOrNo(Constants.KEY_WEB_TRANSLATIONS, Constants.NO);
		if (this.translateTemplates)
    		LogBuffer.log(LogLevel.INFO, "Web templates are translated.");

		
		// HTML Input map
		fis = null;
		final String htmlMap = getString(Constants.KEY_WEB_INPUT_MAP);
		if (htmlMap != null && htmlMap.length() != 0) {
			final File mapFile = new File(htmlMap);
			try {
				this.htmlInputMap = new Properties();
				if (mapFile.exists()) {
					fis = new FileInputStream(mapFile);
					this.htmlInputMap.load(fis);
				} else {
					final InputStream is = BeetRootConfigurationManager.class.getResourceAsStream(htmlMap);
					this.htmlInputMap.load(is);
					if (is == null)
						throw new FileNotFoundException("The HTML input map file could not be loaded during the streaming attempt.");
				}
			} catch (IOException e) {
				htmlInputMap = null;
	    		LogBuffer.log(LogLevel.ERROR, "Couldn't read additionl HTML input mapping file '{}' !", htmlMap, e);
				throw new Exception("Couldn't read additionl HTML input mapping file '" + htmlMap + "' !");
			} finally {
				if (fis != null)
					fis.close();
			}
		}
        
		
		// Languages
        InputStreamReader isr = null;
		file = fullConfigBasePath + "languages.cfg";
		f = new File(file);
		try {
			this.languageMap = new Properties();
			if (f.exists()) {
				isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
				this.languageMap.load(isr);
			} else {
				final InputStream is = BeetRootConfigurationManager.class.getResourceAsStream(file);
				if (is == null)
					throw new FileNotFoundException("Language file could not be loaded during the streaming attempt.");
				isr = new InputStreamReader(is, StandardCharsets.UTF_8);				
				this.languageMap.load(isr);
			}
		} catch (IOException e) {
			this.languageMap = null;
    		LogBuffer.log(LogLevel.WARN, "Couldn't read languages file '{}' !", file, e);
		} finally {
			if (isr != null)
				isr.close();
		}		
		
		
		isInitialized = true;
	}
	
	/**
	 * Initialize with desktop configuration which must have been created
	 * beforehand by the desktop application!
	 *  
	 * @param desktopCfgFile only the file name without path, e.g. 'myapp.cfg'
	 * @param appName application name
	 * @throws Exception exception
	 */
	public synchronized void initializeDesktop(String desktopCfgFile, String appName) throws Exception {
		
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of configuration manager is called more than once!");
    		return;
		}
		
		this.isWithinDesktop = true;
		
		final String path = Helper.getDesktopPropertiesPath(appName);
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
    			LOG.error("Couldn't read general desktop configuration '{}' !", path, ex);
    			throw new Exception("Couldn't read general desktop configuration '" + path + "' !");
            }
            this.generalProps = p;
        } else {
			LOG.error("Couldn't read general desktop configuration '{}', file doesn't exist !", path);
			throw new Exception("Couldn't read general desktop configuration '" + path + "', file doesn't exist !");
        }
        
		// set full path
		fullConfigBasePath = path;
        
		// At last
		isInitialized = true;
	}

	/**
	 * Get language.
	 * 
	 * @param langCode language code
	 * @return full language name
	 */
	public String getLanguage(String langCode) {
		if (languageMap != null) {
			return (String) languageMap.get(langCode);
		}
		return null;
	}
	
	/**
	 * Get HTML input map type.
	 * 
	 * @param columnName column name
	 * @return HTML input map type or null
	 */
	public String getHtmlInputMapType(String columnName) {
		if (htmlInputMap != null) {
			final Object val = htmlInputMap.get(columnName);
			if (val != null) {
				final String parts[] = val.toString().trim().split (",", 2);
				return parts[0].trim();
			}
		}
		return null;
	}
	
	/**
	 * Get HTML input map pattern.
	 * 
	 * @param columnName column name
	 * @return HTML input map pattern or null
	 */
	public String getHtmlInputMapPattern(String columnName) {
		if (htmlInputMap != null) {
			final Object val = htmlInputMap.get(columnName);
			if (val != null) {
				final String parts[] = val.toString().trim().split (",", 2);
				if (parts.length > 1)
					return parts[1].trim();
				else
					return null;
			}
		}
		return null;
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
	 * Translated templates?
	 * @return true if templates should be translated
	 */
	public boolean translateTemplates() {
		return this.translateTemplates;
	}
	
	/**
	 * Use extended roles?
	 * @return true if extended roles should be used
	 */
	public boolean useExtendedRoles() {
		return this.extendedRoles;
	}
	
	/**
	 * Use CSRF?
	 * @return true if CSRF should be used
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
	 * @return string value
	 */
	public String getString(String key) {
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		if (v == null)
			LOG.warn("Value for key '{}' doesn't exist in beetroot configuration!", key);
		return v;
	}

	/**
	 * Get a string value or default value if non-existent.
	 * 
	 * @param key key
	 * @param defaultVal default value
	 * @return string value
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
	 * @return string value
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
	 * @return integer value
	 */
	public int getInt(String key) {
		String v = generalProps.getProperty(key);
		if (v == null || v.length() == 0) {
			LOG.warn("Value for key '{}' doesn't exist in beetroot configuration!", key);
			return -1;
		}
		return Integer.valueOf(v);
	}

	/**
	 * Get integer value.
	 * 
	 * @param key key
	 * @param defaultVal default value or default value if non-existent.
	 * @return integer value
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
	 * @return integer value
	 */
	public int getIntNoWarn(String key) {
		String v = generalProps.getProperty(key);
		if (v == null || v.length() == 0) {
			return -1;
		}
		return Integer.valueOf(v);
	}

	/**
	 * Get yes (true) or no (false), or the default value if the 
	 * configuration is missing.
	 * 
	 * @param key key
	 * @param defaultVal default value
	 * @return true or false
	 */
	public boolean getYesOrNo(String key, String defaultVal) {
		String val = generalProps.getProperty(key);
		if (val == null || val.length() == 0) {
			return defaultVal.equalsIgnoreCase(Constants.YES);
		}
		val = val.trim();
		return val.equalsIgnoreCase(Constants.YES);
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
			LOG.warn("Value for yes/no key '{}' doesn't exist in beetroot configuration!", key);
			return false;
		}
		val = val.trim();
		return val.equalsIgnoreCase(Constants.YES);
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
		return val.equalsIgnoreCase(Constants.YES);
	}	
	
	/**
	 * Decode encrypted value, if it is encrypted by beetRoot standards!
	 * See 'encoder.sh'.
	 * 
	 * @param key key
	 * @param app secure application
	 * @return encrypted value
	 * @throws UtilsException if decoding fails
	 */
	public String getDecodedString(String key, SecureApplication app) throws UtilsException {
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		return Security.decode(v, app);
	}
	
	/**
	 * App-roles for the simple role-management
	 * (1 role per user, stored in user table).
	 * 
	 * Get web app roles.
	 * @return web app roles
	 */
	public String[] getAppRoles() {
		return getSepValues("web_roles");
	}

	/**
	 * Get comma-separated values, e.g. 'a,b,c'.
	 * If the configuration is messed up an empty
	 * array is returned. No warning if key is missing.
	 * 
	 * @param key key
	 * @return values
	 */
	public String[] getSepValuesNoWarn(String key) {
		String v1 = generalProps.getProperty(key);
		if (v1 == null || v1.length() == 0) {
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
	 * Get comma-separated values, e.g. 'a,b,c'.
	 * If no values are found an empty array
	 * is returned.
	 * 
	 * @param key key
	 * @return values
	 */
	public String[] getSepValues(String key) {
		String v1 = generalProps.getProperty(key);
		if (v1 == null || v1.length() == 0) {
			LOG.warn("There are no separated values (or fields configured) for key '{}'!", key);
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
			LOG.warn("There are no mime types for key '{}' ! This will create errors...", key);
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

	// Load XML module configuration
	//------------------------------------------------------------------------------
	
	/**
	 * Get XML module root configuration.
	 * No resource paths!
	 *  
	 * @param xmlConfigFile only the file name, path is concluded by ROOTPATH and cfg-directory
	 * @param moduleName module name
	 * @return XML doc root
	 */
	public static Document getXMLModuleConfig(String xmlConfigFile, String moduleName) {
		return getXMLModuleConfigWithFullPath(rootPath + Constants.CONFIG_PATH + xmlConfigFile, moduleName);
	}
	
	/**
	 * Get XML module root configuration.
	 * No resource paths!
	 *  
	 * @param xmlRelativePath relative path that is concluded with ROOTPATH
	 * @param moduleName module name
	 * @return XML doc root
	 */
	public static Document getXMLModuleConfigRelative(String xmlRelativePath, String moduleName) {
		// check root path
    	if (!rootPath.endsWith(OS.FILE_SEPARATOR))
    		rootPath += OS.FILE_SEPARATOR;
    	return getXMLModuleConfigWithFullPath(rootPath + xmlRelativePath, moduleName);
	}
	
	/**
	 * Get XML module root configuration. The given path can be a resource too, but make sure
	 * there are no mixed path separators, e.g. "/\"!
	 *  
	 * @param xmlConfigFilePath the full configuration file path
	 * @param moduleName module name
	 * @return XML doc root
	 */
	public static Document getXMLModuleConfigWithFullPath(String xmlConfigFilePath, String moduleName) {
		Document doc = null;
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			// optional, but recommended
			// process XML securely, avoid attacks like XML External Entities (XXE)
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// parse XML file
			final DocumentBuilder db = dbf.newDocumentBuilder();
			
			final File cfg = new File(xmlConfigFilePath);
			if (cfg.exists())
				doc = db.parse(new File(xmlConfigFilePath));
			else 
				doc = db.parse(BeetRootConfigurationManager.class.getResourceAsStream(xmlConfigFilePath));
			
			doc.getDocumentElement().normalize();
			
			final String module = doc.getDocumentElement().getNodeName();
			
			if (!module.equalsIgnoreCase(moduleName))
				throw new IllegalAccessException("Module '"+moduleName+"' is not a valid module name; here '"+module+"' would be right!");
				
		} catch (Exception e) {
			LOG.error("Couldn't load module XML configuration from '"+xmlConfigFilePath+"'!", e);
		}
		return doc;
	}
	
}
