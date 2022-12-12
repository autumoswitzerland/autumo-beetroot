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

import java.io.IOException;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Utils;

/**
 * Secure server command.
 */
public class ServerCommand extends AbstractMessage {

	private String serverName = null;

	protected String host = null;
	protected int port = -1;
	private int timeout = -1;
	
	private ServerCommand() {
		super();
		init();
	}
	
	public ServerCommand(String command) {
		super(command);
		init();
	}

	public ServerCommand(String command, String fileId) {
		super(command);
		this.fileId = fileId;
		init();
	}
	
	public ServerCommand(String command, String entity, long id) {
		super(command);
		this.entity = entity;
		this.id = id;
		init();
	}

	public ServerCommand(String command, String entity, long id, String domain) {
		super(command);
		this.entity = entity;
		this.id = id;
		this.domain = domain;
		init();
	}
	
	protected void init() {
		// we must read these configs always, in some applications this may
		// be changed by the user! 
		serverName = BeetRootConfigurationManager.getInstance().getString("server_name");
		if (serverName == null || serverName.length() == 0)
			serverName = "solothurn";
		
		host = BeetRootConfigurationManager.getInstance().getString("admin_host");
		port = BeetRootConfigurationManager.getInstance().getInt("admin_port");
		
		timeout = BeetRootConfigurationManager.getInstance().getInt("connection_timeout");
        if (timeout == -1) {
			timeout = 5000;
			LOG.warn("Using 5 seconds for client connection timeout.");
        }
        timeout = timeout * 1000;			
	}

	public int getTimeout() {
		return timeout;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getServerName() {
		return this.serverName;
	}
	
	public String getCommand() {
		return message;
	}

	@Override
	public String getTransferString() throws IOException {
		
		String ts = serverName + MSG_PART_SEPARATOR +  message.trim() + MSG_PART_SEPARATOR + entity.trim() + MSG_PART_SEPARATOR + id + MSG_PART_SEPARATOR + fileId + MSG_PART_SEPARATOR + domain; 
		if (super.object != null)
			ts = ts + MSG_PART_SEPARATOR + super.serializeObject();
		
		if (ENCRYPT)
			return Utils.encode(ts, SecureApplicationHolder.getInstance().getSecApp());
		else
			return ts;
	}

	/**
	 * Create a new server command out of transfer string.
	 * 
	 * @param transferString transfer string
	 * @return parsed server command
	 * @param IOException
	 */
	public static ServerCommand parse(String transferString) throws IOException {
	
		if (ENCRYPT)
			transferString = Utils.decodeCom(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
		final ServerCommand command = new ServerCommand();
		
		final String parts [] = transferString.split(MSG_PART_SEPARATOR, 7);
		command.serverName = parts[0];
		command.message = parts[1];
		command.entity = parts[2];
		command.id = Integer.valueOf(parts[3]).intValue();
		command.fileId = parts[4];
		command.domain = parts[5];
		if (parts.length == 7) {
			command.deserializeObject(parts[6]);
		}
		
		return command;
	}
	
}
