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
package ch.autumo.beetroot.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * beetRoot stand-alone server.
 */
public class BeetRootServer extends BaseServer {
	
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootServer.class.getName());
	
	/**
	 * Create an beetRoot server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[]) {
		super(params);
	}

	@Override
	protected boolean beforeStart() {
		return true;
	}

	@Override
	protected void afterStart() {
	}
	
	@Override
	protected void beforeStop() {
	}

	@Override
	protected void afterStop() {
	}
	
	/**
	 * Create server and start it.
	 * 
	 * @param args only one: stop or start
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Go !
    	new BeetRootServer(args);
	}

}

