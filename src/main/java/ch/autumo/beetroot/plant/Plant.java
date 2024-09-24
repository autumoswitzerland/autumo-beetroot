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
import java.io.File;
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

import com.diogonunes.jcolor.Attribute;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.common.Colors;
import ch.autumo.beetroot.utils.systen.OS;

/**
 * PLANT - beetRoot CRUD Generator.
 */
public class Plant {

	protected static final Logger LOG = LoggerFactory.getLogger(Plant.class.getName());

	public static final String RELEASE = Constants.APP_VERSION;

	private static final String CR = OS.LINE_SEPARATOR;
	
	private String tableNames[] = null;
	private String singleEntity = null;
	
	private File webDir = null;

	
	public Plant() {
	}

	private String getBanner() {
		final String banner = CR + CR + 
				" __________.____       _____    __________________" + CR +
				" \\______   \\    |     /  _  \\   \\      \\__    ___/" + CR +
				"  |     ___/    |    /  /_\\  \\  /   |   \\|    |" + CR +
				"  |    |   |    |___/    |    \\/    |    \\    |" + CR + 
				"  |____|   |_______ \\____|__  /\\____|__  /____|" + CR +  
				"                   \\/       \\/         \\/";
		return Helper.createBanner(banner, Attribute.BRIGHT_GREEN_TEXT());
	}
	
	private String getDescription() {
		final String all = 
				Colors.darkCyan(" PLANT "+RELEASE) + " - BeetRoot Generator for creating operable CRUD views" + CR
				+ " based on database entities." + CR
				+ " (c) 2024 autumo Ltd. Switzerland";
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
			Helper.invalidArgumentsExit();
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
				System.out.println(Colors.darkYellow("Do you want to do more tasks? (enter=y)") + ": ");
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
			
				String statement = null;
				
				if (BeetRootDatabaseManager.getInstance().isMysqlDb() || BeetRootDatabaseManager.getInstance().isMariaDb() || BeetRootDatabaseManager.getInstance().isH2Db())
					statement = "SHOW TABLES";
				else if (BeetRootDatabaseManager.getInstance().isPostgreDb() || BeetRootDatabaseManager.getInstance().isPostgreDbWithNGDriver())  
					statement = "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema'";
				else if (BeetRootDatabaseManager.getInstance().isOracleDb())
					statement = "SELECT table_name FROM user_tables ORDER BY table_name";
				else
					statement = "SHOW TABLES";
				
				final ResultSet rs = stmt.executeQuery(statement);
				while (rs.next())
					tableList.add(rs.getString(1));
				rs.close();
					
			} finally {
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			
			int size = tableList.size();
			tableNames = tableList.toArray(new String[size]);

			int d = -1;

			// Show entity names
			do {
				System.out.println("");
				System.out.println(Colors.darkYellow("Input entity name") + ": ");
				for (int j = 0; j < tableNames.length; j++) {
					System.out.println("  ["+(j+1)+"] = "+tableNames[j]);
				} 
				System.out.println("  [all] = All tables !");
				System.out.println(Colors.darkYellow("Other functions") + ": ");
				System.out.println("  [t] = Translate templates");
				System.out.println("  [x] = Exit");
				System.out.print(">");

				val = br.readLine();
				if (val.trim().equalsIgnoreCase("all")) {
					d = 1;
					val = "all";
				} else if (val.trim().equalsIgnoreCase("t")) {
					val = "t";
				} else if (val.trim().equalsIgnoreCase("x")) {
					System.out.println("Bye.");
					System.out.println("");
					Helper.normalExit();
				} else {
					d = Integer.valueOf(val).intValue();
					val = "one";
				}

			} while (!val.equals("all") && !val.equals("t") && (d < 1 || d > size));

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

			if (val.equals("all")) {
				
				System.out.println("");
				System.out.println(Colors.darkYellow("NOTE") + ": This will overwrite existing generated sources (HTML, java & columns.cfg)!");
				System.out.print("Generate CRUD templates and code for ALL (!) entities (y/n) ?): ");
				
				String answer = br.readLine().trim();
				if (answer != null && answer.trim().equalsIgnoreCase("y"))
					return 10;
				else
					return -1;

			} else if (val.equals("t")) {
				
				System.out.println("");
				System.out.println(Colors.darkYellow("NOTE") + ": Translate the HTML templates (add, edit, view and index) as well as the column titles in the 'columns.cfg'");
				System.out.println("files. This should only be done once for a specific folder; all subfolders will also be processed recursively.");
				System.out.println("If you do not specify a folder, it will start with the 'web/' folder. Translations are always written or added");
				System.out.println("to the translation file 'web/lang/tmpl_lang_default.properties'. Copy this file for other language translations");
				System.out.println("e.g. to 'web/lang/tmpl_lang_en.properties'.");
				System.out.println("");
				boolean valid = false;
				String answer = null;
				while (!valid) {
					System.out.print("Specify the folder to be started (empty = 'web/'): ");
					answer = br.readLine().trim();
					if (answer.length() == 0) {
						answer = "web/";
					}
					webDir = new File(answer);
					if (!webDir.isDirectory()) {
						System.out.println(Colors.red("Folder is invalid. Enter a valid path."));
					} else {
						System.out.println(Colors.cyan("Folder") + ": " + webDir.getAbsolutePath());
						valid = true;
					}
				}
				System.out.println("");
				System.out.println("Continue (y/n, enter = y) ?): ");
				answer = br.readLine();
				if (answer != null && (answer.trim().equalsIgnoreCase("y") || answer.equals("")))
					return 20;
				else
					return -1;				
				
			} else {
				
				singleEntity = tableNames[d-1];
				System.out.println("");
				System.out.println(Colors.darkYellow("NOTE") + ": This will overwrite existing generated sources (HTML, java & columns.cfg)!");
				System.out.print("Generate CRUD templates and code for entity '" + singleEntity + "' (y/n, enter = y) ?): ");
	
				String answer = br.readLine();
				if (answer != null && (answer.trim().equalsIgnoreCase("y") || answer.equals("")))
					return 1;
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
    	
    	String src = "src/";
    	String srcMainJava = "src/main/java/";
    	final File srcMainJavaDir = new File(srcMainJava);
    	if (srcMainJavaDir.exists() && srcMainJavaDir.isDirectory())
    		src = srcMainJava;
    	
    	Fertilizer fertilizer = null;
    	fertilizer = new Fertilizer(singleEntity, "gen/java/IndexHandler.java", src, "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/ViewHandler.java", src, "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/EditHandler.java", src, "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/AddHandler.java", src, "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/DeleteHandler.java", src, "java");
    	this.process(fertilizer);
    	fertilizer = new Fertilizer(singleEntity, "gen/java/Entity.java", src, "java");
    	this.process(fertilizer);
    	
    	
		// ---- Router
    	
		System.out.println("");
		System.out.println(Colors.darkYellow("  Add the following lines to your beetRoot routing configuration 'routing.xml'"));
		System.out.println(Colors.darkYellow("  and into the right 'package'-section (change package name if necessary):\n"));

		System.out.println("    <!-- "+fertilizer.upperEntityPlural+" -->");
		System.out.println("    <Package name=\"planted.beetroot.handler."+fertilizer.lowerEntityPlural+"\">");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"\" handler=\""+fertilizer.upperEntityPlural+"IndexHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"/index\" handler=\""+fertilizer.upperEntityPlural+"IndexHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"/view\" handler=\""+fertilizer.upperEntityPlural+"ViewHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"/edit\" handler=\""+fertilizer.upperEntityPlural+"EditHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"/add\" handler=\""+fertilizer.upperEntityPlural+"AddHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("        <Route path=\"/:lang/"+fertilizer.lowerEntityPlural+"/delete\" handler=\""+fertilizer.upperEntityPlural+"DeleteHandler\" name=\""+fertilizer.lowerEntityPlural+"\" />");
		System.out.println("    </Package>");
		System.out.println("");
		
		/* Old:
		System.out.println(Colors.darkYellow("  Add the following lines to your beetRoot Router:\n"));
		System.out.println(
				  "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/index\", "+fertilizer.upperEntityPlural+"IndexHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/view\", "+fertilizer.upperEntityPlural+"ViewHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/edit\", "+fertilizer.upperEntityPlural+"EditHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/add\", "+fertilizer.upperEntityPlural+"AddHandler.class, \""+fertilizer.lowerEntityPlural+"\"),\n"
				+ "    new Route(\"/:lang/"+fertilizer.lowerEntityPlural+"/delete\", "+fertilizer.upperEntityPlural+"DeleteHandler.class, \""+fertilizer.lowerEntityPlural+"\")\n"
				+ "");
    	*/
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
			Helper.invalidArgumentsExit();
		}

		int a = 0;
		try {
			
			this.initialize(line);

			do { // loop for more entities
			
				a = this.readParameters(false);
				if (a == 1) {
					
					System.out.println("");
					
					this.execute();
					
					System.out.println(Colors.green("Entity '"+singleEntity+"' processed."));
					System.out.println("");
					
				} else if (a == 10) {
					
					System.out.println("");
					for (int i = 0; i < tableNames.length; i++) {
						singleEntity = tableNames[i];
						this.execute();
						System.out.println(Colors.green("Entity '"+singleEntity+"' processed."));
					}
					System.out.println("");
					
				} else if (a == 20) {
					
					System.out.println("");
					
					final TemplateLanguageProcessor tlp = new TemplateLanguageProcessor();
					tlp.process(webDir.getAbsolutePath(), "web/");
					
					System.out.println("");
					
				} else {
					
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
		if (a != 20) {
			System.out.println("");
			System.out.println(Colors.darkYellow("NOTE")+":");
			System.out.println("- Move generated code to own packages and HTML to the desired (language)");
			System.out.println("  directories.");
			System.out.println("- New generation has overwriten possible previous generated sources!");
			System.out.println("");
			System.out.println(Colors.darkYellow("TODO's")+":");
			System.out.println("- Add the routes above to your configuration 'cfg/routing.xml'!");
			System.out.println("- Adjust mandatory fields in java add handler: only the mandatory fields need a");
			System.out.println("  default value in the add handler that are not present in the GUI!");
			System.out.println("- Remove unwanted GUI fields from 'columns.cfg' for the views 'view', 'add'");
			System.out.println("  and 'edit'.");
			System.out.println("- Also Remove unwanted <col span..> tags in the 'index.html'; e.g. if you");
			System.out.println("  removed standard fields 'id', 'created' and 'modified' from 'columns.cfg'.");
			System.out.println("- Add entity to menu or admin menu and overwrite 'hasAccess' method for every");
			System.out.println("  handler if necessary.");
		} else {
			
		}
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
