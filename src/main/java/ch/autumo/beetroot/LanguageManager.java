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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

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
	
	private static LanguageManager instance = null;	
	
    private static final Map<String, ResourceBundle> bundles = new ConcurrentHashMap<String, ResourceBundle>();
    private static final Map<String, ResourceBundle> template_bundles = new ConcurrentHashMap<String, ResourceBundle>();

    private static ResourceBundle defaultTrans = null;
    private static ResourceBundle defaultTransTmpl = null;
    
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

        	// Context
        	final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
        	
        	// 0. Configuration languages
        	langs = BeetRootConfigurationManager.getInstance().getSepValues("web_languages");
        	if (langs.length > 0)
        		defaultLang = langs[0];
        	if (langs.length == 0)
        		defaultLang = "en";
        	
        	
        	// 1. Default language bundles
        	ClassLoader loader = null;
        	ClassLoader tmplLoader = null;
        	final Locale defaultLocale = new Locale(DEFAULT_LOCALE_NAME);
        	for (int i = 0; i < langs.length; i++) {
            	
            	// a. Within servlets
            	if (context != null) {
        			URI uri = new File(Web.getRealPath(context) + BUNDLE_LOC).toURI();
            		URL urls[] = null;
            		try {
            			urls = new URL[]{ uri.toURL()};
            		} catch (MalformedURLException e) {
            			throw new RuntimeException("Cannot get resource bundles within servlet context extra loader!", e);
    				}
            		loader = new URLClassLoader(urls);
                	defaultTrans = ResourceBundle.getBundle("lang", defaultLocale, loader);
            		if (defaultTrans == null)
                    	defaultTrans = ResourceBundle.getBundle(BUNDLE_LOC + "lang", defaultLocale, Thread.currentThread().getContextClassLoader());

                	if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
                		tmplLoader = new URLClassLoader(urls);
    	            	defaultTransTmpl = ResourceBundle.getBundle("tmpl_lang", defaultLocale, tmplLoader);
    	        		if (defaultTransTmpl == null)
    	        			defaultTransTmpl = ResourceBundle.getBundle(BUNDLE_LOC + "tmpl_lang", new Locale("default"), Thread.currentThread().getContextClassLoader());
                	}
            	// b. Within server
            	} else {
                	defaultTrans = ResourceBundle.getBundle(BUNDLE_LOC + "lang", defaultLocale);
                	if (BeetRootConfigurationManager.getInstance().translateTemplates())
                		defaultTransTmpl = ResourceBundle.getBundle(BUNDLE_LOC + "tmpl_lang", defaultLocale);
            	}				
			}
        	
        	
        	// 2. Bundles for languages, we do not want to let RB's determine languages
        	//    for bundles in case a language is not present; this only works well
        	//    on desktop where a desktop locale (fallback) makes sense!
        	
        	ResourceBundle bundle = null;
        	ResourceBundle bundleTmpl = null;
        	for (int i = 0; i < langs.length; i++) { // configuration languages
				
        		// NOTICE: Not country-specific supported.
        		final Locale locale = new Locale(langs[i]);
        		
        		// a. Within servlets
        		if (context == null) {
        			try {
	        			bundle = ResourceBundle.getBundle(BUNDLE_LOC + "lang", locale,
					    				ResourceBundle.Control.getNoFallbackControl(
					    				        ResourceBundle.Control.FORMAT_DEFAULT));
					} catch (MissingResourceException e) {
						LOG.warn("Language '"+langs[i]+"' has been configured, but no standard translation file found, default -> translations file will be used!");
					}
        			try {
                    	if (BeetRootConfigurationManager.getInstance().translateTemplates())
                    		bundleTmpl = ResourceBundle.getBundle(BUNDLE_LOC + "tmpl_lang", locale,
                    				ResourceBundle.Control.getNoFallbackControl(
                    				        ResourceBundle.Control.FORMAT_DEFAULT));
					} catch (MissingResourceException e) {
						LOG.warn("Language '"+langs[i]+"' has been configured, but no template translation file found -> default translations file will be used!");
					}
        		// b. Within server
        		} else {
        			try {
	                	bundle = ResourceBundle.getBundle("lang", locale, loader,
			    				ResourceBundle.Control.getNoFallbackControl(
			    				        ResourceBundle.Control.FORMAT_DEFAULT));
					} catch (MissingResourceException e) {
						 // No issue yet!
					}
                	if (bundle == null) {
            			try {
	            			bundle = ResourceBundle.getBundle(BUNDLE_LOC + "/lang", locale, Thread.currentThread().getContextClassLoader(),
						    				ResourceBundle.Control.getNoFallbackControl(
						    				        ResourceBundle.Control.FORMAT_DEFAULT));
    					} catch (MissingResourceException e) {
    						LOG.warn("Language '"+langs[i]+"' has been configured, but no standard translation file found -> default translations file will be used!");
    					}
                	}
        			try {
	                	if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
		                	bundleTmpl = ResourceBundle.getBundle("tmpl_lang", locale, tmplLoader,
							    				ResourceBundle.Control.getNoFallbackControl(
							    				        ResourceBundle.Control.FORMAT_DEFAULT));
	                	}
					} catch (MissingResourceException e) {
						 // No issue yet!
					}
                	if (bundleTmpl == null) {
            			try {
	                		bundleTmpl = ResourceBundle.getBundle(BUNDLE_LOC + "/tmpl_lang", locale, Thread.currentThread().getContextClassLoader(),
							    				ResourceBundle.Control.getNoFallbackControl(
							    				        ResourceBundle.Control.FORMAT_DEFAULT));
						} catch (MissingResourceException e) {
							LOG.warn("Language '"+langs[i]+"' has been configured, but no template translation file found -> default translations file will be used!");
						}
                	}
        		}
        		
        		
        		// 3. Assign found language or default; default is what is configured!
        		if (bundle == null)
        			bundles.put(langs[i], defaultTrans);
        		else
        			bundles.put(langs[i], bundle);
            	
            	if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
            		if (bundleTmpl == null)
            			template_bundles.put(langs[i], defaultTransTmpl);
            		else
                		template_bundles.put(langs[i], bundleTmpl);
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
		ResourceBundle bundle = template_bundles.get(lang);
		String text = null;
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	LOG.info("No template translation for key '"+key+"' found! trying default language '"+DEFAULT_LANG+"'.");
	    	bundle = template_bundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
		    	LOG.info("No template translation for key '"+key+"' for default language found! trying default translations 'lang_default.properties'.");
		    	try {
					text = defaultTransTmpl.getString(key);
				} catch (Exception e3) {
			    	LOG.warn("No template translation found at all for key '"+key+"'! *Sniff*");
					return null;
				}
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
					return null;
				}
			}
		}
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
		
		ResourceBundle bundle = bundles.get(lang);
		String text = null;
		
		try {
			text = bundle.getString(key);
		} catch (Exception e) {
	    	bundle = bundles.get(DEFAULT_LANG);
	    	try {
				text = bundle.getString(key);
			} catch (Exception e2) {
		    	try {
					text = defaultTrans.getString(key);
				} catch (Exception e3) {
					text = defaultValue;
				}
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
	 * @param userSession user session
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
