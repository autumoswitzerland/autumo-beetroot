/**
 * 
 * Copyright (c) 2025 autumo Ltd. Switzerland, Michael Gasche
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

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

import ch.autumo.beetroot.BeetRootHTTPSession;

/**
 * No content handler - HTTP 204.
 */
public final class NoContent204Handler extends NoContentAndConfigHandler {

	public NoContent204Handler(String entity) {
		super(entity);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		return null;
	}
	
	@Override
	public IStatus getStatus() {
		return Status.NO_CONTENT;
	}	
}
