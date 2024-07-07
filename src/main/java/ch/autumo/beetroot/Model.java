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

import ch.autumo.beetroot.annotations.Column;
import ch.autumo.beetroot.plant.Plant;
import ch.autumo.beetroot.utils.BeanField;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.DB;
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
 * 
 * Note that all bean property names used within methods are
 * case sensitive!
 */
public abstract class Model implements Entity {

    private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(Model.class.getName());
    
	/**
	 * Unassigned id; a model has been created
	 * but or loaded from database that has no id.
	 */
	public static final int ID_UNASSIGNED = 0;
	
	/**
	 * Invalid ID; used when an object cannot be
	 * read or stored.
	 */
	public static final int ID_INVALID = -1;
	
	/**
	 * Pseudo ID of an a stored model that is
	 * a many-to-many-relation-model- 
	 */
	public static final int ID_M2M_PSEUDO = -2;
    
    
	/** Encode password properties when storing in DB? */
	private static boolean dbPwEnc = false;
	static {
		if (BeetRootConfigurationManager.getInstance().isInitialized())
			dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
	}
	
	/**
	 * Bean model map.
	 */
    private static final Map<String, Map<String, BeanField>> MODEL = new HashMap<String, Map<String, BeanField>>();
    
    /**
     * Stored in DB?
     */
	private boolean isStored = false;
	
	/**
	 * Unique ID.
	 */
	@Column (name = "id")
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
     * Is the field specified by the bean property name nullable?
     * 
     * @param beanPropertyName bean property name
     * @return true if so
     */
    public boolean isNullable(String beanPropertyName) {
    	Beans.updateModel(this, MODEL);
    	final String tableName = Beans.classToTable(modelClass());
		final Map<String, BeanField> beanFields = MODEL.get(tableName);
    	final BeanField field = beanFields.get(Beans.beanPropertyName2DbName(beanPropertyName));
    	return field.isNullable();
    }

    /**
     * Is the field specified by the bean property name unique?
     * 
     * @param beanPropertyName bean property name
     * @return true if so
     */
    public boolean isUnique(String beanPropertyName) {
    	Beans.updateModel(this, MODEL);
    	final String tableName = Beans.classToTable(modelClass());
		final Map<String, BeanField> beanFields = MODEL.get(tableName);
    	final BeanField field = beanFields.get(Beans.beanPropertyName2DbName(beanPropertyName));
    	return field.isUnique();
    }
    
    /**
     * Get a value from this entity except the id.
     * You even can use DB field name such as 'user_id'
     * or 'max_asset_value'.
     * 
     * @param fieldName field name
     * @return bean value
     */
	public String get(String fieldName) {
		Method method;
		String mName = null;
		String val = null;
		
		if (fieldName.contains("_")) {
			String newName = "";
			boolean nextIsUpper = false;
			for (char c : fieldName.toCharArray()) {
				if (nextIsUpper) {
					c = Character.toUpperCase(c);
					nextIsUpper = false;
				}
			    if (c != '_')
			    	newName += c;
			    else
			    	nextIsUpper = true;
			}
			fieldName = newName;
		}
		
		try {
			mName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
			method = modelClass().getDeclaredMethod(mName);
			if (method != null) { // good!
				final Object oVal = method.invoke(this);
				val = oVal.toString();
			}
		} catch (Exception e) {
			LOG.error("Couldn't get property '"+fieldName+"' from bean class '"+modelClass().getName()+"'!", e);
			// no value
		}
		return val;
	}
	
	/**
	 * Save this entity bean to database.
	 * 
	 * @return generated id or -1 if entity couldn't be saved or 
	 * 			the pseudo id -2 for many-to-many relation tables 
	 * 			that have no id
	 */
	public Integer save() {
		
		String stmtParts[] = null;
		
		try {
			stmtParts = this.getStatementParts(true);
		} catch (Exception e) {
			LOG.error("Entity not saved!", e);
			return Integer.valueOf(ID_INVALID);
		}
		
		final Integer saveId = DB.insert(this, stmtParts[0], stmtParts[1]);
		if (saveId == ID_INVALID)
			return saveId;
		
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
			stmtParts = this.getStatementParts(false);
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
	 * @throws Exception exception
	 */
	public void delete() throws Exception {
		if ((id == ID_UNASSIGNED || id == ID_M2M_PSEUDO) && this.getForeignReferences().size() > 1) {
			// we can assume it is a many-to-many bean! It's wild...
			final Set<String> foreignDbKeys = this.getForeignReferences().keySet();
			DB.delete(this, foreignDbKeys);
		} else {
			DB.delete(this);
		}
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
	 * @param referenceClass referenced entity class
	 * @return referenced entity if any or null
	 */
	public Model getAssociatedReference(Class<?> referenceClass) {
		return this.getAssociatedReference(Beans.classToRefBeanId(referenceClass) );
	}
	
	/**
	 * Get associated (parent) entity.
	 * 
	 * @param referenceBeanPropertyName referenced entity bean property name
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

	@Override
	public boolean equals(Object obj) {
		final Model other = (Model) obj;
		return this.modelClass().equals(other.modelClass())
				&& this.getId() == other.id;  
	}

	/**
	 * Serialize this object to JSON.
	 * 
	 * @param entity entity
	 * @return JSON string
	 * @throws JsonProcessingException JSON processing exception
	 */
	public String serialize(Model entity) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(entity);
	}

	/**
	 * De-serialize a JSON string to an entity.
	 * 
	 * @param json JSON string
	 * @return entity
	 * @throws JsonProcessingException JSON processing exception
	 */
	public Model deserialize(String json) throws JsonProcessingException {
		final ObjectMapper mapper = new ObjectMapper();
		return (Model) mapper.readValue(json, modelClass());
	}

	/**
	 * Get database statements insert clause parts (columns and values).
	 * - column clause part; "a,b,c"
	 * - value clause part; "'1','2','3'"
	 *
	 * @param insert true if insert, false if update
	 * @return database statements clause parts
	 * @throws Exception
	 */
	private String[] getStatementParts(boolean insert) throws Exception {
		
		// Update beans model with this model if not already cached
		Beans.updateModel(this, MODEL);
		
		String columns = "";
		String values = "";
		
		final String tableName = Beans.classToTable(modelClass());
		final Map<String, BeanField> beanFields = MODEL.get(tableName);
		
		if (beanFields == null) {
			LOG.error("The bean model is corrupted; check if bean '"+modelClass()+"' has been generated by PLANT or at least it has one column ('@Column') defined!");
		}
		
		final Set<String> dbColnames = beanFields.keySet();
		for (Iterator<String> iterator = dbColnames.iterator(); iterator.hasNext();) {
			
			final String dBname = iterator.next();
			final boolean next = iterator.hasNext();
			if (dBname.equalsIgnoreCase("id"))
				continue;
			if (dBname.equalsIgnoreCase("created") && !insert)
				continue;
			
			// 1. Column names
			if (next)
				columns += dBname + ",";		
			else
				columns += dBname;
			
			// 2. Values
			String val = null;
			final BeanField beanField = beanFields.get(dBname);
			final Object oVal = beanField.getGetterMethod().invoke(this);
			if (oVal != null)
				val = oVal.toString();

			val = DB.escapeValuesForDb(val);
			if (dbPwEnc && dBname.equalsIgnoreCase("password")) {
				try {
					val = Security.hashPw(val);
				} catch (UtilsException e) {
					throw new Exception("Couldnt' hash password in bean class '"+getClass()+"'!", e);
				}
			}
			if (val != null) {
				// Informix wants 't' or 'f'
				if (val.equalsIgnoreCase("true")) {
					val = "1";
				}
				if (val.equalsIgnoreCase("false")) {
					val = "0";
				}
			}
			
			if ((dBname.equalsIgnoreCase("created") && insert) || dBname.equalsIgnoreCase("modified")) {
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
     * Read an entity with given ID.
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
	 * e.g. 'age &gt;= ?'.
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
	 * e.g. 'age &gt;= ?'.
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
	 * e.g. 'age &gt;= ?'.
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
	 * e.g. 'age &gt;= ?'.
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
	 * Get display value of bean.
	 * 
	 * @return display value
	 */
	public String getDisplayValue() {
		return this.get(getDisplayField());
	}
    
    /**
     * Get reference entity map (all referenced entities).
     * 
     * The (PLANT-generated) mapping is:<br>
     * &lt;databaseFieldName&gt; -&gt; &lt;Class&gt;
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
