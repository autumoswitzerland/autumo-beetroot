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
package ch.autumo.beetroot;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.models.ModelTest;
import ch.autumo.beetroot.utils.database.DBField;


/**
 * DB manager test. See also {@link ModelTest}.
 */
public class DBManagerTest {

	@BeforeClass
	public static void setup() throws Exception {
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
		BeetRootDatabaseManager.getInstance().initialize();
	}

	@Test
	public void tabelDesc() throws Exception {
		
		// H2 / MySQL / MariaDB
		String table = "products";
		// PostgreSQL / Oracle
		//String table = "users"; 
		
		final List<DBField> fields = BeetRootDatabaseManager.getInstance().describeTable(table);

		//System.out.println("Amount of DB fields for table '"+table+"': " + fields.size());
		/*
		for (Iterator<DBField> iterator = fields.iterator(); iterator.hasNext();) {
			final DBField dbField = iterator.next();
			System.out.println(dbField);
		}
		*/
		
		assertTrue("Bean fields (from DB colums) missing!", fields.size() == 7);
	}
	
}
