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
package ch.autumo.beetroot.utils.database;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.utils.UtilsException;

/**
 * H2 database URl for further processing.
 */
public final class H2Url {

	protected static final Logger LOG = LoggerFactory.getLogger(H2Url.class.getName());
	
	/** The H2 URL separator separates the URL from the features. */
	public static final String URL_SEPARATOR = ";";

	/** MODE feature. */
	public final static String FEAT_MODE = "MODE";
	/** DATABASE_TO_LOWER feature. */
	public final static String FEAT_DATABASE_TO_LOWER = "DATABASE_TO_LOWER";
	/** CASE_INSENSITIVE_IDENTIFIERS feature. */
	public final static String FEAT_CASE_INSENSITIVE_IDENTIFIERS = "CASE_INSENSITIVE_IDENTIFIERS";
	/** NON_KEYWORDS feature. */
	public final static String FEAT_NON_KEYWORDS = "NON_KEYWORDS";

	/** MODE feature default value. */
	public final static String DEF_VAL_MODE = "MySQL";
	/** DATABASE_TO_LOWER feature default value. */
	public final static String DEF_VAL_DB2LOWER = "TRUE";
	/** CASE_INSENSITIVE_IDENTIFIERS feature default value. */
	public final static String DEF_VAL_CASE = "TRUE";
	/** KEYWORDS feature default value. */
	public final static String DEF_VAL_NON_KEYWORDS = "SECOND,MINUTE,HOUR,DAY,WEEK,MONTH,YEAR";

	private Map<String, String> defaultFeatures = new HashMap<>();
	
	private String originalUrl = null;
	private String features = null;
	private String urlNoFeatures = null;
	private String url = null;
	
	
	/**
	 * Constructor with original URL as configured.
	 * 
	 * @param originalUrl originalUrl
	 * @throws UtilsException exception when the H2 URL cannot be created
	 */
	public H2Url(String originalUrl) throws UtilsException {
		this.originalUrl= originalUrl;
		defaultFeatures.put(FEAT_MODE, DEF_VAL_MODE);
		defaultFeatures.put(FEAT_DATABASE_TO_LOWER, DEF_VAL_DB2LOWER);
		defaultFeatures.put(FEAT_CASE_INSENSITIVE_IDENTIFIERS, DEF_VAL_CASE);
		defaultFeatures.put(FEAT_NON_KEYWORDS, DEF_VAL_NON_KEYWORDS);
		this.enrichUrl();
	}
	
	private void enrichUrl() throws UtilsException {
		
		final String parts[] = this.originalUrl.split(URL_SEPARATOR, 2);
		if (parts.length == 0)
			throw new UtilsException("H2 URL is malformed! URL: '" + this.originalUrl + "'.");
		
		urlNoFeatures = parts[0].trim();
		if (parts.length == 2) {
			final String features[] = parts[1].split(URL_SEPARATOR);
			for (int i = 0; i < features.length; i++) {
				final String kv[] = features[i].split("=");
				if (kv.length != 2) 
					throw new UtilsException("H2 URL feature is malformed! Option: '" + Arrays.toString(kv) + "'.");
				// Overwrite features if matching in default map
				defaultFeatures.put(kv[0].trim(), kv[1].trim());
			}
		}
		
		// Undocumented configuration, if available it overwrites the default keyword values for NON_KEYWORDS
		final String nonKeywords[] = BeetRootConfigurationManager.getInstance().getSepValues("db_url_h2_non_keywords");
		final int len = nonKeywords.length;
		if (len > 0) {
			String s = "";
			for (int i = 0; i < len; i++) {
				if (i + 1 == len)
					s += nonKeywords[i];
				else
					s += nonKeywords[i] + ",";
			}
			defaultFeatures.put(FEAT_NON_KEYWORDS, s);
		}
		
		// Create new URL
		final StringBuilder newFeatures = new StringBuilder();
		final StringBuilder newUrl = new StringBuilder();
		int i = 0;
		newUrl.append(urlNoFeatures);
		for (Map.Entry<String, String> entry : defaultFeatures.entrySet()) {
			if (i > 0)
				newFeatures.append(URL_SEPARATOR);
			newUrl.append(URL_SEPARATOR);
			final String v = entry.getValue();
			if (v.length() > 0) {
				final String part = entry.getKey() + "=" + v;
				newFeatures.append(part);
				newUrl.append(part);
			}
			i++;
		}
		
		this.url = newUrl.toString();
		this.features = newFeatures.toString();
	}
	
	/**
	 * Get new enriched H2 URL.
	 * 
	 * @return new H2 URL
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Get features only.
	 * 
	 * @return features only
	 */
	public String getFeatures() {
		return this.features;
	}
	
	/**
	 * Get H2 URL without any features.
	 * 
	 * @return new H2 URL without features
	 */
	public String getUrlNoFeatures() {
		return this.urlNoFeatures;
	}

}
