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
package ch.autumo.beetroot;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.autumo.beetroot.plant.Plant;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.DBField;
import ch.autumo.beetroot.utils.Security;
import ch.autumo.beetroot.utils.Time;
import ch.autumo.beetroot.utils.UtilsException;


/**
 * Model for all beetRoot entities. Basically an entity works fine
 * in the beetRoot CRUD views when only implementing the interface
 * {@link Entity}, but this model adds convenience functionalities
 * to entity beans:
 * 
 * - Select associated (parent) entity
 * - Select referenced entities
 * - List and find entities 
 * - Save, update and delete
 */
public abstract class Model implements Entity {
	
    private static final long serialVersionUID = 1L;
    
	protected final static Logger LOG = LoggerFactory.getLogger(Model.class.getName());
    
	/** Encode password properties when storing in DB? */
	private static boolean dbPwEnc = false;
	static {
		if (BeetRootConfigurationManager.getInstance().isInitialized())
			dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
	}
	
	/**
	 * Database model map.
	 */
    private static final Map<String, Map<String, DBField>> MODEL = new HashMap<String, Map<String,DBField>>();
    
    /**
     * Stored in DB?
     */
	private boolean isStored = false;
	
	/**
	 * Unique ID.
	 */
	private int id;
	
	/**
	 * Get the ID.
	 * 
	 * Currently only number IDs are provided.
	 * If you need another type of ID, add it as
	 * a separate database field.
	 */
    public int getId() {
        return id;
    }

    /**
     * Set the ID.
     * 
     * Should not be called by your code!
     */
    public void setId(int id) {
        this.id = id;
    }

    
    /**
     * Get a value from this entity except the id.
     * 
     * @param property bean property name
     * @return bean value
     */
	public String get(String beanPropertyName) {
		Method method;
		String mName = null;
		String val = null;
		try {
			mName = "get" + beanPropertyName.substring(0, 1).toUpperCase() + beanPropertyName.substring(1, beanPropertyName.length());
			method = modelClass().getDeclaredMethod(mName);
			if (method != null) { // good!
				final Object oVal = method.invoke(this);
				val = oVal.toString();
			}
		} catch (Exception e) {
			LOG.error("Couldn't get property '"+beanPropertyName+"' from bean class '"+modelClass().getName()+"'!", e);
			// no value
		}
		return val;
	}
	
	/**
	 * Save this entity bean to database.
	 * 
	 * @return generated ID
	 */
	public Integer save() {
		String stmtParts[] = null;
		try {
			stmtParts = this.getStatementParts();
		} catch (Exception e) {
			LOG.error("Entity not saved!", e);
			Integer.valueOf(-1);
		}
		final Integer saveId = DB.insert(this, stmtParts[0], stmtParts[1]);
		this.setId(saveId.intValue());
		this.isStored = true;
		return saveId;
	}

	/**
	 * Update this entity bean in database.
	 */
	public void update() {
		String stmtParts[] = null;
		try {
			stmtParts = this.getStatementParts();
		} catch (Exception e) {
			LOG.error("Entity not updated!", e);
			Integer.valueOf(-1);
		}
		DB.update(this, stmtParts[0], stmtParts[1]);
		this.isStored = true;
	}
	
	/**
	 * Delete this entity bean!
	 * 
	 * @throws Exception
	 */
	public void delete() throws Exception{
		DB.delete(this);
		isStored = false;
	}

	/**
	 * Set stored state.
	 * 
	 * @param isStored true if this bean us stored in database
	 */
	public void setStored(boolean isStored) {
		this.isStored = isStored;
	}
	
	/**
	 * Is this entity bean stored in database?
	 * 
	 * @return true if so
	 */
	public boolean isStored() {
		return isStored;
	}

	/**
	 * Get associated (parent) entity.
	 * 
	 * @param referenceEntity referenced entity class
	 * @return referenced entity if any or null
	 */
	public Model getAssociatedReference(Class<?> referenceClass) {
		return this.getAssociatedReference(Beans.classToRefBeanId(referenceClass) );
	}
	
	/**
	 * Get associated (parent) entity.
	 * 
	 * @param referenceEntity referenced entity bean property name
	 * @return referenced entity if any or null
	 */
	public Model getAssociatedReference(String referenceBeanPropertyName) {
		final Object r =  this.get(referenceBeanPropertyName);
		final Integer id = Integer.valueOf(r.toString());
		final String dbRefColumnName = Beans.beanPropertyName2DbName(referenceBeanPropertyName); 
		Map<String, Class<?>> refs = null;
		try {
			refs = Beans.getForeignReferences(this);
			final Set<String> keys = refs.keySet();
			Class<?> refClass = null;
			for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
				final String v = iterator.next();
				if (v.equals(dbRefColumnName))
					refClass = refs.get(v);
			}
			return DB.selectRecord(refClass, id);
		} catch (Exception e) {
			LOG.error("Couldn't get associated entity from reference property '"+referenceBeanPropertyName+"'!", e);
			return null;
		}
	}
	
	/**
	 * List referenced entities of the referenced class type.
	 * 
	 * @param referenceClass reference class
	 * @return list of entities that reference to this entity
	 */
	public List<Model> listReferences(Class<?> referenceClass) {
		final String dbRefColName = Beans.classToRefDbId(modelClass());
		try {
			return DB.selectRecords(referenceClass, dbRefColName + " = ?", new Object[] {Integer.valueOf(this.getId())});
		} catch (Exception e) {
			LOG.error("Couldn't get referenced entities '"+referenceClass.getName()+"' of entity '"+modelClass().getName()+"'!", e);
			return null;
		}
	}

	@Override
	public String toString() {
		try {
			return this.serialize(this);
		} catch (JsonProcessingException e) {
			LOG.error("Couldn't serialize (JSON) bean!", e);
			return super.toString();
		}
	}

	/**
	 * Serialize this object to JSON.
	 * 
	 * @param entity entity
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	public String serialize(Entity entity) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(entity);
	}

	/**
	 * De-serialize a JSON string to an entity.
	 * 
	 * @param json JSON string
	 * @return entity
	 * @throws JsonProcessingException
	 */
	public Entity deserialize(String json) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return (Entity) mapper.readValue(json, modelClass());
	}

	/**
	 * Get database statements insert clause parts (columns and values).
	 * - column clause part; "a,b,c"
	 * - value clause part; "'1','2','3'"
	 *
	 * @return database statements clause parts
	 * @throws Exception
	 */
	private String[] getStatementParts() throws Exception {
		// Update database model with this bean model if not already cached
		DB.updateModel(this, MODEL);
		String columns = "";
		String values = "";
		final String tableName = Beans.classToTable(modelClass());
		final Map<String, DBField> dbFields = MODEL.get(tableName);
		final Set<String> colnames = dbFields.keySet();
		for (Iterator<String> iterator = colnames.iterator(); iterator.hasNext();) {
			
			// 1. Column names
			final String name = iterator.next();
			final boolean next = iterator.hasNext();
			if (name.equals("id"))
				continue;
			if (next)
				columns += name + ",";		
			else
				columns += name;
			
			// 2. Values
			String val = "null";
			BeanInfo info;
			try {
				info = Introspector.getBeanInfo(modelClass());
			} catch (IntrospectionException e) {
				throw new Exception("Entity not saved! Couldnt' get bean info of bean class '"+getClass()+"'!", e);
			}
			final PropertyDescriptor descs[] = info.getPropertyDescriptors();
			for (int j = 0; j < descs.length; j++) {
				
				final String beanPropName = descs[j].getName();
				if (beanPropName.equals("stored") || beanPropName.equals("class") || beanPropName.equals("id"))
					continue;
				
				final String tColName = Beans.beanPropertyName2DbName(beanPropName);
				if (tColName.equals(name)) { // DB field found!
					
					Method method;
					String mName = null;
					try {
						mName = "get" + beanPropName.substring(0, 1).toUpperCase() + beanPropName.substring(1, beanPropName.length());
						method = modelClass().getDeclaredMethod(mName);
						if (method != null) { // good!
							final Object oVal = method.invoke(this);
							val = oVal.toString();
						}
					} catch (Exception e) {
						throw new Exception("Couldnt' access bean value from method '"+mName+"' in bean class '"+modelClass().getName()+"'!", e);
					}
				}
			}
			val = DB.escapeValuesForDb(val);
			if (dbPwEnc && name.equals("password")) {
				try {
					val = Security.hashPw(val);
				} catch (UtilsException e) {
					throw new Exception("Couldnt' hash password in bean class '"+getClass()+"'!", e);
				}
			}
			// Informix wants 't' or 'f'
			if (val.equalsIgnoreCase("true")) {
				val = "1";
			}
			if (val.equalsIgnoreCase("false")) {
				val = "0";
			}
			// if there's a column that is mapped 
			// to the DB column 'created', overwrite it!
			if (name.equals("created")) {
				if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
					if (next)
						values += Time.nowTimeStamp() + ",";
					else
						values += Time.nowTimeStamp();
				} else {
					if (next)
						values += "'" + Time.nowTimeStamp() + "',";
					else
						values += "'" + Time.nowTimeStamp() + "'";
				}
			} else { // default case
				if (next)
					values += "'"+val+"',";			
				else
					values += "'"+val+"'";
			}
		}
		if (columns.endsWith(","))
			columns = columns.substring(0, columns.length()-1);
		if (values.endsWith(","))
			values = values.substring(0, values.length()-1);
		return new String[] {columns, values};
	}
	
    /**
     * Returns name of corresponding table.
     *
     * @return name of corresponding table.
     */
    public String getTableName() {
        return Beans.classToTable(modelClass());
    }

    /**
     * Read an entity wioth given ID.
     * 
     * @param entity entity bean class
     * @param id ID
     * @return entity
     */
	public static Model read(Class<?> entity, int id) {
		try {
			return DB.selectRecord(entity, id);
		} catch (Exception e) {
			LOG.error("Couldn't read bean class '"+entity.getName()+"' with id = '"+id+"'!", e);
			return null;
		}		
	}
    
	/**
	 * List all entities of the given entity bean.
	 * Be aware: This doesn't limit the amount of
	 * records selected in database!
	 * 
	 * @param entity entity bean class
	 * @return entities
	 */
	public static List<Model> listAll(Class<?> entity) {
		try {
			return DB.selectRecords(entity);
		} catch (Exception e) {
			LOG.error("Couldn't list bean class '"+entity.getName()+"'!", e);
			return null;
		}
	}

	/**
	 * List all entities of the given entity bean with specific condition,
	 * e.g. 'age => ?'.
	 * 
	 * @param entity entity bean class
	 * @param condition SQL condition
	 * @param value value for the condition with one argument
	 * @return entities
	 */
	public static List<Model> where(Class<?> entity, String condition, Object value) {
		return Model.where(entity, condition, new Object[] {value});
	}
	
	/**
	 * List all entities of the given entity bean with specific condition,
	 * e.g. 'age => ?'.
	 * 
	 * @param entity entity bean class
	 * @param condition SQL condition
	 * @param values values for the condition
	 * @return entities
	 */
	public static List<Model> where(Class<?> entity, String condition, Object values[]) {
		try {
			return DB.selectRecords(entity, condition, values);
		} catch (Exception e) {
			LOG.error("Couldn't execute where clause '"+condition+"' with bean class '"+entity.getName()+"'!", e);
			return null;
		}
	}

	/**
	 * Get first entity of the given entity bean with specific condition,
	 * e.g. 'age => ?'.
	 * 
	 * @param entity entity bean class
	 * @param condition SQL condition
	 * @param value value for the condition with one argument
	 * @return entity
	 */
	public static Model findFirst(Class<?> entity, String condition, Object value) {
		return Model.findFirst(entity, condition, new Object[] {value});
	}
	
	/**
	 * Get first entity of the given entity bean with specific condition,
	 * e.g. 'age => ?'.
	 * 
	 * @param entity entity bean class
	 * @param condition SQL condition
	 * @param values values for the condition
	 * @return entity
	 */
	public static Model findFirst(Class<?> entity, String condition, Object values[]) {
		try {
			final List<Model> entities = DB.selectRecords(entity, condition, values, 1, DB.SORT_BY_ID);
			if (entities.size() > 0)
				return entities.get(0);
			else
				return null;
		} catch (Exception e) {
			LOG.error("Couldn't execute where clause '"+condition+"' with bean class '"+entity.getName()+"'!", e);
			return null;
		}
	}

	/**
	 * Default display field.
	 * 
	 * @return display field
	 */
    public String getDisplayField() {
    	return "id";
    }

    /**
     * Get reference entity map (all referenced entities).
     * 
     * @return reference entity map
     */
    public java.util.Map<String, Class<?>> getForeignReferences() {
    	return new HashMap<String, Class<?>>(); // empty map
    }
    
	/**
	 * Get model base class, usually overwritten
	 * by the {@link Plant} generator.
	 *  
	 * @return model base class
	 */
	public abstract Class<?> modelClass();
	
}
