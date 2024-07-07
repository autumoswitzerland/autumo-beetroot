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

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
  * Bean field.
  */
public class BeanField {

	protected static final Logger LOG = LoggerFactory.getLogger(BeanField.class.getName());
	
	private String dbName;
	private String beanName;
	private Class<?> type;
	private boolean isNullable = true;
	private boolean unique = false;

	private Method getterMethod = null;
	private Method setterMethod = null;
	
	/**
	 * Constructor.
	 * 
	 * @param dbName column name
	 * @param beanName attribute name
	 * @param type Java type
	 * @param isNullable is nullable? 
	 * @param unique is unique?
	 * @param getterMethod getter method
	 * @param setterMethod setter method
	 */
	public BeanField(
				String dbName, 
				String beanName, 
				Class<?> type, 
				boolean isNullable, 
				boolean unique, 
				Method getterMethod, 
				Method setterMethod) {
		this.dbName = dbName;
		this.beanName = beanName;
		this.type = type;
		this.isNullable = isNullable;
		this.unique = unique;
		this.getterMethod = getterMethod;
		this.setterMethod = setterMethod;
	}		

	public Method getGetterMethod() {
		return getterMethod;
	}
	
	public Method getSetterMethod() {
		return setterMethod;
	}
	
	public String getDbName() {
		return dbName;
	}
	
	public String getBeanName() {
		return beanName;
	}

	public Class<?> getType() {
		return type;
	}
	
	public boolean isUnique() {
		return unique;
	}
	
	public boolean isNullable() {
		return isNullable;
	}
	
	@Override
	public String toString() {
		try {
			return this.serialize(this);
		} catch (JsonProcessingException e) {
			LOG.error("Couldn't serialize (JSON) DB field!", e);
			return super.toString();
		}
	}

	/**
	 * Serialize this field to JSON.
	 * 
	 * @param field field
	 * @return JSON string
	 * @throws JsonProcessingException JSON processing exception
	 */
	public String serialize(BeanField field) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(field);
	}
	
}
