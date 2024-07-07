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
package ch.autumo.beetroot.general;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.LanguageManager;


/**
 * Language Manager test.
 */
public class LanguageManagerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	
	
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		System.out.println("Template translation activated? " + BeetRootConfigurationManager.getInstance().translateTemplates());
		
		String arr[] = new String[] {"GalaxyClass", "2", "Warp Core Exception, blah blah..."};
		
		System.out.println(LanguageManager.getInstance().translate("base.err.handler.construct.msg", "en", "GalaxyClass", "2", "Warp Core Exception, blah blah...")); 
		System.out.println(LanguageManager.getInstance().translate("base.err.handler.construct.msg", "en", ((Object[])arr)));
		
		arr = new String[] {"Jean-Luc", "two"};
		
		System.out.println(LanguageManager.getInstance().translateTemplate("message", "Jean-Luc", arr)); 
	}

}
