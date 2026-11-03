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
package ch.autumo.beetroot.utils;

import java.io.IOException;

/**
 *
 * Utils exception.
 *
 */
public class UtilsException extends IOException {

	private static final long serialVersionUID = 1997384039883172508L;
	
	/**
	 * The code value.
	 */
	private int code;
	/**
	 * The raw message value.
	 */
	private String rawMessage;

	/**
	 * Creates the exception.
	 *
	 * @param message the message
	 */
	public UtilsException(String message) {
		super(message);
	}

	/**
	 * Creates the exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public UtilsException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates the exception.
	 *
	 * @param message the message
	 * @param rawMessage raw message
	 */
	public UtilsException(String message, String rawMessage) {
		super(message);
		this.rawMessage = rawMessage;
	}

	/**
	 * Returns the code value.
	 *
	 * @return the code value
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the raw message value.
	 *
	 * @return the raw message value
	 */
	public String getRawMessage() {
		return rawMessage;
	}

	/**
	 * Sets the raw message value.
	 *
	 * @param rawMessage the new raw message value
	 */
	public void setRawMessage(String rawMessage) {
		this.rawMessage = rawMessage;
	}
	
}
