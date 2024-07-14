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
package ch.autumo.beetroot.utils.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;

import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.annotations.Column;
import ch.autumo.beetroot.annotations.Nullable;
import ch.autumo.beetroot.annotations.Unique;


/**
 * Beans helper methods.
 */
public class Beans {

	private static final Logger LOG = LoggerFactory.getLogger(Beans.class.getName());

	/**
	 * Snake case converter (AKA camel-case to database naming convention with underscore characters).
	 */
	private static final SnakeCaseStrategy SNAKE_CASE = new SnakeCaseStrategy();
	
	/**
	 * Get database table column name by bean property name.
	 * 
	 * @param beanPropName bean property name
	 * @return database table column name
	 */
	public static String beanPropertyName2DbName(String beanPropName) {
		return SNAKE_CASE.translate(beanPropName);
	}
	
	/**
	 * Class to bean reference ID name.
	 * 
	 * @param clz class
	 * @return name of the bean reference ID.
	 */
	public static String classToRefBeanId(Class<?> clz) {
		final String c = clz.getName().toLowerCase();
		final String table = c.substring(c.lastIndexOf(".") + 1, c.length());
		return table.toLowerCase() + "Id";
	}

	/**
	 * Class to database reference ID name.
	 * 
	 * @param clz class
	 * @return name of the database reference ID.
	 */
	public static String classToRefDbId(Class<?> clz) {
		final String c = clz.getName().toLowerCase();
		final String table = c.substring(c.lastIndexOf(".") + 1, c.length());
		return table.toLowerCase() + "_id";
	}
	
	/**
	 * Class to DB table.
	 * @param clz class
	 * @return name of table in DB
	 */
	public static String classToTable(Class<?> clz) {
		final String c = clz.getName();
		return classNameToTable(c);
	}

	/**
	 * Class name to DB table.
	 * @param clz class name
	 * @return name of table in DB
	 */
	public static String classNameToTable(String clz) {
		
		String table = clz;
		if (table.lastIndexOf(".") != -1)
			table = clz.substring(clz.lastIndexOf(".") + 1, clz.length());

		int countUpperCase = 0;
        int secondUpperCaseIndex = -1;
        for (int i = 0; i < table.length(); i++) {
            char ch = table.charAt(i);
            if (Character.isUpperCase(ch)) {
                countUpperCase++;
                if (countUpperCase == 2) {
                    secondUpperCaseIndex = i;
                    break;
                }
            }
        }
		if (secondUpperCaseIndex != -1) {
			String t0 = table.substring(0, secondUpperCaseIndex);
			String t1 = table.substring(secondUpperCaseIndex, table.length());
			t0 = makePlural(t0);
			t1 = makePlural(t1);
			table = t0 + "_" + t1;
		} else {
			table = makePlural(table);
		}
		return table.toLowerCase();
	}
	
	/**
	 * Make plural name.
	 * 
	 * @param name singular name
	 * @return plural name
	 */
	public static String makePlural(String name) {
		if (name.endsWith("y"))
			name = (name.substring(0, name.length() - 1)) + "ies";
		else
			name += "s";		
		return name;
	}
	
	/**
	 * Returns the class name without package or '.class'.extension.
	 * 
	 * @param tableName DB table name
	 * @return class name
	 */
	public static String tableToClassName(String tableName) {
		
		String cName = null;
		if (tableName.indexOf("_") != -1) {
			final String p[] = tableName.split("_");
			String t0 = p[0].substring(0, 1).toUpperCase() + p[0].substring(1, p[0].length()).toLowerCase();
			String t1 = p[1].substring(0, 1).toUpperCase() + p[1].substring(1, p[1].length()).toLowerCase();
			t0 = makeSingular(t0);
			t1 = makeSingular(t1);
			cName = t0 + t1;
		} else {
			cName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1, tableName.length()).toLowerCase();
			cName = makeSingular(cName);
		}
		return cName;
	}

	/**
	 * Make singular name.
	 * 
	 * @param name plural name
	 * @return singular name
	 */
	public static String makeSingular(String name) {
		if (name.endsWith("ies"))
			name = name.substring(0, name.length() - 3) + "y";
		else
			name = name.substring(0, name.length() - 1);
		return name;
	}
	
	/**
	 * Create empty bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @return entity bean or null
	 * @throws SQLException SQL exception
	 */
	public static Model createBean(Class<?> beanClass) throws Exception {
		final Constructor<?> constructor = beanClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Model bean = (Model) constructor.newInstance();
        return bean;
	}
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @return entity bean or null
	 * @throws SQLException SQL exception
	 */
	public static Model createBean(Class<?> beanClass, ResultSet set) throws SQLException {
		return createBean(beanClass, set, new BeanProcessor());
	}
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @param processor bean processor
	 * @return entity bean or null
	 * @throws SQLException SQL exception
	 */
	public static Model createBean(Class<?> beanClass, ResultSet set, BeanProcessor processor) throws SQLException {
		Model entity = null;
		if (beanClass != null) {
			entity = (Model) processor.toBean(set, beanClass);
			entity.setStored(true);
		}
		return entity;
	}
	
	/**
	 * Get foreign references map if any or null.
	 * The map holds pairs of DB foreign keys and referenced primary tabel names.
	 * 
	 * @param emptyBean an empty bean to access static references if any
	 * @return foreign references map
	 * @throws Exception exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Class<?>> getForeignReferences(Entity emptyBean) throws Exception {
		Map<String, Class<?>> map = null;
		Method getFR = null;
		Class<?> clz = emptyBean.getClass();
		while (clz != null) {
			try {
				getFR = clz.getDeclaredMethod("getForeignReferences");
				clz = null;
			} catch (Exception e) {
				clz = clz.getSuperclass();
			}
		}
		if (getFR != null) {
			map = (Map<String, Class<?>>) getFR.invoke(emptyBean);
		} else {
			// No refs, that's fine!
			map  = null;
		}
		return map;
	}
	
	/**
	 * Get display field name of bean.
	 * 
	 * @param emptyBean an empty bean to access static references if any
	 * @return display field name
	 * @throws Exception exception
	 */
	public static String getDisplayField(Entity emptyBean) throws Exception {
		String displayField = null;
		Method getDV = null;
		Class<?> clz = emptyBean.getClass();
		while (clz != null) {
			try {
				getDV = clz.getDeclaredMethod("getDisplayField");
				clz = null;
			} catch (Exception e) {
				clz = clz.getSuperclass();
			}
		}
		if (getDV != null) {
			displayField = (String) getDV.invoke(emptyBean);
		} else {
			LOG.info("No display field getter found in bean of type '"+emptyBean.getClass().getName()+", but it might be used to be shown as a reference entity' -> using 'id'!");
			displayField = "id";
		}
		return displayField;
	}

	/**
	 * Update the given model with entity from bean/model annotations,
	 * if it hasn't been updated yet.
	 * 
	 * We access information PLANT has generated us with annotations, so
	 * don't have to access database for meta data again!
	 * 
	 * @param entity entity
	 * @param model model
	 */
	public static void updateModel(Entity entity, Map<String, Map<String, BeanField>> model) {
		Class<?> clz = entity.getClass();
		final String tableName = Beans.classToTable(clz);
		if (!model.containsKey(tableName)) {
			final Map<String, BeanField> beanFields = Beans.getBeanFields(clz);
			// Only when we have columns, we have a table so to speak!
	    	// If there are no bean field for this table, something is wrong,
	    	// it is not a beetRoot model then!
			if (beanFields.size() > 0)
				model.put(tableName, beanFields);
		}
	}

	/**
	 * Get bean fields.
	 * 
	 * @param clz bean class
	 * @return bean fields map
	 */
	public static  Map<String, BeanField> getBeanFields(Class<?> clz) {
		
		final Map<String, BeanField> beanFields = new HashMap<String, BeanField>();
		while (clz != null) {
		
	    	for (Field field : clz.getDeclaredFields()) {
	    		
	    		final String beanName = field.getName();
                String dbColumnName = null;
                boolean isNullable = false;
                boolean isUnique = false;
	    		
	    		if (field.isAnnotationPresent(Column.class))
	    			dbColumnName = field.getAnnotation(Column.class).name();
	    		else
                	continue; // We only put database columns as Bean fields!
	    		
	    		if (field.isAnnotationPresent(Nullable.class))
	    			isNullable = true;
	    		
	    		if (field.isAnnotationPresent(Unique.class))
	    			isUnique = true;
	    	
	    		final String gMethodName = "get" + beanName.substring(0, 1).toUpperCase() + beanName.substring(1, beanName.length());
	    		Method gMethod = null;
				try {
					gMethod = clz.getDeclaredMethod(gMethodName);
				} catch (Exception e) {
					LOG.error("Method '"+gMethodName+"' not found in class'"+clz.getName()+"! Your bean is corrupted!", e);
					throw new RuntimeException(e); // not good!
				}
	    		
	    		final String sMethodName = "set" + beanName.substring(0, 1).toUpperCase() + beanName.substring(1, beanName.length());
	    		Method sMethod = null;
				try {
					sMethod = clz.getDeclaredMethod(sMethodName, field.getType());
				} catch (Exception e) {
					LOG.error("Method '"+sMethodName+"' not found in class'"+clz.getName()+"! Your bean is corrupted!", e);
					throw new RuntimeException(e); // not good!
				}
				
                // We don't need type or default value here -> null
                final BeanField beanField = new BeanField(dbColumnName, beanName, field.getType(), isNullable, isUnique, gMethod, sMethod);
                beanFields.putIfAbsent(dbColumnName, beanField);
	    	}
	    	
	    	clz = clz.getSuperclass(); // process super-classes!
		}
		return beanFields;
	}

	/**
	 * Get bean fields as array.
	 * 
	 * @param clz bean class
	 * @return bean fields array
	 */
	public static BeanField[] getBeanFieldsAsArray(Class<?> clz) {
		final Map<String, BeanField> beanFields = Beans.getBeanFields(clz);
		return beanFields.values().toArray(new BeanField[beanFields.size()]);
	}
	
}
