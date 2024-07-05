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

import java.util.Iterator;
import java.util.List;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

/**
 * Simple Password Helper.
 * 
 * Only English messages atm.
 */
public class PasswordHelper {

	/**
	 * Password validator.
	 */
	private static PasswordValidator VALIDATOR = new PasswordValidator(
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

	
	/**
	 * Password valid?
	 * 
	 * @param password password
	 * @return true if so
	 */
	public static RuleResult isValid(String password) {
		return VALIDATOR.validate(new PasswordData(new String(password)));
	}

	/**
	 * Get messages for result.
	 * 
	 * @param result validation result
	 * @return result messages
	 */
	public static List<String> getMessages(RuleResult result) {
		return VALIDATOR.getMessages(result);
	}

	/**
	 * Get HTML messages.
	 * 
	 * @param result validation result
	 * @return messages as HTML string
	 */
	public static String getHTMLMessages(RuleResult result) {
		String msgs = "";
		final List<String> list = PasswordHelper.getMessages(result);
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			final String m = iterator.next();
			msgs += m + "<br>";
		}
		return msgs;
	}
	
}
