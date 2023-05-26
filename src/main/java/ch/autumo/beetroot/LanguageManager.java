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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.utils.Utils;

/**
 * Language manager.
 */
public class LanguageManager {

	protected final static Logger LOG = LoggerFactory.getLogger(LanguageManager.class.getName());
	
	public static final String DEFAULT_LANG = "en";
	
	private static LanguageManager instance = null;	
	
    private static Map<String, ResourceBundle> bundles = new ConcurrentHashMap<String, ResourceBundle>();
	
    private static ResourceBundle defaultTrans = null;
    
	private static String defaultLang = DEFAULT_LANG;
	private static String langs[] = null;
	
	/**
	 * Access language manager.
	 * 
	 * @return DB manager
	 */
	public static LanguageManager getInstance() {
		
        if (instance == null) {
        	
        	instance = new LanguageManager();
        	ClassLoader loader = null;
        	
        	// default lang
        	final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
        	if (context != null) {
        		
    			final String cp = Utils.getRealPath(context) + "web/lang/";
        		
        		File file = new File(cp);
        		URI uri = file.toURI();
        		URL urls[] = null;
        		try {
        			urls = new URL[]{ uri.toURL()};
        		} catch (MalformedURLException e) {
        			throw new RuntimeException("Cannot get resource bundles within servlet context extra loader!", e);
				}
        		loader = new URLClassLoader(urls);
            	defaultTrans = ResourceBundle.getBundle("lang", new Locale("default"), loader);
            	
        		if (defaultTrans == null)
                	defaultTrans = ResourceBundle.getBundle("/web/lang/lang", new Locale("default"), Thread.currentThread().getContextClassLoader());
        		
        	} else {
            	defaultTrans = ResourceBundle.getBundle("web/lang/lang", new Locale("default"));
        	}
        	
        	// langs
        	langs = BeetRootConfigurationManager.getInstance().getSepValues("web_languages");
        	if (langs.length > 0)
        		defaultLang = langs[0];
        	
        	if (langs.length == 0)
        		defaultLang = "en";
        	
        	ResourceBundle bundle = null;

        	for (int i = 0; i < langs.length; i++) {
				
        		// NOTICE: not yet country specific supported.
        		final Locale locale = new Locale(langs[i]);
        		
        		if (context == null)
        			bundle = ResourceBundle.getBundle("web/lang/lang", locale);
        		else {
                	bundle = ResourceBundle.getBundle("lang", locale, loader);
                	if (bundle == null)
            			bundle = ResourceBundle.getBundle("/web/lang/lang", locale, Thread.currentThread().getContextClassLoader());
        		}
            	bundles.put(langs[i], bundle);
			}
        }
        return instance;
    }

	private LanguageManager() {
	}
	
	/**
	 * Translate method for the template engine and for
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'
	 *  
	 * @param key key associated to text in 'trans_xx' resources to translate
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
	 * users of this framework.
	 * 
	 * General language files are place in the directory:
	 * 'web/lang'
	 *  
	 * @param key key associated to text in 'trans_xx' resources to translate
	 * @param lang language code
	 * @param arguments the arguments to replace in the text with variables
	 * @return translated text
	 */
	public String translate(String key, String lang, Object... arguments) {
		
		ResourceBundle bundle = bundles.get(lang);
		String text = null;
		
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	LOG.info("No translation for key '"+key+"' found! trying default language '"+DEFAULT_LANG+"'.");
	    	bundle = bundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
		    	LOG.info("No translation for key '"+key+"' for default language found! trying default translations 'lang_default.properties'.");
		    	try {
					text = defaultTrans.getString(key);
				} catch (Exception e3) {
			    	LOG.warn("No translation found at all for key '"+key+"'! *Sniff*");
					return "[NO TRANSLATION FOUND]";
				}
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
		
		if (configResource.contains(":lang")) {
			if (lang != null)
				res = configResource.replace(":lang", lang);
			else
				res = configResource.replace(":lang/", "");
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
		if (configResource.contains(":lang")) {
			
			res = configResource.replace(":lang", lang);
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
		if (configResource.contains(":lang/")) {
			
			res = configResource.replace(":lang/", "");
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
		
		String lang = defaultLang;
		String res = configResource;
		
		if (configResource.contains(":lang")) {
			
			Object ul = userSession.getUserLang();
			
			if (ul == null) {
				lang = defaultLang;
				LOG.debug("No user language found in session, using '"+defaultLang+"'!");
			} else {
				lang = userSession.getUserLang();
			}
			
			if (lang != null && lang.length() != 0)
				res = configResource.replace(":lang", lang);
			else
				res = configResource.replace(":lang/", "");
		}
		return res;
	}

	/**
	 * Get blocks resource / blocks template based on language.
	 * Blocks are the different basic sections of the HTML page.
	 * Return block template based on language, and if not existing
	 * try to get a general block from the 'web/html/blocks' directory
	 * instead 'web/html/<lang>/blocks'
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
				possibleBlockResource = configResource.replace(":lang/", "");	
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
	 * Get translations from a default translation file, if any.
	 * @param key key
	 * @return default translation
	 */
	public String getFromDefaultTrasnlations(String key) {
		return defaultTrans.getString(key);
	}
	
	/**
	 * Get language. First from session, then DB, 
	 * then use default language if not found earlier.
	 * @param userSession user session
	 * @return language code
	 */
	
	public String getLanguage(Session userSession) {
		
		String lang = (String) userSession.getUserLang();
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
				LOG.warn("Cannot get language from user with id '"+uid.toString()+"'! Using default language.");
			}
		}
		
		if (lang == null)
			lang = DEFAULT_LANG;
		
		return lang;
	}
	
	/**
	 * Update user language.
	 * @param newLanguage new language code
	 * @param userSession
	 */
	public void updateLanguage(String newLanguage, Session userSession) {
		
		Integer uid = (Integer) userSession.get("userid");
		if (uid != null) {
		
			try {
				BeetRootDatabaseManager.getInstance().updateLanguage(newLanguage, uid.intValue());
			} catch (Exception e) {
				
				LOG.warn("Cannot update user language with id '"+uid.toString()+"'!");
			}
		}
	}
	
}
