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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.handler.users.User;
import ch.autumo.beetroot.utils.web.Web;


/**
 * Language manager.
 */
public class LanguageManager {

	protected static final Logger LOG = LoggerFactory.getLogger(LanguageManager.class.getName());
	
	public static final String DEFAULT_LANG = "en";
	
	private static final String URL_LANG_TAG = ":lang";
    private static final String BUNDLE_BASE_LOC = "web/lang/";
	
	private static final String LANG_GRP_APP = "app";
	private static final String LANG_GRP_TMPL = "tmpl";
	private static final String LANG_GRP_PW = "pw";
	
	private static final Map<String, String> GROUPS = new HashMap<>();
	static {
		GROUPS.put(LANG_GRP_APP,  "lang");
		GROUPS.put(LANG_GRP_TMPL, "lang");
		GROUPS.put(LANG_GRP_PW,   "lang");
	}
	private static final Map<String, ResourceBundle> DEF_BUNDLE_PER_GROUP = new HashMap<>();
	private static final Map<String, Map<String, ResourceBundle>> BUNDLE_GROUPS = new HashMap<>();

    // Pattern to match 2 or 3 character language codes only
	private static final Pattern HTTP_HEADER_LANG_PATTERN = Pattern.compile("([a-zA-Z]{2,3})(?:-[a-zA-Z]{2})?(?:;q=([0-9.]+))?");
	
	private static String defaultLang = DEFAULT_LANG;
	
	private static LanguageManager instance = null;	
	
	private static String langs[] = null;

	
	/**
	 * Private constructor.
	 */
	private LanguageManager() {
	}
	
	/**
	 * Is the language manager initialized? 
	 * @return true if so
	 */
	public static boolean isInitialized() {
		return instance != null;
	}
	
	/**
	 * Access language manager.
	 * 
	 * @return DB manager
	 */
	public static synchronized LanguageManager getInstance() {
		
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

        	// Prepare class loaders for the servlet-context
        	final Map<String, ClassLoader> loaders = new HashMap<>();
        	if (context != null) {
        		for (String key : GROUPS.keySet()) {
        			final URI uri = new File(Web.getRealPath(context) + BUNDLE_BASE_LOC + key + "/").toURI();
            		URL urls[] = null;
            		try {
            			urls = new URL[] { uri.toURL() };
            		} catch (MalformedURLException e) {
            			throw new RuntimeException("Cannot get resource bundles within servlet context extra loader!", e);
    				}
            		loaders.put(key, new URLClassLoader(urls));
        		}
        	}
        	
        	// Load defaults (as fallback) and translations for every group ( = directory name)
        	for (Map.Entry<String, String> group : GROUPS.entrySet()) {

        		// Note: group.getValue() is for every group 'lang'.
        		
        		final String groupType = group.getKey();
        		final ClassLoader loader = loaders.get(groupType);
        		
        		// All group types except translations for templates, unless this is explicitly activated!
        		if (!groupType.equals(LANG_GRP_TMPL) || BeetRootConfigurationManager.getInstance().translateTemplates()) {
        		
	            	// 1. Default language bundles (e.g. lang_default.properties, pw_default.properties, tmpl_lang_default.properties)
		        	ResourceBundle defaultBundle = null;	        	
		        	final Locale defaultLocale = Locale.getDefault();
		        	
		        	for (int i = 0; i < langs.length; i++) {
		        		
		            	// a. Within servlets
		            	if (context != null) {
	            			defaultBundle = ResourceBundle.getBundle(group.getValue(), defaultLocale, loader);
		            		if (defaultBundle == null)
		            			defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE_LOC + groupType + "/" + group.getValue(), defaultLocale, Thread.currentThread().getContextClassLoader());
		            		DEF_BUNDLE_PER_GROUP.put(langs[i], defaultBundle);
		            		
		            	// b. Within server
		            	} else {
	                		defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE_LOC + groupType + "/"  + group.getValue(), defaultLocale);
	                		DEF_BUNDLE_PER_GROUP.put(langs[i], defaultBundle);
		            	}	
		            	
					}
	        	
	        	
		        	// 2. Bundles for languages, we do not want to let RB's determine languages
		        	//    for bundles in case a language is not present; this only works well
		        	//    on desktop where a desktop locale (fallback) makes sense!
	            	//    These are the defined language bundles (e.g. lang_en.properties, pw_en.properties, tmpl_lang_en.properties)
		        	ResourceBundle bundle = null;
		        	for (int i = 0; i < langs.length; i++) { // configuration languages
		        		
		        		// NOTICE: Only language-specific supported, without countries
		        		final Locale locale = new Locale(langs[i]);
		        		
		        		// a. Within servlets
		        		if (context != null) {
		        			try {
			                	bundle = ResourceBundle.getBundle(group.getValue(), locale, loader, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
							} catch (MissingResourceException e) {
								 // No issue yet!
							}
		                	if (bundle == null) {
		            			try {
			            			bundle = ResourceBundle.getBundle(BUNDLE_BASE_LOC + groupType + "/"  + group.getValue(), locale, Thread.currentThread().getContextClassLoader(), ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
		    					} catch (MissingResourceException e) {
									LOG.warn("Language '{}' has been configured, but no template translation for group '{}' file found -> default translations file will be used!", langs[i], group.getValue(), e);
		    					}
		                	}
		                	
		        		// b. Within server
		        		} else {
		        			try {
	                    		bundle = ResourceBundle.getBundle(BUNDLE_BASE_LOC + groupType + "/"  + group.getValue(), locale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
							} catch (MissingResourceException e) {
								LOG.warn("Language '{}' has been configured, but no template translation for group '{}' file found -> default translations file will be used!", langs[i], group.getValue(), e);
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

	/**
	 * Translate method for the template engine. Translations will be
	 * HTML escaped with the following characters "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @return translated text
	 */
	public String translateTemplate(String key, Session userSession) {
		return this.translateTemplate(key, userSession, null);
	}
	
	/**
	 * Translate method for the template engine. Translations will be
	 * HTML escaped with the following characters "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
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
	 * Translate method for the template engine. Translations will be
	 * HTML escaped with the following characters "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param lang language
	 * @param values placeholder values
	 * @return translated text
	 */	
	public String translateTemplate(String key, String lang, String values[]) {
		return this.translateTemplate(key, lang, values, true);
	}

	/**
	 * Translate method for the template engine. If escape is true,
	 * Translations will be HTML escaped with the following characters
	 * "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param session HTTP session
	 * @param values place-holder values
	 * @param escape if true, basic HTML escaping is applied
	 * @return translated text
	 */
	public String translateTemplate(String key, BeetRootHTTPSession session, String values[], boolean escape) {
		String lang = session.getUserSession().getUserLang();
		if (lang == null)
			lang = this.retrieveLanguage(session);
		return translateTemplate(key, lang, values, escape);
	}
	
	/**
	 * Translate method for the template engine. If escape is true,
	 * Translations will be fully HTML.
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param session HTTP session
	 * @param values place-holder values
	 * @return translated text
	 */
	public String translateTemplateFullEscape(String key, BeetRootHTTPSession session, String values[]) {
		String lang = session.getUserSession().getUserLang();
		if (lang == null)
			lang = this.retrieveLanguage(session);
		return translateTemplateFullEscape(key, lang, values);
	}
	
	/**
	 * Translate method for the template engine. If escape is true,
	 * Translations will be HTML escaped with the following characters
	 * "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param userSession the user session
	 * @param values place-holder values
	 * @param escape if true, basic HTML escaping is applied
	 * @return translated text
	 */
	public String translateTemplate(String key, Session userSession, String values[], boolean escape) {
		final String lang = userSession.getUserLang();
		return translateTemplate(key, lang, values, escape);
	}
	
	/**
	 * Translate method for the template engine. If escape is true,
	 * Translations will be HTML escaped with the following characters
	 * "&lt;&gt;&amp;\&#39;".
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param lang language
	 * @param values placeholder values
	 * @param escape if true, basic HTML escaping is applied
	 * @return translated text
	 */	
	public String translateTemplate(String key, String lang, String values[], boolean escape) {
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
		final String formatted = MessageFormat.format(text, ((Object[]) values));
		if (escape)
			return Web.escapeHtmlReserved(formatted);
		else
			return formatted;
	}
	
	/**
	 * Translate method for the template engine. If escape is true,
	 * Translations will be fully HTML escaped.
	 * 
	 * It is internally used only.
	 * 
	 * Template language files are place in the directory:
	 * 'web/lang/tmpl'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param lang language
	 * @param values placeholder values
	 * @return translated text
	 */	
	public String translateTemplateFullEscape(String key, String lang, String values[]) {
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
		return Web.escapeHtml(MessageFormat.format(text, ((Object[]) values)));
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
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
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
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
	 * users of this framework. It HTML escapes special
	 * characters and 'Umlaute' fully.
	 * 
	 * Useful for mails.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
	 *  
	 * @param key key associated to text in translation resources
	 * @param session HTTP session
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translateFullEscape(String key, BeetRootHTTPSession session, Object... arguments) {
		String lang = session.getUserSession().getUserLang();
		if (lang == null)
			lang = this.retrieveLanguage(session);
		return this.translate(key, lang, true, arguments);
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
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
	    	LOG.info("No app translation for key '{}' found for language '{}'! trying with default language '{}'.", key, lang, DEFAULT_LANG);
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
			return MessageFormat.format(text, arguments);
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework that returns the defaukt value
	 * if no translation is found at all.
	 * 
	 * Should only be used for special cases.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
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
	 * 'web/lang/app'; e.g. 'lang_en.properties'.
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
		return MessageFormat.format(text, arguments);
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
	 * @return web resource
	 */
	public String getResourceWithoutLang(String configResource) {
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
	 * Retrieve language when user is not or possibly not logged in.
	 * 
	 * @param session beetRoot HTTP session
	 * @return found language
	 */
	public String retrieveLanguage(BeetRootHTTPSession session) {
		final Session userSession = session.getUserSession();
	    String lang = LanguageManager.getInstance().parseLang(Web.normalizeUri(session.getUri()));
		if (lang == null) {
			// From HTTP header!
			lang = LanguageManager.getInstance().getLanguageFromHttpSession(session);
		}
	    User user = userSession.getUser();
	    if (user != null)  {
	    	final String dbLang = user.getLang();
	    	// Special/initial case: We have a DB user, but no language, 
	    	// so initially set the detected language for him!
	    	if (dbLang == null) {
		    	LanguageManager.getInstance().updateLanguage(lang, userSession);
		    	user.setLang(lang);
	    	} else {
	    		lang = dbLang;
	    	}
	    }
		return lang;		
	}
	
	/**
	 * Get language from the header of the HTTP session and use it
	 * if available in beetRoot, otherwise return default language.
	 * 
	 * @param session beetRoot HTTP session
	 * @return found language
	 */
	public String getLanguageFromHttpSession(BeetRootHTTPSession session) {
		
		final String acceptLanguage = ((IHTTPSession) session).getHeaders().get("accept-language");
		
		 if (acceptLanguage == null || acceptLanguage.isEmpty()) {
			 return LanguageManager.DEFAULT_LANG;
		 } else {
		        // Split the Accept-Language header by commas to get each language part
		        final String languages[] = acceptLanguage.split(",");
		        // Map to store language and its priority (q-value)
		        final Map<String, Double> langMap = new HashMap<>();
		        for (String lang : languages) {
		            final Matcher matcher = HTTP_HEADER_LANG_PATTERN.matcher(lang.trim());
		            if (matcher.find()) {
		                final String languageCode = matcher.group(1); // Get the language code (e.g., en, fr, haw)
		                final String qValue = matcher.group(2);       // Get the q-value if present
		                double quality = (qValue != null) ? Double.parseDouble(qValue) : 1.0; // Default q-value is 1.0
		                langMap.put(languageCode, quality);
		            }
		        }
		        // Find the language with the highest q-value
		        final String headerLang = langMap.entrySet()
		                .stream()
		                .max(Map.Entry.comparingByValue()) // Sort by q-value
		                .map(Map.Entry::getKey)            // Return the language code with highest priority
		                .orElse(null);                     // Return null if no valid language code found

				if (headerLang == null)
					return LanguageManager.DEFAULT_LANG;
		        
				// Compare with application available languages
				final String langs[] = BeetRootConfigurationManager.getInstance().getSepValues("web_languages");
				for (int i = 0; i < langs.length; i++) {
					if (headerLang.equals(langs[i]))
						return headerLang;
				}
		 }
		
		return LanguageManager.DEFAULT_LANG;
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
		Integer uid = (Integer) userSession.getUserId();
		
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
		Integer uid = (Integer) userSession.getUserId();
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
	 * @param session HTTP session
	 * @return properties with validation messages
	 */
	public Properties loadPWValidationMessages(BeetRootHTTPSession session) {
		final Map<String, ResourceBundle> langBundles = BUNDLE_GROUPS.get(LANG_GRP_PW);
		String l = session.getUserSession().getUserLang();
		if (l == null)
			l = this.retrieveLanguage(session);
		final ResourceBundle bundle = langBundles.get(l);
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
