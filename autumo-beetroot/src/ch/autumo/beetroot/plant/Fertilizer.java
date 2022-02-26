/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
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

import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.Utils;

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
	
	final List<String> importList = new ArrayList<String>();
	
	
	public Fertilizer(String dbEntity, String resource, String outputBaseDir, String type) throws FertilizerException {
		
		this.dbEntity = dbEntity.toLowerCase().trim();
		if (!dbEntity.endsWith("s")) {
			throw new FertilizerException("Entity '"+dbEntity+"' doesn't end with a 's' letter!\n"
					+ "database entities should be named for example:\n"
					+ "users, tasks, properties, rooms, candies, etc.");
		}
		
		this.resource = resource;
		this.outputBaseDir = outputBaseDir;
		this.type = type.toLowerCase().trim();
		
		Connection conn;
		try {
			conn = DatabaseManager.getInstance().getConnection();
			final Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("DESC " + dbEntity + ";");
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
			rs.close();
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			throw new FertilizerException("Couldn't read DB fields for entity '"+dbEntity+"'!", e);
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
				
				for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext();) {
					final String name = iterator.next();
					//final DBField field = databaseFields.get(name);
					String upperName = name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
					result.append(cruds[i]+"."+name+"="+upperName+"\n");
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
								mands += "		fields.put(\""+name+"\", \"<DEFAULT-VALUE>\");\n";
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
								cols += "			case \""+name+"\": return \"<td>\" + set.getString(columnName) + \"</td>\";";
							else
								cols += "			case \""+name+"\": return \"<td>\" + set.getString(columnName) + \"</td>\";\n";
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

			result.append(text + Utils.LINE_SEPARATOR);
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

			contents.append("    private " + javaType + " " + dbField.getName() + ";\n");
			contents.append("\n");
			
			String properyNameMethodPart = this.propertyNameMethodPath(dbField.getName());
			String propertyName = this.propertyName(dbField.getName());
			
			contents.append("    public " + javaType + " get" + properyNameMethodPart + "() {\n");
			contents.append("        return "+propertyName+";\n");
			contents.append("    }\n");
			contents.append("\n");

			contents.append("    public void set" + properyNameMethodPart + "("+javaType+" "+propertyName+") {\n");
			contents.append("        this."+propertyName+" = "+propertyName+";\n");
			contents.append("    }\n");
			contents.append("\n");
			
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
