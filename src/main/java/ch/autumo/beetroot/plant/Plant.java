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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Colors;
import ch.autumo.beetroot.utils.Utils;

/**
 * PLANT - beetRoot CRUD Generator.
 */
public class Plant {

	protected final static Logger LOG = LoggerFactory.getLogger(Plant.class.getName());

	public static final String RELEASE = Constants.APP_VERSION;

	private static final String CR = Utils.LINE_SEPARATOR;
	
	private String tableNames[] = null;
	private String singleEntity = null;

	
	public Plant() {
	}

	private String getBanner() {
		boolean coloredBanner = true;
		if (Utils.isWindows()) {
			int v = -1;
			String vstr = System.getProperty("os.version");
			try {
				v = Integer.valueOf(vstr).intValue();
				if (v < 10)
					coloredBanner = false;
			} catch (Exception e) {
				coloredBanner = false;
			}
		}
		String banner = CR + CR + 
				" __________.____       _____    __________________" + CR +
				" \\______   \\    |     /  _  \\   \\      \\__    ___/" + CR +
				"  |     ___/    |    /  /_\\  \\  /   |   \\|    |" + CR +
				"  |    |   |    |___/    |    \\/    |    \\    |" + CR + 
				"  |____|   |_______ \\____|__  /\\____|__  /____|" + CR +  
				"                   \\/       \\/         \\/";
		if (coloredBanner)
			banner = Ansi.colorize(banner, Attribute.BRIGHT_GREEN_TEXT());
		
		return banner;
	}
	
	private String getDescription() {
		final String all = 
				Colors.cyan(" PLANT "+RELEASE) + " - BeetRoot Generator for creating operable CRUD views" + CR
				+ " based on database entities." + CR
				+ " (c) 2023 autumo Ltd. Switzerland";
		return all;
	}
	
	private Options makeOptions() {
		final Options options = new Options();
		options.addOption(makeOption("config", false, "Optional configuration file path (if it's not default 'cfg/beetroot.cfg')"));
		return options;
	}
	
	private Option makeOption(String argName, boolean required, String desc) {
		final Option localOption = new Option(argName, true, desc);
		localOption.setRequired(required);
		localOption.setArgName(argName);
		return localOption;
	}

	private void usage() {
		final int width = 80;
		final String usage = "java "+Plant.class.getName()+" <config>";
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(width, usage, null, makeOptions(), null);
		this.printLine();
		System.out.println("Exit.");
	}

	private void printLine() {
		System.out.println(getLine());
	}

	private String getLine() {
		return "---------------------------------------------------------------------------------";
	}

	private void initialize(CommandLine aCmdline) throws Exception {
		
		final String argsList[] = aCmdline.getArgs();
		
		if (argsList.length > 1) {
			usage();
			Utils.invalidArgumentsExit();
		}
        
		// DB connection manager
		BeetRootDatabaseManager.getInstance().initialize();
	}

	private int readParameters(boolean askRetry) throws Exception {
		
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String val = null;

		if (askRetry) {

			do {
				System.out.println("");
				System.out.println(Colors.yellow("Process another entity? (enter=y)") + ": ");
				System.out.println("  [y] = Yes ");
				System.out.println("  [n] = No ");
				System.out.print(">");

				val = br.readLine();
				if (val != null && (val.trim().equalsIgnoreCase("y") || val.equals(""))) {
					this.singleEntity = null;
					return 1;
				}
				
			} while (!val.equalsIgnoreCase("y") && !val.equalsIgnoreCase("n"));

			return -1;
			
		} else {
			
			List<String> tableList = new ArrayList<String>();

			Connection conn = null;
			Statement stmt = null;
			
			try {
				
				conn = BeetRootDatabaseManager.getInstance().getConnection();
				stmt = conn.createStatement();
			
				if (BeetRootDatabaseManager.getInstance().isMysqlDb() || BeetRootDatabaseManager.getInstance().isMariaDb()) {
					ResultSet rs = stmt.executeQuery("SHOW TABLES;");
					while (rs.next())
						tableList.add(rs.getString(1));
					rs.close();
				} else {
					System.out.println(" "+Colors.yellow("NOTE") + ": At this time CRUD generation is only possible with MySQL and MariaDB.");
					System.out.println(" We suggest setting up one of these databases for development and then using");
					System.out.println(" the generated templates and code for the target database. Exit.");
					this.printLine();
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
			
			} finally {
				
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			
			int size = tableList.size();
			tableNames = tableList.toArray(new String[size]);

			int d = -1;

			// Shoe entity names
			do {
				System.out.println("");
				System.out.println(Colors.yellow("Input entity name") + ": ");
				for (int j = 0; j < tableNames.length; j++) {
					System.out.println("  ["+(j+1)+"] = "+tableNames[j]);
				} 
				System.out.println("  [all] = All tables !");
				System.out.println("  [x] = Exit");
				System.out.print(">");

				val = br.readLine();
				if (val.trim().equalsIgnoreCase("all")) {
					d = 1;
					val = "all";
				} else if (val.trim().equalsIgnoreCase("x")) {
					System.out.println("Bye.");
					System.out.println("");
					Utils.normalExit();
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
				System.out.println(Colors.yellow("NOTE") + ": This will overwrite existing generated sources (HTML, java & columns.cfg)!");
				System.out.print(">");
	
				String answer = br.readLine();
				if (answer != null && (answer.trim().equalsIgnoreCase("y") || answer.equals("")))
					return 1;
				else
					return -1;
			
			} else {
				
				System.out.println("");
				System.out.println("Generate CRUD templates and code for ALL (!) entities (y/n) ?): ");
				System.out.println(Colors.yellow("NOTE") + ": This will overwrite existing generated sources (HTML, java & columns.cfg)!");
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
    	fertilizer = new Fertilizer(singleEntity, "gen/java/DeleteHandler.java", "src/", "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/Entity.java", "src/", "java");
    	this.process(fertilizer);
    	
    	
		// ---- Router
    	
		System.out.println("");
		System.out.println(Colors.yellow("  Add the following lines to your beetRoot Router:\n"));
		System.out.println(
				  "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/index\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/view\", "+fertilizer.upperEntityPlural+"ViewHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/edit\", "+fertilizer.upperEntityPlural+"EditHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/add\", "+fertilizer.upperEntityPlural+"AddHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/delete\", "+fertilizer.upperEntityPlural+"DeleteHandler.class, \""+fertilizer.lowerEntityPlural+"\")\n"
				+ "");
    	
    }
    
	private void process(Fertilizer fertilizer) throws Exception {

		fertilizer.write(fertilizer.parse());
	}

	public final int run(String[] args) {
		
		CommandLine line = null;

		System.out.println(getBanner());
		this.printLine();
		System.out.println(getDescription());
		this.printLine();

		// No arguments at this time
		try {
			line = new DefaultParser().parse(makeOptions(), args);
		} catch (ParseException exp) {
			System.err.println(Colors.red("Couldn't read program argument.") + " Reason: " + exp.getMessage());
			usage();
			Utils.invalidArgumentsExit();
		}

		try {
			
			this.initialize(line);

			do { // loop for more entities
			
				int a = this.readParameters(false);
				if (a == 1) {
					
					System.out.println("");
					
					this.execute();
					
					System.out.println(Colors.green("Entity '"+singleEntity+"' processed!"));
					System.out.println("");
					
				} else if (a == 10) {
					
					System.out.println("");
					for (int i = 0; i < tableNames.length; i++) {
						singleEntity = tableNames[i];
						this.execute();
						System.out.println(Colors.green("Entity '"+singleEntity+"' processed!"));
					}
					System.out.println("");
				}
				else {
					// finish!
				}
				
			} while (this.readParameters(true) == 1);

		} catch (IllegalArgumentException e) {
			System.err.println(Colors.red("Uhh, something went wrong"));
			e.printStackTrace();
			//usage();
			return -1;
		} catch (Exception e) {
			System.err.println(Colors.red("Uhh, something went wrong"));
			e.printStackTrace();
			return -1;
		}

		System.out.println("");
		this.printLine();
		System.out.println("");
		System.out.println(Colors.yellow("NOTE")+":");
		System.out.println("- Move generated code to own packages and HTML to the desired (language)");
		System.out.println("  directories.");
		System.out.println("- New generation has overwriten possible previous generated sources!");
		System.out.println("");
		System.out.println(Colors.yellow("TODO's")+":");
		System.out.println("- Add the routes above to your router!");
		System.out.println("- Adjust mandatory fields in java add handler: only the mandatory fields need a");
		System.out.println("  default value in the add handler that are not present in the GUI!");
		System.out.println("- Remove unwanted GUI fields from 'columns.cfg' for the views 'view', 'add'");
		System.out.println("  and 'edit'.");
		System.out.println("- Also Remove unwanted <col span..> tags in the 'index.html'; e.g. if you");
		System.out.println("  removed standard fields 'id', 'created' and 'modified' from 'columns.cfg'.");
		System.out.println("- Add entity to menu or admin menu and overwrite 'hasAccess' method for every");
		System.out.println("  handler if necessary.");
		// overwrite method access
		System.out.println("");
		System.out.println("Done.");
		System.out.println("");

		return 0;
	}

	public static void main(String[] args) throws Exception {

		if (args.length == 1)
			BeetRootConfigurationManager.getInstance().initialize(args[0].trim());
		else
			BeetRootConfigurationManager.getInstance().initialize();
		
		final Plant generator = new Plant();
		final int exit = generator.run(args);
		System.exit(exit);
	}
	
}
