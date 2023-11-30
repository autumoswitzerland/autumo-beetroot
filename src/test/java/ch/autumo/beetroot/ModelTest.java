package ch.autumo.beetroot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ch.autumo.beetroot.models.Product;
import ch.autumo.beetroot.models.Variant;

/**
 * Model test.
 * 
 * JUnit 4 infos: https://www.testwithspring.com/lesson/introduction-to-junit-4-test-classes/
 */
public class ModelTest {

	@BeforeClass
	public static void setup() throws Exception {
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
		BeetRootDatabaseManager.getInstance().initialize();
	}
	
	@Test
	public void listAll1() {
		List<Model> objects = Model.listAll(Product.class);
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
			System.out.println("intSecKey: " + entity.get("intSecKey"));
		}		
	}

	@Test
	public void findFirst() {
		Model object = Model.findFirst(Product.class, "name = ?", "IfaceX");
		System.out.println(object);
	}

	@Test
	public void where() {
		List<Model> objects = Model.where(Product.class, "name = ?", "IfaceX");
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}		
	}

	@Test
	public void listAll2() {
		List<Model> objects = Model.listAll(Variant.class);
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}		
	}

	@Test(expected = Exception.class)
	public void readUpdateDelete() throws Exception {
		Model object = Model.read(Product.class, 1);
		System.out.println("READ:"+object);
		
		Product p = (Product) object;
		String old = p.getIntSecKey();
		p.setIntSecKey("1111111111");
		System.out.println("BF UPDATE:"+p);
		p.update();
		Model object2 = Model.read(Product.class, 1);
		Product p2 = (Product) object2;
		
		System.out.println(p2.getIntSecKey());
		
		assertEquals(p2.getIntSecKey(), "1111111111");
		
		p2.setIntSecKey(old);
		p2.update();
		
		p2.delete(); // exception expected!
	}
	
	@Test
	public void saveReadFindFirstAssocsDeleteFindFirst() throws Exception {
		
		Variant v = new Variant();
		v.setDescription("a New One!");
		v.setIdentifier("TEST");
		v.setProductId(2);
		int id = v.save();
		
		Model object = Model.read(Variant.class, id);
		System.out.println(object);

		object = Model.findFirst(Variant.class, "id = ?", id);
		System.out.println(object);
		
		object = v.getAssociatedReference(Product.class);
		System.out.println(object);

		Product p = (Product) object;
		List<Model> objects = p.listReferences(Variant.class);
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}		

		System.out.println(v.getTableName());
		
		v.delete();
		
		assertTrue(!v.isStored());
		
		object = Model.findFirst(Variant.class, "id = ?", id);
		
		assertNull(object);
		
		System.out.println(object);
	}
	
}
