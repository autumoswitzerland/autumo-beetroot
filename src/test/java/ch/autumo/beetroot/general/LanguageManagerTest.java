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

import static org.junit.Assert.assertTrue;

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
		
		boolean active = BeetRootConfigurationManager.getInstance().translateTemplates();
		//System.out.println("Template translation activated? " + BeetRootConfigurationManager.getInstance().translateTemplates());
		assertTrue("Translate templates not activated!", active);
		
		String arr[] = new String[] {"GalaxyClass", "2", "Warp Core Exception, blah blah..."};
		
		String s0 = LanguageManager.getInstance().translate("base.err.handler.construct.msg", "en", "GalaxyClass", "2", "Warp Core Exception, blah blah...");
		//System.out.println(s0);
		assertTrue("s0 not translated!",!s0.contains("{"));
		String s1 = LanguageManager.getInstance().translate("base.err.handler.construct.msg", "en", ((Object[])arr));
		//System.out.println(s1);
		assertTrue("s1 not translated!",!s1.contains("{"));
		
		arr = new String[] {"Jean-Luc", "two"};
		
		String s2 = LanguageManager.getInstance().translateTemplate("message", "de", arr);
		//System.out.println(s2); 
		assertTrue("s2 not translated!",!s2.contains("{"));
	}

}
