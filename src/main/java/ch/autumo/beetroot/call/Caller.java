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
package ch.autumo.beetroot.call;

import java.util.regex.Pattern;

/**
 * Phone caller interface.
 */
public interface Caller {

	/**
	 * International phone number pattern without spaces, brackets or hyphens.
	 * Example: +41991119911
	 */
	public static final Pattern INTL_PHONE_NUMBER_PATTERN = Pattern.compile("^(\\+\\d{1,3}( )?)?\\d{10}$");
	
	/**
	 * Initialize.
	 * 
	 * @throws Exception exception
	 */
	public void init() throws Exception;
	
	/**
	 * make phone call.
	 * 
	 * @param toNumber to phone number
	 * @param text text
	 * @throws Exception exception
	 */
	public void call(String toNumber, String text) throws Exception;
	
}
