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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Model;


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
		/*
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
			System.out.println("intSecKey: " + entity.get("intSecKey"));
		}
		*/	
		assertTrue("Empty list!", objects.size() > 0);
	}

	@Test
	public void findFirst() {
		Model object = Model.findFirst(Product.class, "name = ?", "ifaceX");
		//System.out.println("why"+object);
		assertNotNull("No entity found!", object);
	}

	@Test
	public void where() {
		List<Model> objects = Model.where(Product.class, "name = ?", "ifaceX");
		/*
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}
		*/		
		assertTrue("Empty result!", objects.size() > 0);
	}

	@Test
	public void listAll2() {
		List<Model> objects = Model.listAll(Variant.class);
		/*
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}
		*/		
		assertTrue("Empty result!", objects.size() > 0);
	}

	@Test(expected = Exception.class)
	public void readUpdateDelete() throws Exception {
		Model object = Model.read(Product.class, 1);
		//System.out.println("READ:"+object);
		assertNotNull("No entity found!", object);
		
		Product p = (Product) object;
		String old = p.getIntSecKey();
		p.setIntSecKey("1111111111");
		//System.out.println("BF UPDATE:"+p);
		
		p.update();
		Model object2 = Model.read(Product.class, 1);
		Product p2 = (Product) object2;
		
		//System.out.println("TEST:" + p2.getIntSecKey() + " = 1111111111");
		assertEquals(p2.getIntSecKey(), "1111111111");
		
		p2.setIntSecKey(old);
		p2.update();
		
		// Referential integrity
		try {
			p2.delete(); // exception expected!
		} catch (Exception e) {
			//e.printStackTrace();
			throw e;
		}
		//assertTrue("Empty result!", objects.size() > 0);
	}
	
	@Test
	public void saveReadFindFirstAssocsDeleteFindFirst() throws Exception {
		
		Variant v = new Variant();
		v.setDescription("a New One!");
		v.setIdentifier("TEST");
		v.setProductId(2);
		int id = v.save();
		
		Model object = Model.read(Variant.class, id);
		//System.out.println(object);
		assertTrue("No entity found!", object != null);

		object = Model.findFirst(Variant.class, "id = ?", id);
		//System.out.println(object);
		assertTrue("No entity found!", object != null);
		
		object = v.getAssociatedReference(Product.class);
		//System.out.println(object);
		assertTrue("No entity found!", object != null);

		Product p = (Product) object;
		List<Model> objects = p.listReferences(Variant.class);
		/*
		for (Iterator<Model> iterator = objects.iterator(); iterator.hasNext();) {
			Model entity = iterator.next();
			System.out.println(entity);
		}
		*/		
		assertTrue("Empty result!", objects.size() > 0);
		assertTrue("Empty result!", v.getTableName().equals("variants"));
		//System.out.println(v.getTableName());
		
		v.delete();
		
		assertTrue(!v.isStored());
		
		object = Model.findFirst(Variant.class, "id = ?", id);
		
		assertNull(object);
		//System.out.println(object);
	}
	
}
