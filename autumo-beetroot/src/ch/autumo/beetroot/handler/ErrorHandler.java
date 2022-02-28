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
package ch.autumo.beetroot.handler;

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;


/**
 * Default error handler.
 */
public class ErrorHandler extends BaseHandler {

	private Status status = null;
	private String title = null;
	private String message = null;
	
	public ErrorHandler(Status status, String title, String message) {
		this.status = status;
		this.title = title;
		this.message = message;
	}
	
	@Override
	public String getEntity() {
		return "Error";
	}

	@Override
	public String parse(String line, BeetRootHTTPSession session) {

		if (line.contains("{$title}") && title != null && title.length() != 0)
			line = line.replace("{$title}", this.title);
		
		if (line.contains("{$message}") && title != null && title.length() != 0)
			line = line.replace("{$message}", this.message);
		else
			line = line.replace("{$message}", "");
		
		return line;
	}

	@Override
	public  String getResource() {
		return "web/html/:lang/error.html";
	}

	@Override
	public IStatus getStatus() {
		return status;
	}

	@Override
	public boolean showMenu(Session userSession) {
		return userSession.getUserRole() != null;
	}
	
}
