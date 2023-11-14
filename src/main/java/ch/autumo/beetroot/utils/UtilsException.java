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
	
	private int code;
	private String rawMessage;

	public UtilsException(String message) {
		super(message);
	}

	public UtilsException(String message, Throwable cause) {
		super(message, cause);
	}

	public UtilsException(String message, String rawMessage) {
		super(message);
		this.rawMessage = rawMessage;
	}

	public int getCode() {
		return code;
	}

	public String getRawMessage() {
		return rawMessage;
	}

	public void setRawMessage(String rawMessage) {
		this.rawMessage = rawMessage;
	}
	
}
