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

import ch.autumo.beetroot.BeetRootConfigurationManager;

/**
 * Mailer factory.
 */
public class MailerFactory {

	private static Mailer mailer;
	
	/**
	 * Get mailer (jakarta or javax).
	 * 
	 * @return mailer
	 * @throws Exception exception
	 */
	public static Mailer getInstance() throws Exception {
		
		if (mailer == null) {
			final String impl = BeetRootConfigurationManager.getInstance().getString("mail_implementation");
			if (impl == null || impl.length() == 0)
				mailer = new JakartaMailer();
			else if (impl.equalsIgnoreCase("jakarta"))
				mailer = new JakartaMailer();
			else if (impl.equalsIgnoreCase("javax"))
				mailer = new JavaxMailer();
			else
				mailer = new JakartaMailer();
		}
		
		return mailer;
	}	
}
