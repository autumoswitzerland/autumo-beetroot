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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
  * DB field.
  */
public class DBField {

	protected final static Logger LOG = LoggerFactory.getLogger(DBField.class.getName());
	
	private String name;
	private String type;
	private boolean isNullable = true;
	private boolean unique = false;
	private String defaultVal;

	/**
	 * Constructor.
	 * 
	 * @param name column name
	 * @param type column type
	 * @param isNullable is nullable? 
	 * @param unique is unique?
	 * @param defaultVal column default value
	 */
	public DBField(String name, String type, boolean isNullable, boolean unique, String defaultVal) {
		this.name = name;
		this.type = type;
		this.isNullable = isNullable;
		this.unique = unique;
		this.defaultVal = defaultVal;
	}		
	
	public String getDefaultVal() {
		return defaultVal;
	}
	
	public String getName() {
		return name;
	}
	
	public String getType() {
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
	public String serialize(DBField field) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(field);
	}
	
}
