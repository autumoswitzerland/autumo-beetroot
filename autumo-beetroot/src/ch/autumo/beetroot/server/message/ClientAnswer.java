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
package ch.autumo.beetroot.server.message;

import java.io.IOException;

import org.json.JSONObject;

import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Utils;

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
			return Utils.encodeCom(ts, SecureApplicationHolder.getInstance().getSecApp());
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
			transferString = Utils.decodeCom(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
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
			String data = Utils.encodeCom(json.toString(), SecureApplicationHolder.getInstance().getSecApp());
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
			data = Utils.decodeCom(data, SecureApplicationHolder.getInstance().getSecApp());
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