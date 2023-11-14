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
package ch.autumo.beetroot.handler;

import ch.autumo.beetroot.BeetRootHTTPSession;

/**
 * Default not-implemented handler.
 */
public class NotImplementedHandler extends BaseHandler {

	public NotImplementedHandler() {
	}
	
	@Override
	public String getEntity() {
		return "";
	}

	@Override
	public String replaceTemplateVariables(String line, BeetRootHTTPSession session) {

		if (line.contains("{$title}"))
			line = line.replace("{$title}", "There's nothing under this page! Soooo sorry...NOT!");

		if (line.contains("{$message}"))
			line = line.replace("{$message}", " ");
		
		return line;
	}

	@Override
	public  String getResource() {
		return "web/html/:lang/notimplemented.html";
	}
	
}
