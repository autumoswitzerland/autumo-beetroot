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
package ch.autumo.beetroot.handler;

/**
 * Handler that is used for custom responses, e.g. JSON outputs.
 */
public abstract class CustomResponseHandler extends BaseHandler {

	public CustomResponseHandler(String entity) {
		super(entity);
	}	
	
	@Override
	protected final boolean isCustomResponse() {
		return true;
	}
	
	@Override
	public final String getResource() {
		return null;
	}

}
