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
package ch.autumo.beetroot.server.message.file;

import ch.autumo.beetroot.server.message.ClientAnswer;

/**
 * File answer: only used when the server received a file from client!
 */
public class FileAnswer extends ClientAnswer {

	/**
	 * Success constructor.
	 * 
	 * @param answer answer
	 * @param fileId file id generated server side
	 */
	public FileAnswer(String answer, String fileId) {
		super(answer, fileId);
	}
	
	/**
	 * Fail constructor.
	 * 
	 * @param answer answer (reason)
	 * @param type failure type
	 */
	public FileAnswer(String answer, int type) {
		super(answer, type);
	}
}
