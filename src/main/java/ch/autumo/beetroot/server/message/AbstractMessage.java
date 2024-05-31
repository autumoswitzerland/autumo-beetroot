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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * Abstract message.
 */
public abstract class AbstractMessage {

	protected final static Logger LOG = LoggerFactory.getLogger(AbstractMessage.class.getName());
	
	/** message part separator */
	public static final String MSG_PART_SEPARATOR = "#|#";
	/** message part separator regular expression */
	public static final String MSG_PART_SEPARATOR_REGEXP = "#\\|#";

	/** internal message part separator */
	public static final String INTERNAL_MSG_PART_SEPARATOR = "|";
	/** internal message part separator regular expression */
	public static final String INTERNAL_MSG_PART_SEPARATOR_REGEXP = "\\|";

	
	/** server name */
	public static String serverName;

	// Encrypt client-server-com?
	protected static final boolean ENCRYPT;
	
	static {
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		ENCRYPT = (mode != null && mode.equalsIgnoreCase("sha3"));
	}	

	
	private Map<String, String> messageMap = null;
	
	protected String message = "null";
	protected String entity = "null";
	protected String domain = "null";
	protected long id = 0;
	protected String fileId = "null";

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
	
	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	public String getEntity() {
		return entity;
	}
	
	public long getId() {
		return id;
	}

	public String getDomain() {
		return domain;
	}

	public String getFileId() {
		return fileId;
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
			String pairs[] = message.trim().split(INTERNAL_MSG_PART_SEPARATOR_REGEXP);
			for (int i = 0; i < pairs.length; i++) {
				final String pair[] = pairs[i].split("=");
				String val = "null";
				if (pair.length == 2)
					val = pair[1].trim();
				messageMap.put(pair[0].trim(), val);
			}
		}
		return messageMap.get(key);
	}
	
	/**
	 * Checks if this key is contained.
	 * @param key key
	 * @return true if so, otherwise false
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

	/**
	 * Get JSON transfer string for transferring.
	 * @return transfer string
	 * @throws IOExcpetion
	 */
	public abstract String getJsonTransferString() throws IOException;
	
}
