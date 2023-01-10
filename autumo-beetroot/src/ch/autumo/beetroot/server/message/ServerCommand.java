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

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Utils;

/**
 * Secure server command.
 */
public class ServerCommand extends AbstractMessage {

	/** Dispatcher ID for internal server-commands */
	public static final String DISPATCHER_ID_INTERNAL = "beetroot-internal";
	
	private static String cfgServerName = null;

	private static transient String mode = null;
	
	protected static String host = null;
	protected static int port = -1;
	private static int timeout = -1; // milliseconds

	private String serverName = null;
	private String dispatcherId = null;
	
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
		// this port might differ depending on the mode and if it's a client
		// if 'web' mode is used, this port should be changed on client side to the beetRoot web-server port!
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
	
	public int getTimeout() {
		return timeout;
	}

	public String getMode() {
		return mode;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public String getCommand() {
		return message;
	}

	@Override
	public String getTransferString() throws IOException {
		
		String ts = serverName + MSG_PART_SEPARATOR + dispatcherId + MSG_PART_SEPARATOR + message.trim() + MSG_PART_SEPARATOR + entity.trim() + MSG_PART_SEPARATOR + id + MSG_PART_SEPARATOR + fileId + MSG_PART_SEPARATOR + domain; 
		if (super.object != null)
			ts = ts + MSG_PART_SEPARATOR + super.serializeObject();
		
		if (ENCRYPT)
			return Utils.encodeCom(ts, SecureApplicationHolder.getInstance().getSecApp());
		else
			return ts;
	}

	/**
	 * Create a new server command out of transfer string.
	 * 
	 * @param transferString transfer string
	 * @return parsed server command
	 * @throws IOException
	 */
	public static ServerCommand parse(String transferString) throws IOException {
	
		if (ENCRYPT)
			transferString = Utils.decodeCom(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
		final ServerCommand command = new ServerCommand();
		
		final String parts [] = transferString.split(MSG_PART_SEPARATOR, 8);
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
		
		final StringBuffer json = new StringBuffer();
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
			String data = Utils.encodeCom(json.toString(), SecureApplicationHolder.getInstance().getSecApp());
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
	 * @param JSON transferString transfer string
	 * @return parsed server command
	 * @throws IOException
	 */
	public static ServerCommand parseJson(String transferString) throws IOException {
		
		final ServerCommand command = new ServerCommand();
		JSONObject o = new JSONObject(transferString);
		
		if (ENCRYPT) {
			String data = o.getString("data");
			data = Utils.decodeCom(data, SecureApplicationHolder.getInstance().getSecApp());
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
