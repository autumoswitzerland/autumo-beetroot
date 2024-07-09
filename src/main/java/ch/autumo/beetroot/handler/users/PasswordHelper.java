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
package ch.autumo.beetroot.handler.users;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.MessageResolver;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.PropertiesMessageResolver;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;


/**
 * Simple Password Helper.
 */
public class PasswordHelper {

	/**
	 * Password validator map.
	 */
	private static Map<String, PasswordValidator> validatorMap = new HashMap<>();
	
	
	/**
	 * Find or create a password validator for the given language.
	 * 
	 * @param userSession user session
	 * @return password validator
	 */
	private static PasswordValidator findOrCreateValidator(Session userSession) {
		PasswordValidator pwv = validatorMap.get(userSession.getUserLang());
		if (pwv == null) {
			final Properties props = LanguageManager.getInstance().loadPWValidationMessages(userSession);
			final MessageResolver resolver = new PropertiesMessageResolver(props);
			pwv = new PasswordValidator(resolver, 
					  // length between 8 and 24 characters
					  new LengthRule(8, 24),
					  // at least one upper-case character
					  new CharacterRule(EnglishCharacterData.UpperCase, 1),
					  // at least one lower-case character
					  new CharacterRule(EnglishCharacterData.LowerCase, 1),
					  // at least one digit character
					  new CharacterRule(EnglishCharacterData.Digit, 1),
					  // at least one symbol (special character)
					  new CharacterRule(EnglishCharacterData.Special, 1),
					  // define some illegal sequences that will fail when >= 5 chars long
					  // alphabetical is of the form 'abcde', numerical is '34567', qwery is 'asdfg'
					  // the false parameter indicates that wrapped sequences are allowed; e.g. 'xyzabc'
					  new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
					  new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
					  new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),
					  // no whitespace
					  new WhitespaceRule());
			validatorMap.put(userSession.getUserLang(), pwv);
		}
		return pwv;
	}
	
	/**
	 * Password valid?
	 * 
	 * @param password password
	 * @param userSession user session
	 * @return rule result
	 */
	public static RuleResult isValid(String password, Session userSession) {
		final PasswordValidator pwv = findOrCreateValidator(userSession);
		if (pwv != null)
			return pwv.validate(new PasswordData(password));
		return null;
	}

	/**
	 * Get messages for result.
	 * 
	 * @param result validation result
	 * @param userSession user session
	 * @return result messages
	 */
	public static List<String> getMessages(RuleResult result, Session userSession) {
		final PasswordValidator pwv = findOrCreateValidator(userSession);
		if (pwv != null)
			return pwv.getMessages(result);
		return null;		
	}

	/**
	 * Get HTML messages.
	 * 
	 * @param result validation result
	 * @param userSession user session
	 * @return messages as HTML string
	 */
	public static String getHTMLMessages(RuleResult result, Session userSession) {
		String msgs = "";
		final List<String> list = PasswordHelper.getMessages(result, userSession);
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			final String m = iterator.next();
			msgs += m + " ";
		}
		return msgs;
	}
	
}
