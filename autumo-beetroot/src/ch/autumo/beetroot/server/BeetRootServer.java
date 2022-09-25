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
		super(params, "beetRoot");
	}
	
	/**
	 * Create an beetRoot server.
	 * 
	 * @param params start or stop
	 */
	public BeetRootServer(String params[], String name) {

		super(params, name);
	}

	/**
	 * Overwrite do do something before starting the server.
	 * Handle exceptions by your own!
	 */
	protected void beforeStart() {
	}

	/**
	 * Overwrite do do something after starting the server.
	 * Handle exceptions by your own!
	 */
	protected void afterStart() {
	}
	
	/**
	 * Overwrite do do something before stopping the server.
	 * Handle exceptions by your own!
	 */
	protected void beforeStop() {
	}

	/**
	 * Overwrite do do something after stopping the server.
	 * Handle exceptions by your own!
	 */
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
