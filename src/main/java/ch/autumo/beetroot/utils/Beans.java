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
package ch.autumo.beetroot.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.dbutils.BeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Entity;


/**
 * Beans helper methods.
 */
public class Beans {

	private final static Logger LOG = LoggerFactory.getLogger(Beans.class.getName());

	/**
	 * Class to DB table.
	 * @param clz class
	 * @return name of table in DB
	 */
	public static String classToTable(Class<?> clz) {
		
		final String c = clz.getName().toLowerCase();
		String table = c.substring(c.lastIndexOf(".") + 1, c.length());
		if (table.endsWith("y"))
			table = (table.substring(0, table.length() - 1)) + "ies";
		else
			table += "s";
		
		return table;
	}
	
	/**
	 * Returns the class name without package or '.class'.extension.
	 * 
	 * @param tableName DB table name
	 * @return class name
	 */
	public static String tableToClassName(String tableName) {
		String cName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1, tableName.length()).toLowerCase();
		if (cName.endsWith("ies"))
			cName = cName.substring(0, cName.length() - 3) + "y";
		else
			cName = cName.substring(0, cName.length() - 1);
		
		return cName;
	}

	/**
	 * Create empty bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @return entity bean or null
	 * @throws SQLException
	 */
	public static Entity createBean(Class<?> beanClass) throws Exception {
		final Constructor<?> constructor = beanClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Entity bean = (Entity) constructor.newInstance();
        return bean;
	}
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @return entity bean or null
	 * @throws SQLException
	 */
	public static Entity createBean(Class<?> beanClass, ResultSet set) throws SQLException {
		return createBean(beanClass, set, new BeanProcessor());
	}
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @param processor bean processor
	 * @return entity bean or null
	 * @throws SQLException
	 */
	public static Entity createBean(Class<?> beanClass, ResultSet set, BeanProcessor processor) throws SQLException {
		
		Entity entity = null;
		if (beanClass != null)
			entity = (Entity) processor.toBean(set, beanClass);
		
		return entity;
	}
	
	/**
	 * Get foreign references map if any or null.
	 * The map holds pairs of DB foreign keys and referenced primary tabel names.
	 * 
	 * @param emptyBean an empty bean to access static references if any
	 * @return foreign references map
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Class<?>> getForeignReferences(Entity emptyBean) throws Exception {
		Map<String, Class<?>> map = null;
		Method getFR = null;
		Class<?> clz = emptyBean.getClass();
		try {
			getFR = clz.getDeclaredMethod("getForeignReferences");
		} catch (Exception e) {
			// No refs, that's fine!
			return null;
		}
		map = (Map<String, Class<?>>) getFR.invoke(emptyBean);
		return map;
	}	
	/**
	 * Get display field name of bean.
	 * 
	 * @param emptyBean an empty bean to access static references if any
	 * @return display field name
	 * @throws Exception
	 */
	public static String getDisplayField(Entity emptyBean) throws Exception {
		String displayField = null;
		Method getDV = null;
		Class<?> clz = emptyBean.getClass();
		try {
			getDV = clz.getDeclaredMethod("getDisplayField");
		} catch (Exception e) {
			LOG.info("No display field getter found in bean of type '"+clz.getName()+", but it might be used to be shown as a reference entity' -> using 'id'!");
			return "id";
		}
		displayField = (String) getDV.invoke(emptyBean);
		return displayField;
	}
	
}

