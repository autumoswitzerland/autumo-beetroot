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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Utils;

/**
 * PLANT - beetRoot CRUD Generator.
 */
public class Plant {

	protected final static Logger LOG = LoggerFactory.getLogger(Plant.class.getName());

	public static final String RELEASE = "1.2.0";

	private static final String CR = Utils.LINE_SEPARATOR;
	
	private String tableNames[] = null;
	private String singleEntity = null;

	
	public Plant() {
	}
	
	private String getDescription() {
		final String all = CR + CR + getLine() + CR
				+ " PLANT "+RELEASE+" - BeetRoot Generator for creating operable CRUD views" + CR
				+ " based on database entities." + CR
				+ " (c) 2022 autumo GmbH";
		return all;
	}
	
	private Options makeOptions() {
		final Options options = new Options();
		//options.addOption(makeOption("source-directory", true, "Source directory to generate Java classes."));
		//options.addOption(makeOption("entity", false, "Entity to process"));
		return options;
	}
	
	/*
	private Option makeOption(String argName, boolean required, String desc) {
		final Option localOption = new Option(argName, true, desc);
		localOption.setRequired(required);
		localOption.setArgName(argName);
		return localOption;
	}
	*/

	/*
	private void usage() {
		final int width = 80;

		final String usage = "java " + Plant.class.getName() + " <source-directory> <entity>";
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(width, usage, null, makeOptions(), null);

		this.printLine();
		System.out.println("Exit.");
	}
	*/

	private void printLine() {
		System.out.println(getLine());
	}

	private String getLine() {
		return "---------------------------------------------------------------------------------";
	}

	private void initialize(CommandLine aCmdline) throws Exception {
		
        //singleEntity = aCmdline.getOptionValue("entity");
        
		// ALWAYS CHECK TO FIXME :)
        ConfigurationManager.getInstance().initialize();
        //ConfigurationManager.getInstance().initialize("cfg/beetroot-mysql.cfg");
        
		// Are pw's in config encoded?
		boolean pwEncoded = ConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
		// DB connection manager
		DatabaseManager.getInstance().initialize(
				ConfigurationManager.getInstance().getString("db_url"),
				ConfigurationManager.getInstance().getString("db_user"),
				pwEncoded ? 
						ConfigurationManager.getInstance().getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : ConfigurationManager.getInstance().getString("db_password")
			);
	}

	private int readParameters(boolean askRetry) throws Exception {
		
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String val = null;

		if (askRetry) {

			do {
				System.out.println("");
				System.out.println("Process another entity?: ");
				System.out.println("  [y] = Yes ");
				System.out.println("  [n] = No ");
				System.out.print(">");

				val = br.readLine().trim();
				if (val != null && val.equalsIgnoreCase("y")) {
					this.singleEntity = null;
					return 1;
				}
				
			} while (!val.equalsIgnoreCase("y") && !val.equalsIgnoreCase("n"));

			return -1;
			
		} else {
			
			List<String> tableList = new ArrayList<String>();
						
			final Connection conn = DatabaseManager.getInstance().getConnection();
			
			final Statement stmt = conn.createStatement();
			
			if (DatabaseManager.getInstance().isMysqlDb() || DatabaseManager.getInstance().isMariaDb()) {
				ResultSet rs = stmt.executeQuery("SHOW TABLES;");
				while (rs.next())
					tableList.add(rs.getString(1));
				rs.close();
			} else {
				System.out.println("NOTE:");
				System.out.println("At this time CRUD generation is only possible with MySQL and MariaDB.");
				System.out.println("We suggest setting up one of these databases for development and then");
				System.out.println("using the generated templates and code for the target database.");
				System.out.println("Sorry!");
				System.out.println("");
				
				// finish now!
				Utils.normalExit();
			} 
			/*
			else if (DatabaseManager.getInstance().isH2Db()) {
				ResultSet rs = stmt.executeQuery("SHOW TABLES;");
				while (rs.next())
					tableList.add(rs.getString(1));
				rs.close();
			} else if (DatabaseManager.getInstance().isPostgreDb()) {
				
				ResultSet rs = stmt.executeQuery("\\dt");
				while (rs.next())
					tableList.add(rs.getString("name"));
				rs.close();
			} else if (DatabaseManager.getInstance().isOracleDb()) {
				ResultSet rs = stmt.executeQuery("SELECT table_name FROM user_tables;");
				while (rs.next())
					tableList.add(rs.getString("table_name"));
				rs.close();
			}
			*/
			stmt.close();
			conn.close();
			
			int size = tableList.size();
			tableNames = tableList.toArray(new String[size]);

			int d = -1;

			// Shoe entity names
			do {
				System.out.println("");
				System.out.println("Input entity name: ");
				for (int j = 0; j < tableNames.length; j++) {
					System.out.println("  ["+(j+1)+"] = "+tableNames[j]);
				} 
				System.out.println("  [all] = All tables !");
				System.out.print(">");

				val = br.readLine();
				if (val.trim().equalsIgnoreCase("all")) {
					d = 1;
					val = "all";
				} else {
					d = Integer.valueOf(val).intValue();
					val = "one";
				}

			} while (!val.equals("all") && (d < 1 || d > size));

			/*
			// Something else
			do {
				System.out.println("");
				System.out.println("Something: ");
				System.out.println("  [1] = A ");
				System.out.println("  [2] = B ");
				System.out.print(">");

				val = br.readLine();

			} while (d != 1 && d != 2);
			*/

			if (!val.equals("all")) {
				
				singleEntity = tableNames[d-1];
				System.out.println("");
				System.out.println("Generate CRUD templates and code for entity '" + singleEntity + "' (y/n, enter = y) ?): ");
				System.out.println("NOTE: This will overwrite existing generated sources (HTML, java & columns.cfg)!");
				System.out.print(">");
	
				String answer = br.readLine().trim();
				if (answer != null && answer.trim().equalsIgnoreCase("y"))
					return 1;
				else
					return -1;
			
			} else {
				
				System.out.println("");
				System.out.println("Generate CRUD templates and code for ALL (!) entities (y/n, enter = y) ?): ");
				System.out.println("NOTE: This will overwrite existing generated sources (HTML, java & columns.cfg)!");
				System.out.print(">");
				
				String answer = br.readLine().trim();
				if (answer != null && answer.trim().equalsIgnoreCase("y"))
					return 10;
				else
					return -1;
			}
		}

	}

    private void execute() throws Exception {

		System.out.println("");
    	
		// ---- HTML
		
    	this.process(new Fertilizer(singleEntity, "gen/html/index.html", "web/html/", "html"));
    	this.process(new Fertilizer(singleEntity, "gen/html/view.html", "web/html/", "html"));
    	this.process(new Fertilizer(singleEntity, "gen/html/edit.html", "web/html/", "html"));
    	this.process(new Fertilizer(singleEntity, "gen/html/add.html", "web/html/", "html"));

    	
		// ---- columns.cfg
    	
    	this.process(new Fertilizer(singleEntity, "columns.cfg", "web/html/", "cfg"));
    	
    	
		// ---- Java
    	
    	Fertilizer fertilizer = null;
    	fertilizer = new Fertilizer(singleEntity, "gen/java/IndexHandler.java", "src/", "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/ViewHandler.java", "src/", "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/EditHandler.java", "src/", "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/AddHandler.java", "src/", "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/Entity.java", "src/", "java");
    	this.process(fertilizer);
    	
    	
		// ---- Router
    	
		System.out.println("");
		System.out.println("  Add the following lines to your beetRoot Router:\n");
		System.out.println(
				  "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/index\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/view\", "+fertilizer.upperEntityPlural+"ViewHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/edit\", "+fertilizer.upperEntityPlural+"EditHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/add\", "+fertilizer.upperEntityPlural+"AddHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/delete\", DefaultDeleteHandler.class, \""+fertilizer.lowerEntityPlural+"\")\n"
				+ "");
    	
    }
    
	private void process(Fertilizer fertilizer) throws Exception {

		fertilizer.write(fertilizer.parse());
	}

	public final int run(String[] args) {
		
		CommandLine line;

		System.out.println(getDescription());
		this.printLine();

		// No arguments at this time
		try {
			line = new DefaultParser().parse(makeOptions(), args);
		} catch (ParseException exp) {
			System.err.println("Couldn't read program argument. Reason: " + exp.getMessage());
			//usage();
			return -1;
		}

		try {
			
			this.initialize(line);

			do { // loop for more entities
			
				int a = this.readParameters(false);
				if (a == 1) {
					
					System.out.println("");
					
					this.execute();
					
					System.out.println("Entity '"+singleEntity+"' processed!");
					System.out.println("");
					
				} else if (a == 10) {
					
					System.out.println("");
					for (int i = 0; i < tableNames.length; i++) {
						singleEntity = tableNames[i];
						this.execute();
						System.out.println("Entity '"+singleEntity+"' processed!");
					}
					System.out.println("");
				}
				else {
					// finish!
				}
				
			} while (this.readParameters(true) == 1);

		} catch (IllegalArgumentException e) {
			System.err.println("Uhh, something went wrong");
			e.printStackTrace();
			//usage();
			return -1;
		} catch (Exception e) {
			System.err.println("Uhh, something went wrong");
			e.printStackTrace();
			return -1;
		}

		System.out.println("");
		this.printLine();
		System.out.println("");
		System.out.println("NOTE:");
		System.out.println("");
		System.out.println("- Move generated code to own packages and HTML to the desired (language)");
		System.out.println("  directories.");
		System.out.println("");
		System.out.println("- New generation has overwriten possible previous generated sources!");
		System.out.println("");
		System.out.println("TODO's:");
		System.out.println("");
		System.out.println("- Add the routes above to your router!");
		System.out.println("");
		System.out.println("- Adjust mandatory fields in java add handler: only the mandatory fields need a");
		System.out.println("  default value in the add handler that are not present in the GUI!");
		System.out.println("");
		System.out.println("- Remove unwanted GUI fields from 'columns.cfg' for the views 'view', 'add'");
		System.out.println("  and 'edit'.");
		System.out.println("");
		System.out.println("- Also Remove unwanted <col span..> tags in the 'index.html'; e.g. if you");
		System.out.println("  removed standard fields 'id', 'created' and 'modified' from 'columns.cfg'.");
		System.out.println("");
		System.out.println("- Add entity to menu or admin menu and overwrite 'hasAccess' method for every");
		System.out.println("  handler if necessary.");
		// overwrite method access
		System.out.println("");
		System.out.println("Done.");
		System.out.println("");

		return 0;
	}

	public static void main(String[] args) {

		final Plant generator = new Plant();
		final int exit = generator.run(args);
		System.exit(exit);
	}
	
}
