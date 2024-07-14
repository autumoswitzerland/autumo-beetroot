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

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.security.Security;

/**
 * Secure server command.
 */
public class ServerCommand extends AbstractMessage {

	/** Dispatcher ID for internal server-commands */
	public static final String DISPATCHER_ID_INTERNAL = "beetroot-internal";
	
	private static String cfgServerName = null;

	/**
	 * 'sockets|web', web for tunneling!
	 */
	private static transient String mode = null;
	
	protected static String host = null;
	protected static int port = -1;
	private static int timeout = -1; // milliseconds

	private String serverName = null;
	private String dispatcherId = null;
	
	private transient boolean forceSockets = false;
	
	static {
		reInit();
	}
	
	
	/**
	 * Initialize configuration.
	 */
	protected static void reInit() {
		// we must read these configs always, in some applications this may
		// be changed by the user! 
		cfgServerName = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_SERVER_NAME);
		if (cfgServerName == null || cfgServerName.length() == 0)
			cfgServerName = "solothurn";
		
		mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_MODE, "sockets");
		host = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_HOST);
		
		// If 'web' mode is used, this port will be overwritten by the communicator, unless this is an
		// internal server command, then it is always processed over sockets per admin-port! 
		port = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_PORT);
		
		timeout = BeetRootConfigurationManager.getInstance().getInt("connection_timeout"); // seconds
        if (timeout == -1) {
			timeout = 5;
			LOG.warn("Using 5 seconds for client connection timeout.");
        }
        timeout = timeout * 1000;		
	}
	
	/**
	 * Internal constructor.
	 */
	private ServerCommand() {
		super();
		this.serverName = cfgServerName;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param dispatcherId dispatcher ID
	 * 	{@link ch.autumo.beetroot.server.modules.Dispatcher}
	 * @param command server command
	 */
	public ServerCommand(String dispatcherId, String command) {
		super(command);
		this.serverName = cfgServerName;
		this.dispatcherId = dispatcherId;
	}

	/**
	 * Constructor.
	 * 
	 * @param dispatcherId dispatcher ID
	 * 	{@link ch.autumo.beetroot.server.modules.Dispatcher}
	 * @param command server command
	 * @param fileId unique file ID
	 */
	public ServerCommand(String dispatcherId, String command, String fileId) {
		super(command);
		this.serverName = cfgServerName;
		this.dispatcherId = dispatcherId;
		this.fileId = fileId;
	}

	/**
	 * Constructor.
	 * 
	 * @param dispatcherId dispatcher ID
	 * 	{@link ch.autumo.beetroot.server.modules.Dispatcher}
	 * @param command server command
	 * @param fileId unique file ID
	 * @param domain domain
	 */
	public ServerCommand(String dispatcherId, String command, String fileId, String domain) {
		super(command);
		this.serverName = cfgServerName;
		this.dispatcherId = dispatcherId;
		this.fileId = fileId;
		this.domain = domain;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param dispatcherId dispatcher ID
	 * 	{@link ch.autumo.beetroot.server.modules.Dispatcher}
	 * @param command server command
	 * @param entity entity name
	 * @param id unique id
	 */
	public ServerCommand(String dispatcherId, String command, String entity, long id) {
		super(command);
		this.serverName = cfgServerName;
		this.dispatcherId = dispatcherId;
		this.entity = entity;
		this.id = id;
	}

	/**
	 * Constructor.
	 * 
	 * @param dispatcherId dispatcher ID
	 * 	{@link ch.autumo.beetroot.server.modules.Dispatcher}
	 * @param command server command
	 * @param entity entity name
	 * @param id unique id
	 * @param domain domain name
	 */
	public ServerCommand(String dispatcherId, String command, String entity, long id, String domain) {
		super(command);
		this.serverName = cfgServerName;
		this.dispatcherId = dispatcherId;
		this.entity = entity;
		this.id = id;
		this.domain = domain;
	}

	/**
	 * Get dispatcher id {@link ch.autumo.beetroot.server.modules.Dispatcher}.
	 * @return dispatcher id
	 */
	public String getDispatcherId() {
		return dispatcherId;
	}
	
	/**
	 * Timeout for sending a server command client-side.
	 * 
	 * @return timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * 'sockets|web' - 'web' for tunneling, 'web' is 
	 * not working for internal server commands.
	 * 
	 * @return mode
	 */
	public String getMode() {
		return mode;
	}
	
	/**
	 * Server host; set by message itself.
	 * 
	 * @return server host
	 */
	public String getHost() {
		return host;
	}
	
	/**
	 * Server port; set by message itself.
	 * 
	 * @return server port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Server name; set by message itself.
	 * 
	 * @return server name
	 */
	public String getServerName() {
		return serverName;
	}
	
	/**
	 * Get command.
	 * 
	 * @return command
	 */
	public String getCommand() {
		return message;
	}

	/**
	 * Force message over sockets; so web tunneling will be ignore.
	 */
	public void forceSockets() {
		this.forceSockets = true;
	}
	
	/**
	 * Is message forced over sockets?
	 * 
	 * @return true, if message is forced over sockets
	 */
	public boolean isForceSockets() {
		return forceSockets;
	}
	
	@Override
	public String getTransferString() throws IOException {
		
		String ts = serverName + MSG_PART_SEPARATOR + dispatcherId + MSG_PART_SEPARATOR + message.trim() + MSG_PART_SEPARATOR + entity.trim() + MSG_PART_SEPARATOR + id + MSG_PART_SEPARATOR + fileId + MSG_PART_SEPARATOR + domain; 
		if (super.object != null)
			ts = ts + MSG_PART_SEPARATOR + super.serializeObject();
		
		if (ENCRYPT)
			return Security.encodeCom(ts, SecureApplicationHolder.getInstance().getSecApp());
		else
			return ts;
	}

	/**
	 * Create a new server command out of transfer string.
	 * 
	 * @param transferString transfer string
	 * @return parsed server command
	 * @throws IOException IO exception
	 */
	public static ServerCommand parse(String transferString) throws IOException {
	
		if (ENCRYPT)
			transferString = Security.decodeCom(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
		final ServerCommand command = new ServerCommand();
		
		final String parts [] = transferString.split(MSG_PART_SEPARATOR_REGEXP, 8);
		command.serverName = parts[0];
		command.dispatcherId = parts[1];
		command.message = parts[2];
		command.entity = parts[3];
		command.id = Integer.valueOf(parts[4]).intValue();
		command.fileId = parts[5];
		command.domain = parts[6];
		if (parts.length == 8) {
			command.deserializeObject(parts[7]);
		}
		
		return command;
	}

	@Override
	public String getJsonTransferString() throws IOException {
		
		final StringBuilder json = new StringBuilder();
		json.append("\"serverName\":\""+serverName+"\",");
		json.append("\"dispatcherId\":\""+dispatcherId+"\",");
		json.append("\"message\":\""+message.trim()+"\",");
		json.append("\"entity\":\""+entity.trim()+"\",");
		json.append("\"id\": \""+id+"\",");
		json.append("\"fileId\":\""+fileId+"\",");
		json.append("\"domain\":\""+domain+"\"");
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
	 * Create a new server command out of JSON transfer string.
	 * 
	 * @param transferString JSON transferString transfer string
	 * @return parsed server command
	 * @throws IOException IO exception
	 */
	public static ServerCommand parseJson(String transferString) throws IOException {
		
		final ServerCommand command = new ServerCommand();
		JSONObject o = new JSONObject(transferString);
		
		if (ENCRYPT) {
			String data = o.getString("data");
			data = Security.decodeCom(data, SecureApplicationHolder.getInstance().getSecApp());
			o = new JSONObject("{"+data+"}");
		}
		
		command.serverName = o.getString("serverName");
		command.dispatcherId = o.getString("dispatcherId");
		command.message = o.getString("message");
		command.entity = o.getString("entity");
		command.id = o.getInt("id");
		command.fileId = o.getString("fileId");
		command.domain = o.getString("domain");
		if (o.has("object")) {
			command.deserializeObject(o.getString("object"));
		}
		
		return command;
	}
	
}
