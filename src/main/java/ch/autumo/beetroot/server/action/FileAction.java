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
package ch.autumo.beetroot.server.action;

/**
 * Server file action.
 */
public class FileAction {
	
	private String fileName = null;
	private String domain = null;

	public FileAction(String fileName, String domain) {
		this.fileName = fileName;
		this.domain = domain;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getDomain() {
		return domain;
	}
	
}
