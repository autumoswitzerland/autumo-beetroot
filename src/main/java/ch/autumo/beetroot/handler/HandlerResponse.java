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

import java.io.File;

/**
 * Handler response.
 */
public class HandlerResponse {

	public static final int STATE_NOT_OK = 0;
	public static final int STATE_OK = 1;
	public static final int STATE_WARNING = 2;
	
	//public static final int STATE_RELOAD_OK = 10;

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
