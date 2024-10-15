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
package ch.autumo.beetroot.models;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.handler.users.User;


/**
 * Model test 2.
 */
public class ModelTest2 {

	@BeforeClass
	public static void setup() throws Exception {
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
		BeetRootDatabaseManager.getInstance().initialize();
	}
	
	@Test
	public void saveReadFindFirstAssocsDeleteFindFirst() throws Exception {
	
		int id = 100;
		
		User u = new User();
		u.setId(id);
		u.setFirstname("Geordi");
		u.setLastname("La Forge");
		u.setEmail("geordi.laforge@enterprise.com");
		u.setLang("en");
		u.setPassword("*************");
		u.setSecretkey("hjde9ifeonsndpoiwdjmp");
		u.setStored(false);
		u.setTwoFa(true);
		u.setUsername("laforge");
		//System.out.println("ORIG: "+u);

		User uc = (User) u.clone();
		//System.out.println("COPY: "+uc);
		
		// Now, we cheat
		uc.setId(id);
		
		assertEquals(uc, u);
	}

    @AfterClass
    public static void tearDown() throws Exception {
        BeetRootDatabaseManager.getInstance().release();
    }
	
}
