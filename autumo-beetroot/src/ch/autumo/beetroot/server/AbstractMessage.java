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
package ch.autumo.beetroot.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * Abstract message.
 */
public abstract class AbstractMessage {

	/** message part separator character */
	public static final String MSG_PART_SEPARATOR = "#";

	// Encrypt client-server-com?
	static {
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		ENCRYPT = (mode != null && mode.equalsIgnoreCase("cmd"));
	}	
	protected static final boolean ENCRYPT;
	
	private Map<String, String> messageMap = null;
	
	protected String message = "null";
	protected String entity = "null";
	protected String domain = "null";
	protected int id = 0;

	protected Serializable object = null;

	
	public AbstractMessage() {
	}

	public AbstractMessage(String message) {
		this.message = message;
	}

	public void setObject(Serializable object) {
		this.object = object;
	}
	
	public Serializable getObject() {
		return object;
	}
	
	public String getEntity() {
		return entity;
	}
	
	public int getId() {
		return id;
	}

	public String getDomain() {
		return domain;
	}
	
	protected void deserializeObject(String serializedObject) throws IOException {
		final byte data[] = Base64.getDecoder().decode(serializedObject);
		final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		try {
			this.object  = (Serializable) ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException("Class not found for transferred object", e);
		}
		ois.close();
	}

	protected String serializeObject() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this.object);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray()); 
	}
	
	/**
	 * Get transfer data.
	 * @return transfer data
	 * @throws IOExcpetion
	 */
	public byte[] getData()  throws IOException {
		return getTransferString().getBytes(StandardCharsets.UTF_8);
	}
	
	/**
	 * Get transfer data length.
	 * 
	 * @return transfer data length
	 * @throws IOExcpetion
	 */
	public int getDataLength() throws IOException {
		return getData().length;
	}

	/**
	 * Helper method for paired message.
	 * @param key key
	 * @return value
	 */
	public int getMessageIntValue(String key) {
		return Integer.valueOf(this.getMessageValue(key)).intValue();
	}
	
	/**
	 * Helper method for paired message.
	 * @param key key
	 * @return value
	 */
	public String getMessageValue(String key) {

		if (messageMap == null) {
			messageMap = new HashMap<String, String>();
			String pairs[] = message.replaceAll(" ", "").trim().split(",");
			for (int i = 0; i < pairs.length; i++) {
				final String pair[] = pairs[i].split("=");
				messageMap.put(pair[0], pair[1]);
			}
		}
		return messageMap.get(key);
	}
	
	/**
	 * Checks if this key is contained.
	 * @param key key
	 * @return true if so, otehrwise false
	 */
	public boolean contains(String key) {
		return this.getMessageValue(key) != null;
	}
	
	/**
	 * Get transfer string for transferring.
	 * @return transfer string
	 * @throws IOExcpetion
	 */
	public abstract String getTransferString() throws IOException;
	
}
