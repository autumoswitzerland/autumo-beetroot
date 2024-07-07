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

import com.diogonunes.jcolor.Attribute;

import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Helper;

/**
 * beetRoot stand-alone server.
 */
public class BeetRootServer extends BaseServer {
	
	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootServer.class.getName());
	
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
	
	private static void optionalBanner(String args[]) {
		
		if (args.length < 1)
			return;
		if (!args[0].equals("start"))
			return;
		
		final String banner = "\n\n" + 
				" ___.                  __ __________               __\n" +   
				" \\_ |__   ____   _____/  |\\______   \\ ____   _____/  |_\n" + 
				"  | __ \\_/ __ \\_/ __ \\   __\\       _//  _ \\ /  _ \\   __\\\n" +
				"  | \\_\\ \\  ___/\\  ___/|  | |    |   (  <_> |  <_> )  |\n" +
				"  |___  /\\___/  \\___/ |__| |____|_  /\\____/ \\____/|__|\n" +
				"      \\/                          \\/";
		System.out.println(Helper.createBanner(banner, Attribute.BRIGHT_MAGENTA_TEXT()));
		System.out.println("  autumo beetRoot " + Constants.APP_VERSION);
		System.out.println("");
	}
	
	/**
	 * Create server and start it.
	 * 
	 * @param args only one: stop or start
	 * @throws Exception exception
	 */
	public static void main(String args[]) throws Exception {
		
		// Banner if possible
		optionalBanner(args);
		
		// Go !
    	new BeetRootServer(args);
	}

}

