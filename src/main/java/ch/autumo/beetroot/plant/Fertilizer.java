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
package ch.autumo.beetroot.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.OS;

/**
 * Fertilizer for plant.
 */
public class Fertilizer {
	
	public static final String LOWER_ENTITIES_NAME = "##entitiesname##";
	public static final String UPPER_ENTITIES_NAME = "##Entitiesname##";
	public static final String LOWER_ENTITIES_NAME_PLURAL = "##entitiesname#s#";
	public static final String UPPER_ENTITIES_NAME_PLURAL = "##Entitiesname#s#";
	
	public static final String cruds[] = new String[] {"list", "view", "edit", "add"};
	private Set<String> fieldNames = null;
	private int amountOfFields = -1;
	
	private String dbEntity = null;
	private String resource = null;
	private String outputBaseDir = null;
	private String type = null;
	
	String lowerEntityPlural = null;
	String upperEntityPlural = null;
	String lowerEntity = null;
	String upperEntity = null;
	
	private Map<String, DBField> databaseFields = new HashMap<String, DBField>();
	// foreign key mapping
	private Map<String, String> foreignKeyMap = new HashMap<String, String>(); 
	
	final List<String> importList = new ArrayList<String>();
	
	
	public Fertilizer(String dbEntity, String resource, String outputBaseDir, String type) throws FertilizerException {
		
		this.dbEntity = dbEntity.toLowerCase().trim();
		if (!dbEntity.endsWith("s") && !dbEntity.endsWith("S")) {
			throw new FertilizerException("Entity '"+dbEntity+"' doesn't end with a 's' letter!\n"
					+ "database entities should be named for example:\n"
					+ "users, tasks, properties, rooms, candies, etc.");
		}
		
		this.resource = resource;
		this.outputBaseDir = outputBaseDir;
		this.type = type.toLowerCase().trim();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// Collect foreign keys
			final DatabaseMetaData meta = conn.getMetaData();
			final ResultSet rsk = meta.getImportedKeys(conn.getCatalog(), null, dbEntity);
			while (rsk.next()) {
				String fkColumnName = rsk.getString("FKCOLUMN_NAME");
				String pkTableName = rsk.getString("PKTABLE_NAME");
				String clz = "planted.beetroot.handler." + pkTableName + "." + Beans.tableToClassName(pkTableName) + ".class";
				this.addForeignKeyMapping(fkColumnName, clz);
			}
			rsk.close();
			
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("DESC " + dbEntity + ";");
			
			DBField dbField = null;
			while (rs.next()) {
				String name = rs.getString(1);
				dbField = new DBField(
					name, // Field
					rs.getString(2), // Type
					rs.getString(3).toLowerCase().equals("yes") ? true : false, // Null
					rs.getString(4).toLowerCase().equals("uni") ? true : false, // Null
					rs.getString(5) // default val
				);
				
				databaseFields.put(name, dbField);
			}
			
		} catch (SQLException e) {
			
			throw new FertilizerException("Couldn't read DB fields for entity '"+dbEntity+"'!", e);
			
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				// nothing to do
			}
		}
		
		fieldNames = databaseFields.keySet();
		amountOfFields = fieldNames.size();
	}
	
	private Scanner getNewScanner(String resource) throws FileNotFoundException {
		return new Scanner(new File(resource));
	}

	public void execute() throws FertilizerException {
		
		final String result = this.parse();
		this.write(result);
	}
	
	public void write(String result) throws FertilizerException {
		
		if (!this.outputBaseDir.endsWith("/"))
			this.outputBaseDir += "/";
		
		String fullDir = null;
		if (type.equals("html") || type.equals("cfg"))
			fullDir = this.outputBaseDir + lowerEntityPlural + "/";
		else if (type.equals("java"))
			fullDir = this.outputBaseDir + "planted/beetroot/handler/" + lowerEntityPlural + "/";
		else
			throw new FertilizerException("Template type '"+type+"' is not supported!");
			
		final File dirs = new File(fullDir);
		dirs.mkdirs();

		
		String filename = resource.substring(resource.lastIndexOf("/") + 1, resource.length());
		if (type.equals("java")) {
			if (filename.endsWith("Entity.java"))
				filename = upperEntity + ".java";
			else
				filename = upperEntityPlural + resource.substring(resource.lastIndexOf("/") + 1, resource.length());
		}
		
		String fullPath = fullDir + filename;
		final File target = new File(fullPath);
		
		FileWriter writer = null;
		try {
			writer  = new FileWriter(target);
			writer.write(result);
			writer.close();
			
			System.out.println("  Generated: " + fullPath);
			
		} catch (IOException e) {
			throw new FertilizerException("Couldn't create writer for output file '"+fullPath+"'!", e);
		}
	}

	public String parse() throws FertilizerException {
		
		final StringBuffer result = new StringBuffer();
		
		// ---- Checks
		
		final String entity = dbEntity.toLowerCase().trim();
		if (!entity.endsWith("s")) {
			throw new FertilizerException("Entity '"+entity+"' doesn't end with a 's' letter!\n"
					+ "database entities should be named for example:\n"
					+ "users, tasks, properties, rooms, candies, etc.");
		}

		// ---- Create entity name versions
		
		lowerEntityPlural = entity;
		upperEntityPlural = entity.substring(0, 1).toUpperCase() + entity.substring(1, entity.length());
		lowerEntity = null;
		if (entity.endsWith("ies")) {
			lowerEntity = lowerEntityPlural.substring(0, lowerEntityPlural.length() - 3) + "y";
		} else {
			lowerEntity = lowerEntityPlural.substring(0, lowerEntityPlural.length() - 1);
		}
		upperEntity = lowerEntity.substring(0, 1).toUpperCase() + lowerEntity.substring(1, lowerEntity.length());

		// ---- columns.cfg
		
		if (type.equals("cfg")) {// nothing to parse, all info from DB
			
			for (int i = 0; i < cruds.length; i++) {
				
				LOOP: for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
					final String name = iterator.next();
					//final DBField field = databaseFields.get(name);

					// Don't add 'id' columns to 'add' or 'edit', they are auto-generated!
					if (name.equals("id") && (cruds[i].equals("add") || cruds[i].equals("edit")))
						continue LOOP;
					
					String guiName = "";
					if (name.contains("_")) {
						String parts[] = name.split("_");
						for (int j = 0; j < parts.length; j++) {
							String s = parts[j];
							if (j < parts.length - 1)
								guiName += s.substring(0, 1).toUpperCase() + s.substring(1, s.length()) + " ";
							else
								guiName += s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
						}
					} else {
						guiName = name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
					}
					result.append(cruds[i]+"."+name+"="+guiName+"\n");
				}
				result.append("\n");
			}
			
			result.append("unique=");
			for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
				final String name = iterator.next();
				final DBField field = databaseFields.get(name);
				if (field.isUnique())
					result.append(name+",");
			}
			if (result.charAt(result.length()-1) != '=')
				result.delete(result.length()-1, result.length());
			result.append("\n");
			return result.toString();
		}
		
		final String filename = resource.substring(resource.lastIndexOf("/") + 1, resource.length());
		
		// ---- Create scanner
		
		Scanner sc = null;
		try {
			sc = getNewScanner(resource);
		} catch (Exception e) {
			throw new FertilizerException("Parser couldn't be created for resource '"+resource+"'!", e);
		}
		
		// ---- Parse java & html
		  
		while (sc.hasNextLine()) {
			
			String text = sc.nextLine();

			
			// ---- General
			
			if (text.contains("##entitynameplural##")) {
				text = text.replaceAll("##entitynameplural##", lowerEntityPlural);
			}
			if (text.contains("##Entitynameplural##")) {
				text = text.replaceAll("##Entitynameplural##", upperEntityPlural);
			}
			if (text.contains("##entityname##")) {
				text = text.replaceAll("##entityname##", lowerEntity);
			}
			if (text.contains("##Entityname##")) {
				text = text.replaceAll("##Entityname##", upperEntity);
			}


			// ---- html
			
			if (type.equals("html")) {
				
				if (filename.equals("index.html")) {
					
					if (text.contains("##columns##")) {
						String cols = "";
						int c = 0;
						for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
							c++;
							iterator.next(); // consume!
							if (c == amountOfFields)
								cols += "				<col span=\"1\">";
							else
								cols += "				<col span=\"1\">\n";
						}
						text = text.replace("##columns##", cols);
					}
				}
			}
		
			// ---- Java
			
			if (type.equals("java")) {
				
				if (filename.equals("AddHandler.java")) {
					
					if (text.contains("##mandfields##")) {

						String mands = "";
						for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
							final String name = iterator.next();
							final DBField field = databaseFields.get(name);
							if (!field.isNullable())
								mands += "		// fields.put(\""+name+"\", \"<DEFAULT-VALUE>\");\n";
						}
						text = text.replace("##mandfields##", mands);
					}
				}
				
				if (filename.equals("ViewHandler.java") || filename.equals("IndexHandler.java")) {
					
					if (text.contains("##columns##")) {

						String cols = "";
						int c = 0;
						for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
							c++;
							final String name = iterator.next();
							if (c == amountOfFields)
								cols += "			case \""+name+"\": return \"<td>\" + Utils.getValue(set, columnName) + \"</td>\";";
							else
								cols += "			case \""+name+"\": return \"<td>\" + Utils.getValue(set, columnName) + \"</td>\";\n";
						}
						text = text.replace("##columns##", cols);
					}
				}
				
				if (filename.equals("Entity.java")) {
					
					String beanContents = this.processBean();
					/*
					String imports = "";
					for (Iterator<String> iterator = importList.iterator(); iterator.hasNext();) {
						String imp = iterator.next();
						imports += "import " + imp + ";\n";
					}
					*/
					//text = text.replace("##beanImports##", imports);
					
					text = text.replace("##beanContents##", beanContents);
				}
				
			}

			result.append(text + OS.LINE_SEPARATOR);
		}
		
		return result.toString();
	}
	
	private String processBean() {

		final StringBuffer contents = new StringBuffer();
		
		for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
			
			String name = iterator.next();
			DBField dbField = databaseFields.get(name);
			String sqlType = dbField.getType().toLowerCase();
			
			String javaType = this.getJavaType(sqlType);

			String propertyName = this.propertyName(dbField.getName());
			
			contents.append("    private " + javaType + " " + propertyName + ";\n");
			contents.append("\n");
			
			String properyNameMethodPart = this.propertyNameMethodPath(dbField.getName());
			
			contents.append("    public " + javaType + " get" + properyNameMethodPart + "() {\n");
			contents.append("        return "+propertyName+";\n");
			contents.append("    }\n");
			contents.append("\n");

			contents.append("    public void set" + properyNameMethodPart + "("+javaType+" "+propertyName+") {\n");
			contents.append("        this."+propertyName+" = "+propertyName+";\n");
			contents.append("    }\n");
			contents.append("\n");
			
		}

		// Display field
		String displayField = "id";
		final Set<String> dbColNames = this.databaseFields.keySet();
		if (dbColNames.contains("email"))
			displayField = "email";
		else if (dbColNames.contains("name"))
			displayField = "name";
		else if (dbColNames.contains("description"))
			displayField = "description";
		contents.append("    public String getDisplayField() {\n");
		contents.append("        return \""+displayField+"\";\n");
		contents.append("    }");
		

		// Foreign key map <dbid:dbfield>
		if (this.hasForeignKeys()) {
			String fkMap = "        return java.util.Map.ofEntries(\n";
			Set<String> keys = this.foreignKeyMap.keySet();
			int i = 1;
			for (String fkName : keys) {
				if (i < keys.size())
					fkMap +=  "                java.util.Map.entry(\"" + fkName + "\", " + this.foreignKeyMap.get(fkName) + "),\n";
				else
					fkMap +=  "                java.util.Map.entry(\"" + fkName + "\", " + this.foreignKeyMap.get(fkName) + ")\n";
			}
			fkMap +=  "            );\n";
			contents.append("\n");
			contents.append("\n");
			contents.append("    public java.util.Map<String, Class<?>> getForeignReferences() {\n");
			contents.append(fkMap);
			contents.append("    }");
		}
		
		return contents.toString();
	}
	
	private String propertyNameMethodPath(String property) {
		
		String parts[] = property.toLowerCase().split("_");
		String partName = "";
		for (int i = 0; i < parts.length; i++) {
			partName += parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1, parts[i].length()); 
		}
		return partName;
	}
	
	private String propertyName(String property) {
		
		String parts[] = property.toLowerCase().split("_");
		String partName = "";
		for (int i = 0; i < parts.length; i++) {
			if (i == 0)
				partName += parts[i];
			else
				partName += parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1, parts[i].length());
		}
		return partName;
	}	

	private String getJavaType(String sqlType) {

		String javaType = null;
		
		if (sqlType.startsWith("bigint") || sqlType.startsWith("bigserial") || sqlType.startsWith("int8")  || sqlType.startsWith("serial8")) {
			javaType= "long";
		}
		if (sqlType.startsWith("blob") || sqlType.startsWith("byte") || sqlType.startsWith("clob")) {
			javaType= "byte";
		}
		if (sqlType.startsWith("bit") || sqlType.startsWith("tinyint") || sqlType.startsWith("boolean")) {
			javaType = "boolean";
		}
		if (sqlType.startsWith("varchar") || sqlType.startsWith("nvarchar") || sqlType.startsWith("lvarchar") || sqlType.startsWith("char") || sqlType.startsWith("nchar") || sqlType.startsWith("text") || sqlType.startsWith("character")) {
			javaType = "String";
		}
		
		if (sqlType.startsWith("datetime")) {
			javaType= "java.sql.Timestamp";
			if (!importList.contains(javaType))
				importList.add(javaType);
		} else if (sqlType.startsWith("date")) { // don't overwrite date-time!
			javaType= "java.sql.Date";
			if (!importList.contains(javaType))
				importList.add(javaType);
		}
		
		if (sqlType.startsWith("decimal") || sqlType.startsWith("numeric")) {
			javaType= "java.math.BigDecimal";
			if (!importList.contains(javaType))
				importList.add(javaType);
		}
		if (sqlType.startsWith("float") || sqlType.startsWith("double")) {
			javaType= "double";
		}
		if (sqlType.startsWith("smallfloat")) {
			javaType= "float";
		}
		if (sqlType.startsWith("int") || sqlType.startsWith("integer") || sqlType.equals("serial")) { // overwrite serial8 if necessary
			javaType = "int";
		}
		if (sqlType.startsWith("smallint")) {
			javaType = "short";
		}
		if (sqlType.startsWith("real")) {
			javaType= "float";
		}	
		
		return javaType;
	}
	
	/**
	 * Add a foreign key mapping.
	 * 
	 * @param foreignKeyName foreign key name
	 * @param fullClassname full qualified class name of referenced object
	 */
	private void addForeignKeyMapping(String foreignKeyName, String fullClassName) {
		this.foreignKeyMap.put(foreignKeyName, fullClassName);
	}
	
	/**
	 * Does the entity has foreign keys?
	 * 
	 * @return true if processd entity has at least one foreign key
	 */
	private boolean hasForeignKeys() {
		return !this.foreignKeyMap.isEmpty();
	}
	
	public class FertilizerException extends Exception {

		private static final long serialVersionUID = 1L;

		public FertilizerException(String msg) {
			super(msg);
		}
		public FertilizerException(String msg, Throwable t) {
			super(msg, t);
		}
		
	}
	
}
