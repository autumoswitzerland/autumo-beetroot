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
package ch.autumo.beetroot.mailing;

import java.util.Map;
import java.util.regex.Pattern;

import ch.autumo.beetroot.BeetRootHTTPSession;

/**
 * Mailer interface. 
 */
public interface Mailer {

	/**
	 * Email address regex pattern.
	 */
	public static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(\\S+)$");	
	
	/**
	 * Mail. HTML + TXT templates are served.
	 * 
	 * @param to email receiver addresses
	 * @param subject subject
	 * @param variables variables to parse in templates
	 * @param templateName template name
	 * @param session HTTP session or null if not called within a servlet context (default language used)
	 * @throws Exception exception
	 */
	public void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception;

}
