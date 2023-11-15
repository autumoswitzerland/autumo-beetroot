<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/autumoswitzerland/autumo.svg?style=for-the-badge
[contributors-url]: https://github.com/autumoswitzerland/autumo/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/autumoswitzerland/autumo.svg?style=for-the-badge
[forks-url]: https://github.com/autumoswitzerland/autumo/network/members
[stars-shield]: https://img.shields.io/github/stars/autumoswitzerland/autumo.svg?style=for-the-badge
[stars-url]: https://github.com/autumoswitzerland/autumo/stargazers
[issues-shield]: https://img.shields.io/github/issues/autumoswitzerland/autumo.svg?style=for-the-badge
[issues-url]: https://github.com/autumoswitzerland/autumo/issues
[license-shield]: https://img.shields.io/badge/License-BSD_3--Clause-blue.svg?style=for-the-badge
[license-url]: https://github.com/autumoswitzerland/autumo/blob/master/autumo-beetroot/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/company/autumo
[product-screenshot]: https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-screen.png

<div id="top"></div>



<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<!-- PROJECT LOGO -->
<br>
<div align="center">
  <a href="https://github.com/autumoswitzerland/autumo/tree/master/autumo-beetroot">
    <img src="https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/beetroot.png" alt="Logo" width="200" height="200">
  </a>

<h1 align="center">autumo beetRoot</h1>

  <p align="center">
    A slim & rapid Java web-dev framework
    <br>
    <a href="https://github.com/autumoswitzerland/autumo/issues">Report Bug</a>
    ·
    <a href="https://github.com/autumoswitzerland/autumo/issues">Request Feature</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->

<br>
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#toc_0">What is beetRoot ?</a>
      <ul>
        <li><a href="#toc_1">Built With</a></li>
      </ul>
    </li>
    <li><a href="#toc_2">Distributions</a></li>
    <li><a href="#toc_3">Running Modes</a></li>
    <li><a href="#toc_4">Running</a></li>
    <li><a href="#toc_5">Configuration and Passwords</a></li>
    <li><a href="#toc_6">Default Database and Schema</a></li>
    <li><a href="#toc_7">CRUD Generator PLANT</a></li>
    <li><a href="#toc_8">Standard HTML Templates</a></li>
    <li><a href="#toc_9">JSON REST API</a></li>
    <li><a href="#toc_10">Routing</a></li>
    <li><a href="#toc_11">Logging</a></li>
    <li><a href="#toc_12">Mailing</a></li>
    <li><a href="#toc_13">Mail Templates</a></li>
    <li><a href="#toc_14">Java Translations</a></li>
    <li><a href="#toc_15">Webapp Design and Javascript</a></li>
    <li><a href="#toc_16">HTTPS</a></li>
    <li><a href="#toc_17">Roadmap | Backlog</a></li>
    <li><a href="#toc_18">License</a></li>
    <li><a href="#toc_19">Contact</a></li>
    <li><a href="#toc_20">Acknowledgments</a></li>
  </ol>
</details>
<br>



<!-- WAHT IS BEETROOT -->
## What is beetRoot ?

[![autumo beetRoot 2.0.0 - Quickstart](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-screen.png)](https://www.youtube.com/watch?v=ruZrP-7yCDY)

<div style="text-align: center;"><strong><a href="https://www.youtube.com/watch?v=ruZrP-7yCDY">autumo beetRoot 2.0.0 - Quickstart Video</a></strong></div>

beetRoot is a rapid Java web-development as well as a full & secure client-server framework ready to run! If you know [CakePHP](https://cakePHP.org) for web development, you'll like beetRoot. It is based on the same principles and comes with a full CRUD generator generating all views, the model specification and controllers (handlers in beetRoot's terminology) based on the database model! The client-server framework supports encrypted communication (SSL) as well as HTTP/HTTPS-tunneling, provides a file download and upload interface and it can be extended with own (distributed) modules.

Note that the client-server framework is not documented in this document; it focuses only on the web development part.

The Web framework is shipped with the following features ready to use:

- Add, edit, view, list and delete functionality for entities
- Bean support with transient and unique fields
- Language management
- Exchangeable logging
- File up- and download
- Full MIME types control
- 2-Factor-Authentication
- Password reset mechanism
- Extendable user settings
- Dark theme and theme support
- Interface for SMS notifications
- Mailing inclusive mail templates
- URL routing with language support
- File caching (resources and templates)
- Easy to understand HTML template engine
- Argon2/PBKPD2 password encryption
- HTTPS protocol and TLS for mail if configured
- User roles & access control on controller level
- User session are stored when servers are stopped
- Entities can be served through the JSON REST API
- Logging implementations other than log4j2 supported
- Servlet API 4.0 Java EE 8 (prepared for 5.0 Jakarta EE 8)
- Full CRUD-Generator **PLANT** for views, models and handlers
- One-to-many database relationships are fully applied in MVC layers 
- Tested on Apache Tomcat 9, Eclipse Jetty 10 and Oracle Weblogic 14
- Standard CSRF mechanism as well as obfuscated CRUD IDs within HTTP requests
- Database connection pooling (HikariCP, with internal and external JNDI data sources)
- Runs stand-alone as well as in common servlet containers such as Apache Tomcat and Jetty on URL root path as well behind a servlet-path without modifications of HTML templates, etc.
- Secure client-server communication, if beetRoot is installed in a servlet container apart from beetRoot server and if there's need for such communication to steer backend processes
- Hierarchical resource loader; e.g. German language requested, if not found, use configured default language, then use no language at all; "lookup till you find something usable" is the 
  algorithm for everything. As well, load resources from file system (first), then as a resource within packages (jar, war) if not found beforehand.
- And some more stuff... 

Enjoy!

<p align="right">(<a href="#top">back to top</a>)</p>



### Built With

* [NanoHTTPD](http://nanohttpd.org)
* [Apache commons](https://commons.apache.org)
* [SLF4j](https://www.slf4j.org)
* [Log4j2](https://logging.apache.org/log4j/2.x)
* [Checker Framework Qualifiers](https://checkerframework.org)
* [Jakarta Mail API](https://eclipse-ee4j.github.io/mail)
* [Google ZXing Java SE Extensions](https://github.com/zxing)
* [JQuery](https://jquery.com)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [normalize.css](https://necolas.github.io/normalize.css)
* ...and some more; see [THIRDPARTYLICENSES.html](https://htmlpreview.github.io/?https://github.com/autumoswitzerland/autumo-beetroot/blob/master/THIRDPARTYLICENSES.html)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- DISTRIBUTIONS -->
## Distributions

1. **`autumo-beetRoot-x.y.z.zip`**: Stand-alone server version with all files for every distribution.
2. **`autumo-beetRoot-web-x.y.z.zip`**: General web-app version.
3. **`beetroot.war`**: Tomcat version.
4. **`beetroot-jetty.war`**: Jetty version (for demo purposes only). 

Distributions are available here: [Releases](https://github.com/autumoswitzerland/autumo/releases) - they can be generated with the make shell-script `make.sh` too.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- RUNNING MODES STARTED -->
## Running Modes

This is an example of how you may give instructions on setting up your project locally. To get a local copy up and running follow these simple example steps.

beetRoot can be run within two modes:

1. Stand-alone web-server, based on a partially patched version of NanoHTTP and [RouterNanoHTTPD](https://github.com/NanoHttpd/nanohttpd/tree/master/nanolets/src/main/java/org/nanohttpd/router); the specific versions are:
     
	- NanoHTTP a.b.c-SNAPHOT, which is distributed by autumo GmbH through the library
       'nanohttpd-a.b.c-BEETROOT.jar'
       
	- RouterNanoHTTPD a.b.c-SNAPHOT, patched within `autumo-beetroot-x.y.z.jar`
      
2. Within a servlet container such as tomcat or jetty.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- RUNNING -->
## Running

All batch files and shell scripts are located in the `bin` directory.

1. Stand-alone usage:

	- `beetRoot.sh start` / `beetRoot.bat start` 

		See shell-script if you need special modifications. The stand-alone beetRoot web-server reads the configuration `cfg/beetroot.cfg`. Most configuration parameters are used for servlet-container  operation too. All Configuration parameters are explained in the configuration file itself.

	- Surf to http://localhost:8778 (8778 is the default port for the stand-alone server).

	- The stand-alone server with its resources is packed into `autumo-beetRoot-x.y.z.zip`.

	- Use `beetRoot.sh stop` / `beetRoot.bat stop` to properly stop the stand-alone server.
	
	- **NOTE**: We don't distribute the Java Servlet API. If you run beetRoot as a stand-alone server, you have to download this API yourself e.g. through maven-dependencies or here: [Java Servlet Specification](https://javaee.github.io/servlet-spec). You basically need to add the library javax.servlet-api-x.y.z.jar. The same applies to jakarta's mail (jakarta.mail-x.y.z.jar) and activation (jakarta.activation-x.y.z.jar) implementation for the mailing component. If you want to uses Oracle's mail implementation (javax.mail-x.y.z.jar), you have to get that library by your own too. Always place additional libraries in the `lib/` folder.  

2. Servlet-container:

	Throw `beetroot.war` into tomcat `webapps/` and throw `beetroot-jetty.war` into jetty's `webapps/` folder. Fire it up and surf to http://localhost:8080/beetroot. Configure your containers as you wish. We recommend running the beetroot webapp extracted, so any change on the HTML templates and the model configuration (`columns.cfg` for each entity) can be made on-the-fly.

If you want to keep everything under control use the archive `autumo-beetRoot-web-x.y.z.zip`.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONFIGURATION AND PASSWORDS -->
## Configuration and Passwords

Have a look at `cfg/beetroot.cfg`. Every configuration parameter is explained. You can run beetRoot with ALL passwords encoded if you wish. You can define, if passwords used in the configuration file should be encoded. The same for passwords stored in the beetRoot-database-table `users`.

There are two configuration variables for this: `admin_pw_encoded` & `db_pw_encoded` (yes/no).
 
For security reasons, you should change the secret key seed (`secret_key_seed`) in the beginning and then generate new passwords with the tool `pwencoder.sh/pwencoder.bat`. If you do, you have to change the initial encoded password for the beetRoot `admin` user in the database to gain access again!

**NOTE**: All passwords are **`beetroot`** in the beginning!


Furthermore, the configuration offers wide possibilities of customization for your app, such as:

- Roles
- Buffer sizes
- Server ports
- SSL keystore
- Session storage
- Protocol (HTTP/HTTPS)
- Web application languages
- Password encryption (see above)
- Auto-update of modification time-stamps
- DB access and DB type (connected through JDBC)
- Supported databases: MySQL, MariaDB, Java H2, Oracle, PostgreSQL
- Default web view (in case of certain redirects)
- Mail configuration inclusive TLS; some configuration parameters can be overwritten by values in the standard DB table `properties`
- ...and much more.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- DEFAULT DATABASE AND SCHEMA -->
## Default Database and Schema

Every beetRoot package (stand-alone & web-apps) come with a [H2 database](https://h2database.com) filled with sample data and the configuration points to this database. If you want to connect to your own database, simply change the connections parameters in the configuration.

To setup a new database scheme use the SQL-Script `db/install_beetroot.sql` and customize it to your needs and database. 

A word when using MySQL: Due to the GPL license, we don't distribute or create a dependency to the MySQL Connector for Java. Visit Oracle MySQL website and download it yourself if you want to use this connector. Note that the MariaAB connector for Java works also for MySQL databases up to the version 5.5 of MySQL or even for higher versions! Also have a look here for further valuable information in this context: [MariaDB License FAQ](https://mariadb.com/kb/en/licensing-faq).

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CRUD GENERATOR PLANT -->
## CRUD Generator *PLANT*

Start the CRUD generator with the script `plant.sh` / `plant.bat` and follow the steps!

The generator reads the entities in the database that is configured in `cfg/beetroot.cfg`, from which you can choose one or all! If you need another configuration file (e.g. with another database conenction), you can specify that configuration file as an argument in the shell / batch scripts above. PLANT supports the MySsql/MariaDB database at the moment; use such a DB during the app development and change to your desired target DB for production.

**NOTE**: Entities for beetRoot MUST be named in plural form in the database, e.g.: tasks, users, cities, properties, cars, etc.

In the Generated HTMLs the following standard transformation takes place:

- tasks -> task (singular)
- properties -> property (singular)

Hence, we suggest you use English names for your database entities, even you can adjust the named versions in the generated HTMLs of course and which is even necessary if you copy the templates for other languages.


It generates the following sources:

HTML templates & model configuration (`columns.cfg`):

- web/html/`{entity-plural-name}`/add.html
- web/html/`{entity-plural-name}`/edit.html
- web/html/`{entity-plural-name}`/view.html
- web/html/`{entity-plural-name}`/index.html
- web/html/`{entity-plural-name}`/columns.cfg

Java sources (handlers):

- src/planted/beetroot/handler/`{entity-plural-name}`/`{Entity-plural-name}`AddHandler.java
- src/planted/beetroot/handler/`{entity-plural-name}`/`{Entity-plural-name}`EditHandler.java
- src/planted/beetroot/handler/`{entity-plural-name}`/`{Entity-plural-name}`ViewHandler.java
- src/planted/beetroot/handler/`{entity-plural-name}`/`{Entity-plural-name}`IndexHandler.java
- src/planted/beetroot/handler/`{entity-plural-name}`/`{Entity-plural-name}`DeleteHandler.java

Adjust them to your needs (see existing handlers for examples) and more **IMPORTANT**: Move the Java sources to another package! All generated files are overwritten the next time if you re-generate sources for the same entity!

The HTML templates & model configuration are usually moved/copied to a language sub-directory:

- web/html/`{entity-plural-name}`/en/*.html|columns.cfg
- web/html/`{entity-plural-name}`/de/*.html|columns.cfg
- web/html/`{entity-plural-name}`/fr/*.html|columns.cfg
- etc.

We suggest to backup your original generated HTML templates and model configuration, they serve as a fallback scenario when the user request a language that is not present!


The model configuration `columns.cfg` does the following for every entity:

- It defines what columns you see for every view (add, edit, view/single-record, index/list) and defines a value for the field name. See existing files for sample entities `Task` and/or `User`. E.g.:

	index.html: `list.aDbfield=GUI Name for that Field`
	
	view.html: `view.aDbfield=GUI Name for that Field`
	
	edit.html: `edit.aDbfield=GUI Name for that Field`
	
	add.html: `add.aDbfield=GUI Name for that Field`
	
- It also defines which columns are UNIQUE in the database by defining them with the key `unique`, e.g.:

	`unique=name, path`
	
- If you want to load a value from a database field into the entity bean to use it in a handler, but you do not want it to be displayed in the GUI, define the constant 'NO_SHOW' as the GUI field name, for example:

	index.html: `list.secretDbField=NO_SHOW`
	
	view.html: `view.secretDbField=NO_SHOW`
	
	edit.html: `edit.secretDbField=NO_SHOW`
	
	add.html: `add.secretDbField=NO_SHOW`

- Furthermore, you can manually define transient values that are not read from or stored to database nor they are loaded within a bean, they are just delievered within the handler methods, so another value can be served for these transient columns/fields, e.g.:

	`transient=status`

- Last, you can manually specify default values with the `init`-prefix that should be shown in the `add.html`-template when a new record is created, e.g:

	`init.threshold=3`<br>
	`init.counter=0`

Your TODO's are the following after generating:

- Add mandatory (DB: not nullable) fields in the `add`-handler: only the mandatory fields need a default value in the add handler that are not present in the GUI! See all `Properties` handlers for more information.

- Remove unwanted GUI fields from `columns.cfg` for the views `view`, `add` and `edit`.
		
- Also Remove unwanted `<col span..>` tags in the `index.html`; e.g. if you removed standard fields `id`, `created` and `modified` from `columns.cfg`.

- Add entity to menu or admin menu and overwrite `hasAccess`-method for every handler if necessary.


**NOTE**: PLANT uses its own templates stored within the `gen/`-directory. Of course, you can even adjust these templates next to the block-templates, so they have the structure you want to have for your own web-app.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- STANDARD HTML TEMPLATES -->
## Standard HTML Templates

The following standard HTML templates are present, which can be customized too of course:

- `web/html/blocks/*.html`:
 
	- Defines the layout of the page with its general elements such as head, header, menu admin menu, language menu, message block and script section (javascript).
  
	- They can be copied to other language directories too (e.g. `web/html/blocks/en/*.html`), if they need to be language specific, which in most cases, is not necessary. They also serve as fallback templates, if the user requested a language that isn't found respectively the web-app is not yet translated into that language.
  
	- **NOTE**: Here, as well as with the generated HTML templates, the lookup algorithm is:
		a) First, lookup templates in requested language directory (2-letter ISO code)
		b) If not found, try the default language; this is the one that is first defined in the configuration, see parameter `web_languages`.
		c) If still not found, use the templates by omitting the language code, respectively the language directory in the `web/html`-directory structure.
	 
E.g.: the Englisch `index.html` for the entity `tasks` is here: `web/html/en/tasks/index.html`. If this resource is not available, then `web/html/tasks/index.html` is looked up. The same applies for `colums.cfg` resources.
	  
**NOTE**: Valid for templates and any other HTMLs file that are added to the `web/html`-directory structure:

- The relative URL (without Host, Port and Servlet name) requested by the web-app user is translated not 1-to-1 by the directory structure, but through **Routing** chapter!

The following template variables are always parsed and you can use them as many times as you want:

- `{$lang}` : User's language, 2-ISO-code
- `{$user}` : User login name
- `{$userfull}` : Full user name (first and last name if available, otherwise login name)  
- `{$title}` : Page title (within add, edit, view and list)
- `{$id}` : Obfuscated object id (within add, edit and view)
- `{$dbid}` : Real database id (within add, edit and view)
- `{$csrfToken}` : CSRF token

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- JSON REST API -->
## JSON REST API

beetRoot comes with an "out-of-the-box" JSON REST API that serves any entity from the application. The API uses an API key that is defined within the "Settings" by the key `web.json.api.key`. The API key name itself can be changed in the beetRoot configuration `cfg/beetroot.cfg`.

A REST API call looks like this:

- `http://localhost:8778/tasks/index.json?apiKey=c56950c47cc9f055a17395d6bf94222d&page=1&fetchsize=2&sort=id&direction=desc&page=1`

Example Answer:

	{
	    "tasks": [
	        {
	            "id": "5",
	            "name": "Task 5",
	            "active": "false",
	            "laststatus": "false"
	        },
	        {
	            "id": "4",
	            "name": "Task 4",
	            "active": "false",
	            "laststatus": "true"
	        }   
	    ],
	    "paginator": {
	        "itemsPerPage": 2,
	        "itemsTotal": 2,
	        "lastPage": 1,
	    }    
	}

As you can see, you can iterate with the `paginator` object through pages with your REST calls - the same way as you would navigate within an HTML `index` page.

JSON templates can be handled like HTML templates: Put them into the directory `web/html/..`. No user languages are used in any way by using this API. Therefore, you can dismiss the HTML template language directories and place the template, e.g. for entity `tasks`, directly here `web/html/tasks/index.json`; it looks like this:

	{
    	"tasks": [
			{$data}    
    	],
			{$paginator}    
	}

Also, you can create an own `columns.cfg` that is specific for the JSON request in this directory, for example looking like this:

	list_json.id=is
	list_json.name=name
	list_json.active=active
	list_json.laststatus=laststatus

It never has been easier using a REST API!

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ROUTING -->
## Routing

The router defines which resources are served by the requested URL of a web-app user. The out-of-box router is the `BeetRootDefaultRouter.java`. In any case, it always should be replaced for your own app: Define your router's java class in the `web_router` parameter. You simply have to implement the `Router` interface.

Let's have a look at some routes:

	/** Home stuff */
	new Route("/:lang/home", HomeHandler.class, "home"),
	new Route("/:lang/home/index", HomeHandler.class, "home"),

	/** Files */
	new Route("/:lang/files/view", ExampleDownloadHandler.class, "files"),
	new Route("/:lang/files/add", ExampleUploadHandler.class, "files"),
	
	/** Tasks */
	new Route("/:lang/tasks", TasksIndexHandler.class, "tasks"),
	new Route("/:lang/tasks/index.json", TasksRESTIndexHandler.class, "tasks"),
	new Route("/:lang/tasks/index", TasksIndexHandler.class, "tasks"),
	new Route("/:lang/tasks/view", TasksViexwHandler.class, "tasks"),
	new Route("/:lang/tasks/edit", TasksEditHandler.class, "tasks"),
	new Route("/:lang/tasks/add", TasksAddHandler.class, "tasks"),
	new Route("/:lang/tasks/delete", TasksDeleteHandler.class, "tasks")


The requested URL's are translated to generated (or self-created) handlers which always must implement the method:

	public  String getResource();

For generated handlers (with the CRUD generator) this is usually not necessary, because they have a standard implementation:

	@Override
	public  String getResource() {
	    return "web/html/:lang/"+entity+"/index.html";
	}

If your handlers need customization, just overwrite this method. As you can see, here the translation of the requested URL takes place and points to the `web/html`-directory structure!

The language is replaced by the requested language, and if not found the above algorithm is executed. The `entity` name is assigned through the construction of the handler (see route examples above). That's it, more or less! 


**NOTE**: You never have to reference the servlet-name within any request URL not in the router, not even in your HTML templates when running beetRoot in a servlet container. Just specify the configuration parameter `web_html_ref_pre_url_part`.

For example ('beetroot' is the default servlet name/url):

  - http://localhost:8080/beetroot/en/tasks/add -> routes to `web/html/en/tasks/add.html` as specified by the corresponding handler. 

beetRoot handles every pre-url-path / servlet-name-path by its own, if configured correctly!


**IMPORTANT**: If you run beetRoot as a stand-alone server or in a servlet container, where beetRoot is installed on the ROOT path, NEVER specify the parameter `web_html_ref_pre_url_part`! When you define a HTML a-tag (e.g. `<a href="/{$lang}/tasks/index"....>`) or an image source or any other source (e.g. `<img src="/img/beetroot-100.png">`), you always point to the root-path "/". Tough, you have to include the language placeholder `:lang` for HTML templates always.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- LOGGING -->
## Logging

beetRoot uses [SLF4j](https://slf4j.org). For the stand-alone and tomcat wep-app version, the log4j2 implementation (the one that has NOT the log4j2 bug in it...!) is used and the default configuration `cfg/logging.xml` (stand-alone) and `logging.xml` (in tomcat web-app servlet directory) is used. If you want to specify your own, adjust it this way:

- stand-alone: Define a runtime parameter in the shell/bash script when starting Java:

	`-Dlog4j2.configurationFile=file:<log-cfg-path>/myLogConfig.xml`

- tomcat web-app: Define your log file in the 'WEB-INF/web.xml', within the parameter:

	`beetRootLogConfig`

As for jetty, they stand above all that "log-framework-soup" and they just simply use a SLF4j implementation that needs no further configuration. Hence, the library `slf4j.simple-x.y.z.jar` is packed into `beetroot-jetty.war`. The only concern is to add your package to the the jetty basic logging configuration in `{JETTY_BASE}/resources/jetty-logging.properties`:

	## Configure a level for specific logger
	ch.autumo.beetroot.LEVEL=INFO


**NOTE**: All logging levels are set to `INFO` in the beginning!

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- MAILING -->
## Mailing

Mailing supports Eclipse's Jakarta (`jakarta.mail`) as well as Oracle's JavaMail (`javax.mail`) implementation as originally defined by the [JavaMail project](https://javaee.github.io/javamail). By default, Jakarta is used. This possibly must be switched to JavaMail in certain environments that don't "interact" well within certain environments. E.g., WebLogic works only with Oracle's implementation. When using JavaMail, also a mail session name must be specified in the beetRoot configuration.

Check the configuration `cfg/beetroot.cfg` for further mailing options. Some of them can be even overwitten by the application "Settings"; check the "Settings" page in the beetRoot Web application.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- MAIL TEMPLATES -->
## Mail Templates

Mail templates are here and follow the same lookup-patterns as for HTML templates:

HTML format:

  - web/html/en/email
  - web/html/de/email
  - web/html/email
  - etc.

Text format:

  - txt/html/en/email
  - txt/html/de/email
  - txt/html/email
  - etc.
  
beetRoot can send both HTML and text emails. Formats are configured with the parameter `mail_formats`. 


**NOTE**: Java mail doesn't allow sending HTML with a head nor body-tag, hence you only are able to define HTNML templates with tags that would be inside of a the body-tag. It is specification! 

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- JAVA TRANSLATIONS -->
## Java Translations

If you need translations within the Java code, you simply add language java resource bundle keys within the `web/lang` directory to the specific language resource bundle, e.g. `lang_en.properties`. You can change beetRoot's standard messages too. For every new language added respectively requested by the web-app user, add the corresponding language java resource bundle within this directory.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- WEBAPP DESIGN AND JAVASCRIPT -->
## Webapp Design and Javascript

All you need for this, is located here:

- web/css/*
- web/font/*
- web/img/*
- web/js/*

These resources are straightly routed by the user's URL request, e.g.:

  - `http://localhost:8080/img/myImage.png` -> `web/img/myImage.png`
  - `http://localhost:8080/js/myScript.js` -> `web/js/myScript.js`
  - etc.

Also, in this case, you never have to reference a servlet name in any HTML template, you always point to teh root-path "/", no matter what!

A few words about existing stylesheets:

  - `web/css/base.css`: Base styles, you don't want to change this in most cases.

  - `web/css/style.css`: Adjust your general web.app style here.

  - `web/css/refs.css`: Add here styles that reference images, fonts, etc. per url-references, e.g.: `url('/img/...');`. This is necessary, so beetRoot can translate resource URL's for a servlet context correctly.

  - `web/css/jquery-ui.min.css`: Better tooltips.

  - `web/css/default.css`: Your default web-app styles and designs.

  - `web/css/theme-dark.css`: The default dark theme; you can add your own themes by naming it `theme-yourname.css` and HTTP-request it through the users' settings handler.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- HTTPS -->
## HTTPS

If you run beetRoot as a stand-alone server and you want to run it with the HTTPS protocol, there's a prepared and self-signed keystore file: `ssl/beetroot.jks` that is valid forever.

If you configure beetRoot to run with the HTTPS protocol (configuration parameter `ws_https`), you can use this keystore file and it is specified by default in the configuration (`keystore`). Your browser will still complain, because it is not issued by a valid Certificate Authority (CA), but you can force the browser to still load the web-app by adding this exception. If you run beetRoot in productive mode, you have to acquire a valid certificate and store it this keystore or in a an own; Java supports the PKCS\#12 format and Java keystore can be opened with tools such as this one: https://keystore-explorer.org. The password for `ssl/beetroot.jks` is **`beetroot`**.  

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap | Backlog

- Low Prio.: [Add SQLLite DB Connectivity](https://www.sqlite.org)
- Low Prio.: [Update to normalize.css 8](https://necolas.github.io/normalize.css/)

See also the [open issues](https://github.com/autumoswitzerland/autumo/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- LICENSE -->
## License

Distributed under the BSD 3-Clause-License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

autumo Switzerland - [@autumo](https://twitter.com/autumo) - autumo.switzerland@gmail.com

Project Link: [https://github.com/autumoswitzerland/autumo-beetroot](https://github.com/autumoswitzerland/autumo-beetroot)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* Inspired by CakePHP (https://cakephp.org)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- DONATE -->
## Donate

Your donation helps to develop autumo beetRoot further. Thank you!

[![paypal](https://products.autumo.ch/img/DonateWithPayPal.png)](https://www.paypal.com/donate/?hosted_button_id=WWDWJG7Z4WJZC)

<br>
<br>
Copyright 2023, autumo Ltd., Switzerland


