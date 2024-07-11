/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.utils.Web;


/**
 * Language manager.
 */
public class LanguageManager {

	protected static final Logger LOG = LoggerFactory.getLogger(LanguageManager.class.getName());
	
	
	public static final String DEFAULT_LANG = "en";
	
	private static final String DEFAULT_LOCALE_NAME = "default";
	
	private static final String URL_LANG_TAG = ":lang";
    private static final String BUNDLE_LOC = "web/lang/";
	
	private static final String LANG_GRP_APP = "app";
	private static final String LANG_GRP_TMPL = "template";
	private static final String LANG_GRP_PW = "pw";
	
	private static final Map<String, String> GROUPS = new HashMap<>();
	static {
		GROUPS.put(LANG_GRP_APP, "lang");
		GROUPS.put(LANG_GRP_TMPL, "tmpl_lang");
		GROUPS.put(LANG_GRP_PW, "pw");
	}
	private static final Map<String, ResourceBundle> DEF_BUNDLE_PER_GROUP = new HashMap<>();
	private static final Map<String, Map<String, ResourceBundle>> BUNDLE_GROUPS = new HashMap<>();

	private static String defaultLang = DEFAULT_LANG;

	private static LanguageManager instance = null;	
	
	private static String langs[] = null;
	
	
	/**
	 * Access language manager.
	 * 
	 * @return DB manager
	 */
	public static LanguageManager getInstance() {
		
        if (instance == null) {
        	
        	instance = new LanguageManager();

        	// Context
        	final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
        	
        	// 0. Configuration languages
        	langs = BeetRootConfigurationManager.getInstance().getSepValues("web_languages");
        	if (langs.length > 0)
        		defaultLang = langs[0];
        	if (langs.length == 0)
        		defaultLang = "en";

        	// Prepare a class loader for servlet context
        	ClassLoader loader = null;
        	if (context != null) {
    			final URI uri = new File(Web.getRealPath(context) + BUNDLE_LOC).toURI();
        		URL urls[] = null;
        		try {
        			urls = new URL[]{ uri.toURL()};
        		} catch (MalformedURLException e) {
        			throw new RuntimeException("Cannot get resource bundles within servlet context extra loader!", e);
				}
        		loader = new URLClassLoader(urls);
        	}
        	
        	for (Map.Entry<String, String> group : GROUPS.entrySet()) {

        		final String groupType = group.getKey();
        		
        		// All group types except translatiosn for templates, unless this is explicitely activated!
        		if (!groupType.equals(LANG_GRP_TMPL) || BeetRootConfigurationManager.getInstance().translateTemplates()) {
        		
	            	// 1. Default language bundles
		        	ResourceBundle defaultBundle = null;	        	
		        	final Locale defaultLocale = new Locale(DEFAULT_LOCALE_NAME);
		        	for (int i = 0; i < langs.length; i++) {
		            	// a. Within servlets
		            	if (context != null) {
	            			defaultBundle = ResourceBundle.getBundle(group.getValue(), defaultLocale, loader);
		            		if (defaultBundle == null)
		            			defaultBundle = ResourceBundle.getBundle(BUNDLE_LOC + group.getValue(), defaultLocale, Thread.currentThread().getContextClassLoader());
		            		DEF_BUNDLE_PER_GROUP.put(langs[i], defaultBundle);
		            	// b. Within server
		            	} else {
	                		defaultBundle = ResourceBundle.getBundle(BUNDLE_LOC + group.getValue(), defaultLocale);
	                		DEF_BUNDLE_PER_GROUP.put(langs[i], defaultBundle);
		            	}				
					}
	        	
	        	
		        	// 2. Bundles for languages, we do not want to let RB's determine languages
		        	//    for bundles in case a language is not present; this only works well
		        	//    on desktop where a desktop locale (fallback) makes sense!
		        	ResourceBundle bundle = null;
		        	for (int i = 0; i < langs.length; i++) { // configuration languages
		        		// NOTICE: Not country-specific supported.
		        		final Locale locale = new Locale(langs[i]);
		        		// a. Within servlets
		        		if (context != null) {
		        			try {
			                	bundle = ResourceBundle.getBundle(group.getValue(), locale, loader,
					    				ResourceBundle.Control.getNoFallbackControl(
					    				        ResourceBundle.Control.FORMAT_DEFAULT));
							} catch (MissingResourceException e) {
								 // No issue yet!
							}
		                	if (bundle == null) {
		            			try {
			            			bundle = ResourceBundle.getBundle(BUNDLE_LOC + group.getValue(), locale, Thread.currentThread().getContextClassLoader(),
								    				ResourceBundle.Control.getNoFallbackControl(
								    				        ResourceBundle.Control.FORMAT_DEFAULT));
		    					} catch (MissingResourceException e) {
									LOG.warn("Language '{}' has been configured, but no template translation for group '{}' file found -> default translations file will be used!", langs[i], group.getValue());
		    					}
		                	}
		        		// b. Within server
		        		} else {
		        			try {
	                    		bundle = ResourceBundle.getBundle(BUNDLE_LOC + group.getValue(), locale,
	                    				ResourceBundle.Control.getNoFallbackControl(
	                    				        ResourceBundle.Control.FORMAT_DEFAULT));
							} catch (MissingResourceException e) {
								LOG.warn("Language '{}' has been configured, but no template translation for group '{}' file found -> default translations file will be used!", langs[i], group.getValue());
							}
		        		}
	        		
	        		
	        			// 3. Assign found language or default; default is what is configured!
	        			Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(groupType);
	        			if (langBundles == null)
	        				langBundles = new HashMap<String, ResourceBundle>();
	        			
		        		if (bundle == null) {
		        			langBundles.put(langs[i], DEF_BUNDLE_PER_GROUP.get(langs[i]));
		        		}
		        		else {
		        			langBundles.put(langs[i], bundle);
		        		}
		        		
	        			BUNDLE_GROUPS.put(groupType, langBundles);
	        			
	        		}
				}
        	}
        }
        return instance;
    }

	private LanguageManager() {
	}

	/**
	 * Translate method for the template engine.
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang'; e.g. 'tmpl_lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @return translated text
	 */
	public String translateTemplate(String key, Session userSession) {
		return this.translateTemplate(key, userSession, null);
	}
	
	/**
	 * Translate method for the template engine.
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang'; e.g. 'tmpl_lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @param values place-holder values
	 * @return translated text
	 */
	public String translateTemplate(String key, Session userSession, String values[]) {
		final String lang = userSession.getUserLang();
		return translateTemplate(key, lang, values);
	}
	
	/**
	 * Translate method for the template engine.
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang'; e.g. 'tmpl_lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param lang language
	 * @param values placeholder values
	 * @return translated text
	 */	
	public String translateTemplate(String key, String lang, String values[]) {
		final Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(LANG_GRP_TMPL);
		ResourceBundle bundle = langBundles.get(lang);
		String text = null;
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	LOG.info("No template translation for key '{}' found! trying with default language '{}'.", key, DEFAULT_LANG);
	    	bundle = langBundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
		    	LOG.warn("No template translation for key '{}' for default language '{}' found!", key, DEFAULT_LANG);
				return null;
			}
		}
		return Web.escapeHtmlReserved(MessageFormat.format(text, ((Object[]) values) ));
	}

	/**
	 * Translate method for the template engine and for
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translate(String key, Session userSession, Object... arguments) {
		final String lang = userSession.getUserLang();
		return this.translate(key, lang, arguments);
	}

	/**
	 * Translate method for the template engine and for
	 * users of this framework. It HTML escapes special
	 * characters and 'Umlaute' fully.
	 * 
	 * Useful for mails.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translateFullEscape(String key, Session userSession, Object... arguments) {
		final String lang = userSession.getUserLang();
		return this.translate(key, lang, true, arguments);
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param lang language code
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translate(String key, String lang, Object... arguments) {
		return this.translate(key, lang, false, arguments);
	}
	
	private String translate(String key, String lang, boolean fullEscape, Object... arguments) {
		final Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(LANG_GRP_APP);
		ResourceBundle bundle = langBundles.get(lang);
		String text = null;
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	LOG.info("No app translation for key '{}' found! trying with default language '{}'.", key, DEFAULT_LANG);
	    	bundle = langBundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
		    	LOG.warn("No app translation for key '{}' for default language '{}' found!", key, DEFAULT_LANG);
				return null;
			}
		}
		
		if (fullEscape)
			return Web.escapeHtml(MessageFormat.format(text, arguments));
		else
			return Web.escapeHtmlReserved2(MessageFormat.format(text, arguments));
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework that returns the defaukt value
	 * if no translation is found at all.
	 * 
	 * Should only be used for special cases.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param defaultValue default value
	 * @param userSession the user session
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translateOrDefVal(String key, String defaultValue, Session userSession, Object... arguments) {
		final String lang = userSession.getUserLang();
		return this.translateOrDefVal(key, defaultValue, lang, arguments);
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework that returns the defaukt value
	 * if no translation is found at all.
	 * 
	 * Should only be used for special cases.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param defaultValue default value
	 * @param lang language code
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translateOrDefVal(String key, String defaultValue, String lang, Object... arguments) {
		final Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(LANG_GRP_APP);
		ResourceBundle bundle = langBundles.get(lang);
		String text = null;
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	bundle = langBundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
				text = defaultValue;
			}
		}
		return Web.escapeHtmlReserved2(MessageFormat.format(text, arguments));
	}
	
	/**
	 * Checks if the given language is configured.
	 * @param lang language code
	 * @return true if so
	 */
	public boolean isLangConfigured(String lang) {
		for (int i = 0; i < langs.length; i++) {
			if (langs[i].equals(lang))
				return true;
		}
		return false;
	}
	
	/**
	 * Remove language from URI.
	 * 
	 * @param uri URI
	 * @return laguage code
	 */
	public String removeLang(String uri) {
		String lang = "";
		boolean found = false;
		for (int i = 0; i < langs.length; i++) {
			if (uri.startsWith("/"+langs[i]+"/")) {
				lang = langs[i];
				found = true;	
			}
		}
		if (found)
			uri = uri.replace("/"+lang+"/", "/");
		return uri;
	}
	
	/**
	 * Get language from URI (e.g. 'en', 'de').
	 * 
	 * @param uri URI
	 * @return language code as configured
	 */
	public String parseLang(String uri) {
		String lang = null;
		for (int i = 0; i < langs.length; i++) {
			if (BeetRootConfigurationManager.getInstance().runsWithinServletContext()) {
				final String servlet = BeetRootConfigurationManager.getInstance().getServletName();
				if (uri.startsWith(servlet+"/"+langs[i]+"/") || uri.startsWith(servlet+"/"+langs[i]))
					lang = langs[i];
			} else {
				if (uri.startsWith(langs[i]+"/") || uri.startsWith(langs[i]))
					lang = langs[i];
			}
		}
		return lang;
	}

	/**
	 * Get web config resource / template based on language.
	 * Used when user is not logged in.
	 * 
	 * @param configResource language template/resource
	 * @param uri URI
	 * @return web resource
	 */
	public String getResource(String configResource, String uri) {
		final String lang = parseLang(uri);
		String res = configResource;
		if (configResource.contains(URL_LANG_TAG)) {
			if (lang != null)
				res = configResource.replace(URL_LANG_TAG, lang);
			else
				res = configResource.replace(URL_LANG_TAG+"/", "");
		}		
		return res;
	}
	
	/**
	 * Get web config resource / template based on language code
	 * Used when user is not logged in.
	 * 
	 * @param configResource language template/resource
	 * @param lang language code
	 * @return web resource
	 */
	public String getResourceByLang(String configResource, String lang) {
		String res = configResource;
		if (configResource.contains(URL_LANG_TAG)) {
			
			res = configResource.replace(URL_LANG_TAG, lang);
		}		
		return res;
	}	
	
	/**
	 * Get web config resource / template based on no language :)
	 * Used for general templates usually.
	 * 
	 * @param configResource language template/resource
	 * @param lang öanguage code
	 * @return web resource
	 */
	public String getResourceWithoutLang(String configResource, String lang) {
		String res = configResource;
		if (configResource.contains(URL_LANG_TAG+"/")) {
			
			res = configResource.replace(URL_LANG_TAG+"/", "");
		}		
		return res;
	}	
	
	/**
	 * Get web config resource / template based on language.
	 * Used when user is logged in.
	 * 
	 * @param configResource language template/resource
	 * @param userSession user session
	 * @return web resource
	 */
	public String getResource(String configResource, Session userSession) {
		String lang = null;
		String res = configResource;
		if (configResource.contains(URL_LANG_TAG)) {
			Object ul = userSession.getUserLang();
			if (ul == null) {
				lang = defaultLang;
				LOG.debug("No user language found in session, using '{}'!", defaultLang);
			} else {
				lang = userSession.getUserLang();
			}
			if (lang != null && lang.length() != 0)
				res = configResource.replace(URL_LANG_TAG, lang);
			else
				res = configResource.replace(URL_LANG_TAG+"/", "");
		}
		return res;
	}

	/**
	 * Get blocks resource / blocks template based on language.
	 * Blocks are the different basic sections of the HTML page.
	 * Return block template based on language, and if not existing
	 * try to get a general block from the 'web/html/blocks' directory
	 * instead 'web/html/&lt;lang&gt;/blocks'
	 * 
	 * Used when user is logged in.
	 * 
	 * @param configResource language template/resource
	 * @param userSession user session
	 * @return web resource
	 */
	public String getBlockResource(String configResource, Session userSession) {
		String possibleBlockResource = getResource(configResource, userSession);
		final File f = new File(possibleBlockResource);
		
		if (!f.exists()) {
			if (configResource.contains(":lang/")) {
				possibleBlockResource = configResource.replace(URL_LANG_TAG+"/", "");	
			} else {
				possibleBlockResource = configResource;
			}
		}
		return possibleBlockResource;
	}

	/**
	 * Get configured languages.
	 * 
	 * @return languages codes
	 */
	public String[] getConfiguredLanguages() {
		return langs;
	}
	
	/**
	 * Get language. First from session, then DB, 
	 * then use default language if not found earlier.
	 * @param userSession user session
	 * @return language code
	 */
	
	public String getLanguage(Session userSession) {
		String lang = userSession.getUserLang();
		if (lang == null || lang.length() == 0) {
			//from db
			lang = this.getLanguageFromDb(userSession);
		}
		return lang;
	}

	/**
	 * Get language from DB. First from DB, 
	 * then use default language if not found earlier.
	 * @param userSession user session
	 * @return language code
	 */
	public String getLanguageFromDb(Session userSession) {
		String lang = null;
		//from db
		Integer uid = (Integer) userSession.get("userid");
		
		if (uid != null) {
			try {
				lang = BeetRootDatabaseManager.getInstance().getLanguage(uid.intValue());
			} catch (Exception e) {
				LOG.warn("Cannot get language from user with id '{}'! Using default language.", uid);
			}
		}
		if (lang == null)
			lang = DEFAULT_LANG;
		return lang;
	}
	
	/**
	 * Update user language.
	 * @param newLanguage new language code
	 * @param userSession user session
	 */
	public void updateLanguage(String newLanguage, Session userSession) {
		Integer uid = (Integer) userSession.get("userid");
		if (uid != null) {
		
			try {
				BeetRootDatabaseManager.getInstance().updateLanguage(newLanguage, uid.intValue());
			} catch (Exception e) {
				LOG.warn("Cannot update user language with id '{}'!", uid);
			}
		}
	}

	/**
	 * Load translations for password checks.
	 * 
	 * @param userSession user session
	 * @return properties with validation messages
	 */
	public Properties loadPWValidationMessages(Session userSession) {
		final Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(LANG_GRP_PW);
		final ResourceBundle bundle = langBundles.get(userSession.getUserLang());
		return convertBundleToProperties(bundle);
	}

	private static Properties convertBundleToProperties(ResourceBundle resource) {
	    final Properties properties = new Properties();
	    final Enumeration<String> keys = resource.getKeys();
	    while (keys.hasMoreElements()) {
	    	final String key = keys.nextElement();
	    	properties.put(key, resource.getString(key));
		}
		return properties;
	 }
	
}
