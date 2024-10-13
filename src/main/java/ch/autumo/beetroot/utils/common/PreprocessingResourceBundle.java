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
package ch.autumo.beetroot.utils.common;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;


/**
 * Pre-processes special resource bundle entries. 
 */
public final class PreprocessingResourceBundle extends ResourceBundle {
	
    private final Map<String, Object> processedValues = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param originalBundle original resource bundle
     */
    private PreprocessingResourceBundle(ResourceBundle originalBundle) {
        // Pre-process all keys and values in the original bundle
        for (String key : originalBundle.keySet()) {
             final Object value = originalBundle.getObject(key);
             if (value instanceof String) {
            	 processedValues.put(key, preprocess(value.toString()));
             } else {
            	 processedValues.put(key, value);
             }        	
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        return processedValues.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
    	return new Enumeration<String>() {
            private final Iterator<String> iterator = processedValues.keySet().iterator();
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }
    
    @Override
    public boolean containsKey(String key) {
        return processedValues.containsKey(key);
    }    

    private Object preprocess(Object value) {
        if (value.equals("{$APP_VERSION}")) // App-Version
        	return BeetRootConfigurationManager.getAppVersion();
        else if (value.equals("{$APP_NAME}")) // App-Name
        	return BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_APP_NAME, "<UNDEFINED:"+Constants.KEY_WS_APP_NAME+">");
        
        return value;
    }

    /**
     * Create a pre-processed bundle if necessary (based on type), otherwise
     * return a standard bundle.
     * 
     * @param type bundle type
     * @param baseName base name
     * @param locale locale
     * @return pre-processed or standard bundle
     */
    public static ResourceBundle getBundle(String type, String baseName, Locale locale) {
    	final ResourceBundle rb = ResourceBundle.getBundle(baseName, locale);
    	if (rb == null)
    		return null;
    	if (type.equals("tmpl"))
    		return new PreprocessingResourceBundle(rb);
    	else
    		return rb;
	}
    
    /**
     * Create a pre-processed bundle if necessary (based on type), otherwise
     * return a standard bundle.
     * 
     * @param type bundle type
     * @param baseName base name
     * @param locale locale
     * @param loader class loader
     * @return pre-processed or standard bundle
     */
    public static ResourceBundle getBundle(String type, String baseName, Locale locale, ClassLoader loader) {
    	final ResourceBundle rb = ResourceBundle.getBundle(baseName, locale, loader);
    	if (rb == null)
    		return null;
    	if (type.equals("tmpl"))
    		return new PreprocessingResourceBundle(rb);
    	else
    		return rb;
	}

    /**
     * Create a pre-processed bundle if necessary (based on type), otherwise
     * return a standard bundle.
     * 
     * @param type bundle type
     * @param baseName base name
     * @param locale locale
     * @param control resource bundle control
     * @return pre-processed or standard bundle
     */
    public static ResourceBundle getBundle(String type, String baseName, Locale locale, Control control) {
    	final ResourceBundle rb = ResourceBundle.getBundle(baseName, locale, control);
    	if (rb == null)
    		return null;
    	if (type.equals("tmpl"))
    		return new PreprocessingResourceBundle(rb);
    	else
    		return rb;
    }
    
    /**
     * Create a pre-processed bundle if necessary (based on type), otherwise
     * return a standard bundle.
     * 
     * @param type bundle type
     * @param baseName base name
     * @param locale locale
     * @param loader class loader
     * @param control resource bundle control
     * @return pre-processed or standard bundle
     */
    public static ResourceBundle getBundle(String type, String baseName, Locale locale, ClassLoader loader, Control control) {
    	final ResourceBundle rb = ResourceBundle.getBundle(baseName, locale, loader, control);
    	if (rb == null)
    		return null;
    	if (type.equals("tmpl"))
    		return new PreprocessingResourceBundle(rb);
    	else
    		return rb;
    }
    
}
