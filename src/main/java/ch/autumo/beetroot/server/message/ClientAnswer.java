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
package ch.autumo.beetroot.server.message;

import java.io.IOException;

import org.json.JSONObject;

import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Security;

/**
 * Client answer.
 */
public class ClientAnswer extends AbstractMessage {

	public static final int TYPE_OK = 1;
	public static final int TYPE_FILE_OK = 2;
	public static final int TYPE_FILE_NOK = -2;
	public static final int TYPE_ERROR = -1;
	
	private int type = TYPE_OK;
	private String errorReason = "null";
	
	public ClientAnswer() {
	}

	public ClientAnswer(int type) {
		this.type = type;
	}

	public ClientAnswer(String answer) {
		super(answer);
	}

	public ClientAnswer(String answer, int type) {
		super(answer);
		this.type = type;
		this.fileId = null;
	}
	
	public ClientAnswer(String answer, String fileId) {
		super(answer);
		this.type = TYPE_FILE_OK;
		this.fileId = fileId;
	}
	
	public ClientAnswer(String answer, String entity, long id) {
		super(answer);
		this.type = TYPE_FILE_OK;
		this.entity = entity;
		this.id = id;
	}
	
	public ClientAnswer(int type, String answer, String entity, long id, String errorReason) {
		super(answer);
		this.type = type;
		this.entity = entity;
		this.id = id;
		this.errorReason = errorReason;
	}
	
	public int getType() {
		return type;
	}
	
	public String getAnswer() {
		return message;
	}
	
	public String getErrorReason() {
		return errorReason;
	}
	
	@Override
	public String getTransferString() throws IOException {

		String ts = type + MSG_PART_SEPARATOR + message.trim() + MSG_PART_SEPARATOR + entity.trim() + MSG_PART_SEPARATOR + id + MSG_PART_SEPARATOR + fileId + MSG_PART_SEPARATOR + errorReason.trim(); 
		if (super.object != null)
			ts = ts + MSG_PART_SEPARATOR + super.serializeObject();
		
		if (ENCRYPT)
			return Security.encodeCom(ts, SecureApplicationHolder.getInstance().getSecApp());
		else
			return ts;
	}

	/**
	 * Create a new client answer out of transfer string.
	 * 
	 * @param transferString transfer string
	 * @return parsed client answer command
	 * @throws IOException
	 */
	public static ClientAnswer parse(String transferString) throws IOException {
	
		if (ENCRYPT)
			transferString = Security.decodeCom(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
		final ClientAnswer answer = new ClientAnswer();
		
		final String parts [] = transferString.split(MSG_PART_SEPARATOR, 7);
		answer.type = Integer.valueOf(parts[0]).intValue();
		answer.message = parts[1];
		answer.entity = parts[2];
		answer.id = Integer.valueOf(parts[3]).intValue();
		answer.fileId = parts[4];
		answer.errorReason = parts[5];
		if (parts.length == 7) {
			answer.deserializeObject(parts[6]);
		}
		
		return answer;
	}	

	@Override
	public String getJsonTransferString() throws IOException {
		final StringBuffer json = new StringBuffer();
		json.append("\"type\":\""+type+"\",");
		json.append("\"message\":\""+message.trim()+"\",");
		json.append("\"entity\":\""+entity.trim()+"\",");
		json.append("\"id\": \""+id+"\",");
		json.append("\"fileId\":\""+fileId+"\",");
		json.append("\"errorReason\":\""+errorReason.trim()+"\"");
		if (super.object != null) {
			json.append(",");
			json.append("\"object\":\""+super.serializeObject()+"\"");
		}
		
		String result = "{";
		if (ENCRYPT) {
			String data = Security.encodeCom(json.toString(), SecureApplicationHolder.getInstance().getSecApp());
			result += "\"data\":\""+data+"\"";
		} else {
			result += json.toString();
		}
		result += "}";
		
		return result;
	}
	
	/**
	 * Create a new client answer out of JSON transfer string.
	 * 
	 * @param transferString JSON transfer string
	 * @return parsed client answer
	 * @throws IOException
	 */
	public static ClientAnswer parseJson(String transferString) throws IOException {

		final ClientAnswer answer = new ClientAnswer();
		JSONObject o = new JSONObject(transferString);
		
		if (ENCRYPT) {
			String data = o.getString("data");
			data = Security.decodeCom(data, SecureApplicationHolder.getInstance().getSecApp());
			o = new JSONObject("{"+data+"}");
		}
		
		answer.type = o.getInt("type");
		answer.message = o.getString("message");
		answer.entity = o.getString("entity");
		answer.id = o.getInt("id");
		answer.fileId = o.getString("fileId");
		answer.errorReason = o.getString("errorReason");
		if (o.has("object")) {
			answer.deserializeObject(o.getString("object"));
		}
		
		return answer;
	}	
	
}
