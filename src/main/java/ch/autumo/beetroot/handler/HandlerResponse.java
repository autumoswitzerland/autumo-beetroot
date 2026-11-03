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

import org.nanohttpd.protocols.http.response.Response;

/**
 * Handler response.
 */
public class HandlerResponse {

	/**
	 * Handler state: not OK.
	 */
	public static final int STATE_NOT_OK = 0;
	/**
	 * Handler state: OK.
	 */
	public static final int STATE_OK = 1;
	/**
	 * Handler state: warning.
	 */
	public static final int STATE_WARNING = 2;
	
	//public static final int STATE_RELOAD_OK = 10;

	/**
	 * Response type: form.
	 */
	public static final int TYPE_FORM = 10;
	/**
	 * Response type: file upload.
	 */
	public static final int TYPE_FILE_UPLOAD = 20;
	/**
	 * Response type: file download.
	 */
	public static final int TYPE_FILE_DOWNLOAD = 30;
	
	private int status = -1;
	private String title = null;
	private String message = null;
	private Exception exception = null;
	private Object object = null;
	private File downloadFile = null;
	private String downloadFileName = null;
	private String downloadFileMimeType = null;
	
	private Response httpResponse = null;

	private int type = TYPE_FORM;
	private int id = -1;

	private int savedId = -1;
	
	/**
	 * Handler with as status.
	 * 
	 * @param status status
	 */
	public HandlerResponse(int status) {
		this.status = status;
	}

	/**
	 * Handler with a pre-created HTTP response.
	 * 
	 * @param status status
	 * @param httpResponse HTTP response
	 */
	public HandlerResponse(int status, Response httpResponse) {
		this.status = status;
		this.httpResponse = httpResponse;
	}
	
	/**
	 * Handler with status and a DB ID of a just created record.
	 * 
	 * @param status status
	 * @param savedId save ID
	 */
	public HandlerResponse(int status, int savedId) {
		this.status = status;
		this.savedId = savedId;
	}
	
	/**
	 * Handler with status and a message to show.
	 * 
	 * @param status status 
	 * @param message message
	 */
	public HandlerResponse(int status, String message) {
		this.status = status;
		this.message = message;
	}
	
	/**
	 * Handler with status, title and a message to show.
	 * 
	 * @param status status 
	 * @param title title
	 * @param message message
	 */
	public HandlerResponse(int status, String title, String message) {
		this.status = status;
		this.title = title;
		this.message = message;
	}
	
	/**
	 * Handler with status, title, message and an exception to show.
	 * 
	 * @param status status 
	 * @param title title
	 * @param message message
	 * @param exception exception
	 */
	public HandlerResponse(int status, String title, String message, Exception exception) {
		this.status = status;
		this.title = title;
		this.message = message;
		this.exception = exception;
	}
	
	/**
	 * Handler with status, title, message and an exception to show and an additional object.
	 * 
	 * @param status status 
	 * @param title title
	 * @param message message
	 * @param exception exception
	 * @param object object
	 */
	public HandlerResponse(int status, String title, String message, Exception exception, Object object) {
		super();
		this.status = status;
		this.title = title;
		this.message = message;
		this.exception = exception;
		this.object = object;
	}

	/**
	 * Returns the id value.
	 *
	 * @return the id value
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id value.
	 *
	 * @param id the new id value
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Returns the status value.
	 *
	 * @return the status value
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status value.
	 *
	 * @param status the new status value
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the http response value.
	 *
	 * @return the http response value
	 */
	public Response getHttpResponse() {
		return httpResponse;
	}

	/**
	 * Returns the title value.
	 *
	 * @return the title value
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title value.
	 *
	 * @param title the new title value
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the message value.
	 *
	 * @return the message value
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the message value.
	 *
	 * @param message the new message value
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns the exception value.
	 *
	 * @return the exception value
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Sets the exception value.
	 *
	 * @param exception the new exception value
	 */
	public void setException(Exception exception) {
		this.exception = exception;
	}

	/**
	 * Returns the object value.
	 *
	 * @return the object value
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * Sets the object value.
	 *
	 * @param object the new object value
	 */
	public void setObject(Object object) {
		this.object = object;
	}

	/**
	 * Returns the type value.
	 *
	 * @return the type value
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the type value.
	 *
	 * @param type the new type value
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Returns the download file value.
	 *
	 * @return the download file value
	 */
	public File getDownloadFile() {
		return downloadFile;
	}

	/**
	 * Sets the download file value.
	 *
	 * @param downloadFile the new download file value
	 */
	public void setDownloadFile(File downloadFile) {
		this.downloadFile = downloadFile;
	}

	/**
	 * Returns the download file name value.
	 *
	 * @return the download file name value
	 */
	public String getDownloadFileName() {
		return downloadFileName;
	}

	/**
	 * Sets the download file name value.
	 *
	 * @param downloadFileName the new download file name value
	 */
	public void setDownloadFileName(String downloadFileName) {
		this.downloadFileName = downloadFileName;
	}
	
	/**
	 * Returns the download file mime type value.
	 *
	 * @return the download file mime type value
	 */
	public String getDownloadFileMimeType() {
		return downloadFileMimeType;
	}

	/**
	 * Sets the download file mime type value.
	 *
	 * @param downloadFileMimeType the new download file mime type value
	 */
	public void setDownloadFileMimeType(String downloadFileMimeType) {
		this.downloadFileMimeType = downloadFileMimeType;
	}

	/**
	 * Returns the saved id value.
	 *
	 * @return the saved id value
	 */
	public int getSavedId() {
		return savedId;
	}

	/**
	 * Sets the saved id value.
	 *
	 * @param savedId the new saved id value
	 */
	public void setSavedId(int savedId) {
		this.savedId = savedId;
	}
	
}
