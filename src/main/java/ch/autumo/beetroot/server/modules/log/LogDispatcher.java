/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.server.modules.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.modules.Dispatcher;


/**
 * Log Extended Dispatcher.
 * 
 * Add the following server configuration parameter:
 *   dispatcher_probe=ch.autumo.ifacex.system.LogDispatcher
 */
public class LogDispatcher implements Dispatcher {

	protected static final Logger LOG = LoggerFactory.getLogger(LogDispatcher.class.getName());
	
	/** Unique ID */
	public static final String ID = "autumo-beetroot-log"; 
	
	private LocalLog localLog = new LocalLog();
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public ClientAnswer dispatch(ServerCommand serverCommand) {
		try {
			return localLog.getLog();
		} catch (Exception e) {
			LOG.error("Couldn't get extended server log!", e);
			return new ClientAnswer(ClientAnswer.TYPE_ERROR);
		}	
	}

}
