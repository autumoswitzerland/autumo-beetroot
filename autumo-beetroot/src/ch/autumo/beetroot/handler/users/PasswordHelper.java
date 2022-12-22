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
		int i = 0;
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			final String m = iterator.next();
			if (i == 0)
				msgs += m + "<br>";
			else
				msgs += "&emsp;&emsp;&emsp;" + m + "<br>"; // nasty.
			i++;
		}
		return msgs;
	}
	
}
