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

import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Utils;

/**
 * Secure server command.
 */
public class ServerCommand extends AbstractMessage {

	private String serverName = null;
	private String host = null;
	private int port = -1;
	
	private String secKey = null;

	private ServerCommand() {
	}
	
	public ServerCommand(String serverName, String host, int port, String command) {
		super(command);
		this.serverName = serverName;
		this.host = host;
		this.port = port;
		this.secKey = SecureApplicationHolder.getInstance().getSecApp().getUniqueSecurityKey();
	}
	
	public ServerCommand(String serverName, String host, int port, String secKey, String command) {
		super(command);
		this.serverName = serverName;
		this.host = host;
		this.port = port;
		this.secKey = secKey;
	}

	public ServerCommand(String serverName, String host, int port, String command, String entity, int id) {
		super(command);
		this.serverName = serverName;
		this.host = host;
		this.port = port;
		this.secKey = SecureApplicationHolder.getInstance().getSecApp().getUniqueSecurityKey();
		this.entity = entity;
		this.id = id;
	}
	
	public ServerCommand(String serverName, String host, int port, String secKey, String command, String entity, int id) {
		super(command);
		this.serverName = serverName;
		this.host = host;
		this.port = port;
		this.secKey = secKey;
		this.entity = entity;
		this.id = id;
	}
	
	public String getServerName() {
		return this.serverName;
	}

	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getSecKey() {
		return secKey;
	}
	
	public String getCommand() {
		return message;
	}

	@Override
	public String getTransferString() throws IOException {
		final String ts = serverName + MSG_PART_SEPARATOR +  secKey + MSG_PART_SEPARATOR +  message.trim() + MSG_PART_SEPARATOR + entity.trim() + MSG_PART_SEPARATOR + id;
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
			transferString = Utils.decode(transferString, SecureApplicationHolder.getInstance().getSecApp());
		
		final ServerCommand command = new ServerCommand();
		
		final String parts [] = transferString.split(MSG_PART_SEPARATOR);
		command.serverName = parts[0];
		command.secKey = parts[1];
		command.message = parts[2];
		command.entity = parts[3];
		command.id = Integer.valueOf(parts[4]).intValue();
		
		return command;
	}
	
}
