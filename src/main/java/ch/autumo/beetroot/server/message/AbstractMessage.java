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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * Abstract message.
 * <br><br>
 * Reduce the transport layer to the max. with the option to encrypt all
 * information with SHA3.
 * <br><br>
 * Messages are mainly used for dispatchers / distributed modules between 
 * the dedicated web-app installed in a web-container and the beetRoot 
 * server or for server administration commands.
 * <br><br>
 * It is also used for the file-server (if activated) to down- or upload files 
 * from/to a plug-able file storage and for roundtrip-checks of the file-server.
 * <br><br>
 * Messages for dispatchers are sent and received with {@link ch.autumo.beetroot.server.communication.ClientCommunicator}
 * and files are sent and received with {@link ch.autumo.beetroot.server.communication.FileTransfer}; in this case messages
 * are being used for coordinating the upload and download of files that are directly transported by sockets. 
 */
public abstract class AbstractMessage {

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractMessage.class.getName());
	
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

	
	/** Transport message map. */
	private Map<String, String> messageMap = null;
	/** Transport message. */
	protected String message = "null";
	/** Transport entity name. */
	protected String entity = "null";
	/** Transport domain name. */
	protected String domain = "null";
	/** Transport entity ID. */
	protected long id = 0;
	/** Transport file ID. */
	protected String fileId = "null";

	/** Additional transport object. */
	protected Serializable object = null;

	
	/**
	 * Constructor.
	 */
	public AbstractMessage() {
	}

	/**
	 * Constructor with a transport message. A transport message
	 * is usually the minimum of information needed.
	 * 
	 * @param message text message with an own chosen format
	 */
	public AbstractMessage(String message) {
		this.message = message;
	}
	
	/**
	 * Return a JSON object or null; null means the object hold
	 * by this message represents another object type, but it is
	 * always serializable.
	 * 
	 * @return JSON object or null
	 */
	public JSONObject getJSONObject() {
		try {
			return new JSONObject(this.object.toString());
        } catch (JSONException ex) {
            return null;
        }
	}

	/**
	 * Set any serializable object, usually a JSON string
	 * is meaningful for transport.
	 * 
	 * @param object serializable object, e.g. JSON string
	 */
	public void setObject(Serializable object) {
		try {
			final String str = object.toString();
			new JSONObject(str);
			this.object = str;
        } catch (JSONException ex) {
			this.object = object;
        }		
	}
	
	/**
	 * Get serializable transport object hold by this message or null.
	 * 
	 * @return transport object or null
	 */
	public Serializable getObject() {
		return object;
	}
	
	/**
	 * Set an entity associated with this message.
	 * 
	 * @param entity entity
	 */
	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	/**
	 * Get entity associated with this message or null.
	 * 
	 * @return entity or null.
	 */
	public String getEntity() {
		return entity;
	}
	
	/**
	 * Get entity ID associated with this message or null.
	 * 
	 * @return ID or 0.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Get a domain associated with this message or null.
	 * A domain can help to categorize this message for
	 * specific destinations or logical containers.
	 * 
	 * @return domain or null.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * File ID if this message is used to transport file information.
	 * 
	 * @return File ID or 0
	 */
	public String getFileId() {
		return fileId;
	}
	
	/**
	 * Get transfer data; this includes all set information including an object.
	 * 
	 * @return transfer data
	 * @throws IOException IO exception
	 */
	public byte[] getData() throws IOException {
		return getTransferString().getBytes(StandardCharsets.UTF_8);
	}
	
	/**
	 * Get transfer data length.
	 * 
	 * @return transfer data length
	 * @throws IOException IO exception
	 */
	public int getDataLength() throws IOException {
		return getData().length;
	}

	/**
	 * Helper method for accessing paired transport message values. A paired 
	 * transport message  separates values with {@link #INTERNAL_MSG_PART_SEPARATOR} 
	 * and is set by the constructor {@link #AbstractMessage(String)}.
	 * 
	 * Key-Value pairs are separated by the '=' sign.
	 * 
	 * @param key key
	 * @return value
	 */
	public int getMessageIntValue(String key) {
		return Integer.parseInt(this.getMessageValue(key));
	}
	
	/**
	 * Helper method for accessing paired transport message values. A paired 
	 * transport message separates values with {@link #INTERNAL_MSG_PART_SEPARATOR} 
	 * and is set by the constructor {@link #AbstractMessage(String)}.
	 * 
	 * Key-Value pairs are separated by the '=' sign.
	 * 
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
	 * Checks if this key is contained in a paired transport message.
	 * 
	 * @param key key
	 * @return true if so, otherwise false
	 */
	public boolean contains(String key) {
		return this.getMessageValue(key) != null;
	}
	
	/**
	 * Get transfer string; this includes all set information including
	 * the additional object is set.
	 * 
	 * @return transfer string
	 * @throws IOException IO exception
	 */
	public abstract String getTransferString() throws IOException;

	/**
	 * Get JSON representation; this includes all set information including
	 * the additional object if set.
	 * 
	 * @return transfer JSON string
	 * @throws IOException IO exception
	 */
	public abstract String getJsonTransferString() throws IOException;

	/**
	 * Deserialize this object with FasterXML/Jackson. Internal method,
	 * don't call it.
	 * 
	 * @param serializedObject serialized object
	 */
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

	/**
	 * Serialize this object with FasterXML/Jackson. Internal method,
	 * don't call it.
	 * 
	 * @return serialized object
	 */
	protected String serializeObject() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this.object);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray()); 
	}
	
}
