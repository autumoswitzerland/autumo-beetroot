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

import java.io.File;

/**
 * Handler response.
 */
public class HandlerResponse {

	public static final int STATE_NOT_OK = 0;
	public static final int STATE_OK = 1;
	public static final int STATE_WARNING = 2;

	public static final int TYPE_FORM = 10;
	public static final int TYPE_FILE_UPLOAD = 20;
	public static final int TYPE_FILE_DOWNLOAD = 30;
	
	private int status = -1;
	private String title = null;
	private String message = null;
	private Exception exception = null;
	private Object object = null;
	private File downloadFile = null;
	private String downloadFileMimeType = null;

	private int type = TYPE_FORM;
	private int id = -1;

	private int savedId = -1;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public HandlerResponse(int status) {
		this.status = status;
	}

	public HandlerResponse(int status, int savedId) {
		this.status = status;
		this.savedId = savedId;
	}
	
	public HandlerResponse(int status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public HandlerResponse(int status, String title, String message) {
		this.status = status;
		this.title = title;
		this.message = message;
	}
	
	public HandlerResponse(int status, String title, String message, Exception exception) {
		this.status = status;
		this.title = title;
		this.message = message;
		this.exception = exception;
	}
	
	public HandlerResponse(int status, String title, String message, Exception exception, Object object) {
		super();
		this.status = status;
		this.title = title;
		this.message = message;
		this.exception = exception;
		this.object = object;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public File getDownloadFile() {
		return downloadFile;
	}

	public void setDownloadFile(File downloadFile) {
		this.downloadFile = downloadFile;
	}

	public String getDownloadFileMimeType() {
		return downloadFileMimeType;
	}

	public void setDownloadFileMimeType(String downloadFileMimeType) {
		this.downloadFileMimeType = downloadFileMimeType;
	}

	public int getSavedId() {
		return savedId;
	}

	public void setSavedId(int savedId) {
		this.savedId = savedId;
	}
	
}
