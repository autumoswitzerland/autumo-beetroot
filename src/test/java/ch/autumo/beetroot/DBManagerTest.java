package ch.autumo.beetroot;

import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.utils.DBField;

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

		System.out.println("Amount of DB fields for table '"+table+"': " + fields.size());
		
		for (Iterator<DBField> iterator = fields.iterator(); iterator.hasNext();) {
			final DBField dbField = iterator.next();
			System.out.println(dbField);
		}
	}
	
}
