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
[license-shield]: https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge
[license-url]: https://opensource.org/licenses/Apache-2.0
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
      <a href="#what-is-beetroot-">What is beetRoot ?</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li><a href="#quickstart">Quickstart</a></li>
    <li><a href="#distributions">Distributions</a></li>
    <li><a href="#running-modes">Running Modes</a></li>
    <li><a href="#running-1">Running</a></li>
    <li><a href="#configuration-and-passwords">Configuration and Passwords</a></li>
    <li><a href="#default-database-and-schema">Default Database and Schema</a></li>
    <li><a href="#crud-generator-plant">CRUD Generator PLANT</a></li>
    <li><a href="#crud-hooks">CRUD Hooks</a></li>
    <li><a href="#standard-html-templates">Standard HTML Templates</a></li>
    <li><a href="#json-rest-api">JSON REST API</a></li>
    <li><a href="#routing">Routing</a></li>
    <li><a href="#logging">Logging</a></li>
    <li><a href="#mailing">Mailing</a></li>
    <li><a href="#mail-templates">Mail Templates</a></li>
    <li><a href="#java-translations">Java Translations</a></li>
    <li><a href="#webapp-design-and-javascript">Webapp Design and Javascript</a></li>
    <li><a href="#https">HTTPS</a></li>
    <li><a href="#roadmap--backlog">Roadmap | Backlog</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>
<br>



<!-- WHAT IS BEETROOT -->
## What is beetRoot ?

[![autumo beetRoot 2.x - Quickstart](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-screen.png)](https://www.youtube.com/watch?v=ruZrP-7yCDY)

<p style="text-align: center;"><strong><a href="https://www.youtube.com/watch?v=ruZrP-7yCDY">autumo beetRoot 2.x - Quickstart Video</a></strong></p>

**Note**: The video shows the old routing which has changed in the version 2.3.0; it is done now with a configuration file (routing.xml); see chapter <a href="#routing">Routing</a>.

![beetRoot Console](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-console.png)

beetRoot is a rapid Java web-development as well as a full & secure client-server framework ready to run! If you know [CakePHP](https://cakePHP.org) for web development, you'll like beetRoot. 
It is based on the same principles and comes with a full CRUD generator generating all views, the model specification and controllers (handlers in beetRoot's terminology) based on the database 
model! The client-server framework supports encrypted communication (SSL) as well as HTTP/HTTPS-tunneling, provides a file download and upload interface and it can be extended with own (distributed) 
modules.

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
- SMS and phone call interfaces
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
* ...and some more; see [THIRDPARTYLICENSES.html](https://htmlpreview.github.io/?https://github.com/autumoswitzerland/autumo-beetroot/blob/master/THIRDPARTYLICENSES.html)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- QUICKSTART -->
## Quickstart

### Running

Enter the following statements into your terminal.

**Linux, macOS**

```NuShell
VERSION=2.3.1
PACKAGE=autumo-beetRoot-$VERSION

curl -LO https://github.com/autumoswitzerland/autumo-beetroot/releases/download/v$VERSION/$PACKAGE.zip

unzip $PACKAGE.zip
rm $PACKAGE.zip

# Optional libraries to send emails (needed for password reset)
(cd $PACKAGE/lib && curl -LO https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.1/jakarta.activation-2.0.1.jar)
(cd $PACKAGE/lib && curl -LO https://repo1.maven.org/maven2/com/sun/mail/jakarta.mail/2.0.1/jakarta.mail-2.0.1.jar)

$PACKAGE/bin/beetroot.sh start
```

**Windows**

```Batchfile
SET VERSION=2.3.1
SET PACKAGE=autumo-beetRoot-%VERSION%

curl -LO https://github.com/autumoswitzerland/autumo-beetroot/releases/download/v%VERSION%/%PACKAGE%.zip

tar -xf %PACKAGE%.zip
del %PACKAGE%.zip

REM Optional libraries to send emails (needed for password reset)
cd %PACKAGE%/lib && curl -LO https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.1/jakarta.activation-2.0.1.jar && cd ..\..
cd %PACKAGE%/lib && curl -LO https://repo1.maven.org/maven2/com/sun/mail/jakarta.mail/2.0.1/jakarta.mail-2.0.1.jar && cd ..\..

%PACKAGE%\bin\beetroot.bat start
```


### Developing

1. Clone the repository:

```NuShell
git clone https://github.com/autumoswitzerland/autumo-beetroot.git
```

2. Import the maven-project into your favourite development IDE.

3. Start developing/customizing: <a href="https://www.youtube.com/watch?v=ruZrP-7yCDY">autumo beetRoot 2.x - Quickstart Video</a>.

<p align="right">(<a href="#top">back to top</a>)</p> 



<!-- DISTRIBUTIONS -->
## Distributions

1. **`autumo-beetRoot-x.y.z.zip`**: Stand-alone server version with all files for every distribution.
2. **`autumo-beetRoot-web-x.y.z.zip`**: General web-app version.
3. **`beetroot.war`**: Tomcat version.
4. **`beetroot-weblogic.zip`**: Weblogic version (Stage deployment).
5. **`beetroot-jetty.war`**: Jetty version (for demo purposes only).

Distributions are available here: [Releases](https://github.com/autumoswitzerland/autumo/releases) - they can be generated with the make shell-script `make.sh` too.

<p align="right">(<a href="#top">back to top</a>)</p> 



<!-- RUNNING MODES STARTED -->
## Running Modes

beetRoot can be run in two modes:

1. As a stand-alone server that consists of:

	- An administration interface for server commands and for executing distributed dispatcher modules.

	- A web-server;
	
		- uses a [patched version](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java) 
		of the [RouterNanoHTTPD](https://github.com/NanoHttpd/nanohttpd/blob/master/nanolets/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java) and
		
		- an [updated version of the NanoHttpd project](https://github.com/autumoswitzerland/nanohttpd) through the provided library `nanohttpd-a.b.c-BEETROOT.jar`.

	- An optional file-server (storage / find & download).
      
2. Within a servlet-container such as Tomcat, WebLogic or Jetty that can optionally interact with the stand-alone server; see above.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- RUNNING -->
## Running

All batch files and shell scripts are located in the `bin` directory.

1. Stand-alone usage:

	- `beetRoot.sh start` / `beetRoot.bat start` 

		See shell-script if you need special modifications. The stand-alone beetRoot web-server reads the configuration `cfg/beetroot.cfg`. Most configuration parameters 
		are used for servlet-container  operation too. All Configuration parameters are explained in the configuration file itself.

	- Surf to http://localhost:8778 (8778 is the default port for the stand-alone server).

	- The stand-alone server with its resources is packed into `autumo-beetRoot-x.y.z.zip`.

	- Use `beetRoot.sh stop` / `beetRoot.bat stop` to properly stop the stand-alone server.

2. Servlet-container:

	Throw `beetroot.war` into tomcat `webapps/` and throw `beetroot-jetty.war` into jetty's `webapps/` folder. Fire it up and surf to http://localhost:8080/beetroot. Configure your 
	containers as you wish. We recommend running the beetroot webapp extracted, so any change on the HTML templates and the model configuration (`columns.cfg` for each entity) 
	can be made on-the-fly.

If you want to keep everything under control use the archive `autumo-beetRoot-web-x.y.z.zip`.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONFIGURATION AND PASSWORDS -->
## Configuration and Passwords

Have a look at `cfg/beetroot.cfg`. Every configuration parameter is explained. You can run beetRoot with ALL passwords encoded if you wish. You can define, if passwords used in the 
configuration file should be encoded. The same for passwords stored in the beetRoot-database-table `users`.

There are two configuration variables for this: `admin_pw_encoded` & `db_pw_encoded` (yes/no).
 
For security reasons, you should change the secret key seed (`secret_key_seed`) in the beginning and then generate new passwords with the tool `pwencoder.sh/pwencoder.bat`. If you 
do, you have to change the initial encoded password for the beetRoot `admin` user in the database to gain access again!

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

Every beetRoot package (stand-alone & web-apps) come with a [H2 database](https://h2database.com) filled with sample data and the configuration points to this database. If you 
want to connect to your own database, simply change the connections parameters in the configuration.

To setup a new database scheme use the SQL-Script `db/install_<database-type>.sql` and customize it to your needs (initial data). 

A word when using MySQL: Due to the GPL license, we don't distribute or create a dependency to the MySQL Connector for Java. Visit Oracle MySQL website and download it yourself 
if you want to use this connector. Note that the MariaAB connector for Java works also for MySQL databases up to the version 5.5 of MySQL or even for higher versions! Also have 
a look here for further valuable information in this context: [MariaDB License FAQ](https://mariadb.com/kb/en/licensing-faq).

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CRUD GENERATOR PLANT -->
## CRUD Generator *PLANT*

Start the CRUD generator with the script `plant.sh` / `plant.bat` and follow the steps!

The generator reads the entities in the database that is configured in `cfg/beetroot.cfg`, from which you can choose one or all! If you need another configuration file (e.g. with 
another database connection), you can specify that configuration file as an argument in the shell / batch scripts above. PLANT supports the MySQL, MariaDB, H2, Oracle and PostgreSQL 
databases.

**NOTE**: Entities for beetRoot MUST be named in plural form in the database, e.g.: tasks, users, cities, properties, cars, etc.

In the Generated HTMLs the following standard transformation takes place:

- tasks -> task (singular)
- properties -> property (singular)

Hence, we suggest you use English names for your database entities, even you can adjust the named versions in the generated HTMLs of course and which is even necessary if you copy 
the templates for other languages.


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

Adjust them to your needs (see existing handlers for examples) and more **IMPORTANT**: Move the Java sources to another package! All generated files are overwritten the next time 
if you re-generate sources for the same entity!

The HTML templates & model configuration are usually moved/copied to a language sub-directory:

- web/html/`{entity-plural-name}`/en/*.html|columns.cfg
- web/html/`{entity-plural-name}`/de/*.html|columns.cfg
- web/html/`{entity-plural-name}`/fr/*.html|columns.cfg
- etc.

We suggest to backup your original generated HTML templates and model configuration, they serve as a fallback scenario when the user request a language that is not present!


The model configuration `columns.cfg` does the following for every entity:

- It defines what columns you see for every view (add, edit, view/single-record, index/list) and defines a value for the field name. See existing files for sample entities `Task` 
and/or `User`. E.g.:

	index.html: `list.aDbfield=GUI Name for that Field`
	
	view.html: `view.aDbfield=GUI Name for that Field`
	
	edit.html: `edit.aDbfield=GUI Name for that Field`
	
	add.html: `add.aDbfield=GUI Name for that Field`
	
- It also defines which columns are UNIQUE in the database by defining them with the key `unique`, e.g.:

	`unique=name, path`
	
- If you want to load a value from a database field into the entity bean to use it in a handler, but you do not want it to be displayed in the GUI, define the constant 'NO_SHOW' 
as the GUI field name, for example:

	index.html: `list.secretDbField=NO_SHOW`
	
	view.html: `view.secretDbField=NO_SHOW`
	
	edit.html: `edit.secretDbField=NO_SHOW`
	
	add.html: `add.secretDbField=NO_SHOW`

- Furthermore, you can manually define transient values that are not read from or stored to database nor they are loaded within a bean, they are just delievered within the handler 
methods, so another value can be served for these transient columns/fields, e.g.:

	`transient=status`

- Last, you can manually specify default values with the `init`-prefix that should be shown in the `add.html`-template when a new record is created, e.g:

	`init.threshold=3`<br>
	`init.counter=0`

Your TODO's are the following after generating:

- Add mandatory (DB: not nullable) fields in the `add`-handler: only the mandatory fields need a default value in the add handler that are not present in the GUI! See all `Properties` 
handlers for more information.

- Remove unwanted GUI fields from `columns.cfg` for the views `view`, `add` and `edit`.
		
- Also Remove unwanted `<col span..>` tags in the `index.html`; e.g. if you removed standard fields `id`, `created` and `modified` from `columns.cfg`.

- Add entity to menu or admin menu and overwrite `hasAccess`-method for every handler if necessary.


**NOTE**: PLANT uses its own templates stored within the `gen/`-directory. Of course, you can even adjust these templates next to the block-templates, so they have the structure you 
want to have for your own web-app.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CRUD HOOKS -->
## CRUD Hooks

You can receive notifications using the [EventHanfler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/EventHandler.java) 
by registering an entity and a listener that is called back. The following callback methods are available:

- [CreateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/CreateListener.java)
  - `afterCreate(Model bean)`
- [UpdateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/UpdateListener.java)
  - `beforeUpdate(Model bean)`
  - `afterUpdate(Model bean)`
- [DeleteListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/DeleteListener.java)
  - `beforeDelete(Model bean)`



<!-- STANDARD HTML TEMPLATES -->
## Standard HTML Templates

The following standard HTML templates are present, which can be customized too of course:

- `web/html/blocks/*.html`:
 
	- Defines the layout of the page with its general elements such as head, header, menu admin menu, language menu, message block and script section (javascript).
  
	- They can be copied to other language directories too (e.g. `web/html/blocks/en/*.html`), if they need to be language specific, which in most cases, is not necessary. They also 
	serve as fallback templates, if the user requested a language that isn't found respectively the web-app is not yet translated into that language.
  
	- **NOTE**: Here, as well as with the generated HTML templates, the lookup algorithm is:
		a) First, lookup templates in requested language directory (2-letter ISO code)
		b) If not found, try the default language; this is the one that is first defined in the configuration, see parameter `web_languages`.
		c) If still not found, use the templates by omitting the language code, respectively the language directory in the `web/html`-directory structure.
	 
E.g.: the Englisch `index.html` for the entity `tasks` is here: `web/html/en/tasks/index.html`. If this resource is not available, then `web/html/tasks/index.html` is looked up. The 
same applies for `colums.cfg` resources.
	  
**NOTE**: Valid for templates and any other HTML files that are added to the `web/html`-directory structure:

- The relative URL (without Host, Port and Servlet name) requested by the web-app user is translated not 1-to-1 by the directory structure, but through **Routing**; see <a href="#routing">chapter</a>!

The following template variables are always parsed and you can use them as many times as you want:

- `{$lang}` : User's language, 2-ISO-code
- `{$user}` : User login name
- `{$userfull}` : Full user name (first and last name if available, otherwise login name)  
- `{$title}` : Page title (within add, edit, view and list)
- `{$id}` : Obfuscated object id (within add, edit and view)
- `{$dbid}` : Real database id (within add, edit and view)
- `{$csrfToken}` : CSRF token
- `{$theme}` : The currently default chosen style theme, e.g. `default`
- `{$antitheme}` : The default style theme to which can be switched to, e.g. `dark` 
- `{$displayName}` : Display name taken from the bean entity (not in `index.html`)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- JSON REST API -->
## JSON REST API

beetRoot comes with an "out-of-the-box" JSON REST API that serves any entity from the application. The API uses an API key that is defined within the "Settings" by the key 
`web.json.api.key`. The API key name itself can be changed in the beetRoot configuration `cfg/beetroot.cfg`.

A REST API call looks like this:

- `http://localhost:8778/tasks/index.json?apiKey=c56950c47cc9f055a17395d6bf94222d&page=1&fetchsize=2&sort=id&direction=desc&page=1`

Example Answer:

```JSON
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
```

As you can see, you can iterate with the `paginator` object through pages with your REST calls - the same way as you would navigate within an HTML `index` page.

JSON templates can be handled like HTML templates: Put them into the directory `web/html/..`. No user languages are used in any way by using this API. Therefore, you can dismiss the 
HTML template language directories and place the template, e.g. for entity `tasks`, directly here `web/html/tasks/index.json`; it looks like this:

```JSON
	{
	    "tasks": [
	        {$data}    
	    ],
	    {$paginator}    
	}
```

Also, you can create an own `columns.cfg` that is specific for the JSON request in this directory, for example looking like this:

```properties
	list_json.id=is
	list_json.name=name
	list_json.active=active
	list_json.laststatus=laststatus
```

It never has been easier using a REST API!

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ROUTING -->
## Routing

The router defines which resources are served by the requested URL. The out-of-box router is the `BeetRootDefaultRouter` and it reads the configuration `routing.xml`;
you simply add your own routes to this configuration. If you use the **PLANT** generator, it will output the necessary information to be used in the routing configuration. 

Let's have a look at some example routes in `routing.xml`:

```Xml
	<Package name="ch.autumo.beetroot.handler">
		<!-- Home  -->
	    <Route path="/:lang/home" handler="HomeHandler" name="home" />
	    <Route path="/:lang/home/index" handler="HomeHandler" name="home" />
		<!-- Files  -->
	    <Route path="/:lang/files/view" handler="ExampleDownloadHandler" name="files" />
	    <Route path="/:lang/files/add" handler="ExampleUploadHandler" name="files" />
	</Package>
	
	<Package name="ch.autumo.beetroot.handler.tasks">
		<!-- Tasks  -->
	    <Route path="/:lang/tasks" handler="TasksIndexHandler" name="tasks" />
	    <Route path="/:lang/tasks/index" handler="TasksIndexHandler" name="tasks" />
	    <Route path="/:lang/tasks/view" handler="TasksViewHandler" name="tasks" />
	    <Route path="/:lang/tasks/edit" handler="TasksEditHandler" name="tasks" />
	    <Route path="/:lang/tasks/add" handler="TasksAddHandler" name="tasks" />
	    <Route path="/:lang/tasks/delete" handler="TasksDeleteHandler" name="tasks" />
	    <Route path="/:lang/tasks/index.json" handler="TasksRESTIndexHandler" name="tasks" />
	</Package>
```

**Note**: Don't forget to change the package names in `routing.xml` if you rename them in your handler classes!

The requested URL's are translated to generated (or self-created) handlers which always must implement the method:

```Java
	public  String getResource();
```

For generated handlers (with the CRUD generator) this is usually not necessary, because they have a standard implementation (here for `index.html`):

```Java
	@Override
	public  String getResource() {
	    return "web/html/:lang/"+entity+"/index.html";
	}
```

If your handlers need customization, just overwrite this method. As you can see, here the translation of the requested URL takes place and points to the `web/html`-directory structure!

The language is replaced by the requested language, and if not found the lookup algorithm as earlier explained is executed. The `entity` name is assigned through the construction of the handler (see route 
examples above). That's it, more or less! 


**NOTE**: You never have to reference the servlet-name within any request URL not in the router, not even in your HTML templates when running beetRoot in a servlet container. Just specify 
the configuration parameter `web_html_ref_pre_url_part`.

For example ('beetroot' is the default servlet name/url):

  - http://localhost:8080/beetroot/en/tasks/add -> routes to `web/html/en/tasks/add.html` as specified by the corresponding handler. 

beetRoot handles every pre-url-path / servlet-name-path by its own, if configured correctly!


**IMPORTANT**: If you run beetRoot as a stand-alone server or in a servlet container, where beetRoot is installed on the ROOT path, NEVER specify the parameter `web_html_ref_pre_url_part`! 
When you define a HTML a-tag (e.g. `<a href="/{$lang}/tasks/index"....>`) or an image source or any other source (e.g. `<img src="/img/beetroot-100.png">`), you always point to the root-path 
"/". Tough, you have to include the language placeholder `:lang` for HTML templates always.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- LOGGING -->
## Logging

beetRoot uses [SLF4j](https://slf4j.org). For the stand-alone and tomcat wep-app version, the log4j2 implementation (the one that has NOT the log4j2 bug in it...!) is used and the default 
configuration `cfg/logging.xml` (stand-alone) and/or `logging.xml` (in tomcat web-app servlet directory) is used. If you want to specify your own, adjust it this way:

- stand-alone: Define a runtime parameter in the shell/bash script when starting Java:

	`-Dlog4j2.configurationFile=file:<log-cfg-path>/myLogConfig.xml`

- tomcat web-app: Define your log file in the 'WEB-INF/web.xml', parameter:

	`beetRootLogConfig`

As for jetty, they stand above all that "log-framework-soup" and they just simply use a SLF4j implementation that needs no further configuration. Hence, the library `slf4j.simple-x.y.z.jar` 
is packed into `beetroot-jetty.war`. The only concern is to add your package to the the jetty basic logging configuration in `{JETTY_BASE}/resources/jetty-logging.properties`:

```properties
	## Configure a level for specific logger
	ch.autumo.beetroot.LEVEL=INFO
```


**NOTE**: All logging levels are set to `INFO` in the beginning!

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- MAILING -->
## Mailing

Mailing supports Eclipse's Jakarta (`jakarta.mail`) as well as Oracle's JavaMail (`javax.mail`) implementation as originally defined by the [JavaMail project](https://javaee.github.io/javamail). 
By default, Jakarta is used. This possibly must be switched to JavaMail in certain environments that don't "interact" well with Jakarta. E.g., WebLogic uses Oracle's 
original implementation when using their mail-sessions as it should be done in that container. When using JavaMail, such a mail-session must be specified in the beetRoot configuration.

Check the configuration `cfg/beetroot.cfg` for further mailing options. Some of them can be even overwritten by the application "Settings"; check the "Settings" page in the beetRoot Web application.

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


**NOTE**: Java mail doesn't allow sending HTML with a head nor body-tag, hence you only are able to define HTML templates with tags that would be inside of a the body-tag. It is specification! 

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- JAVA TRANSLATIONS -->
## Java Translations

If you need translations within the Java code, you simply add language java resource bundle keys within the `web/lang` directory to the specific language resource bundle, e.g. `lang_en.properties`. 
You can change beetRoot's standard messages too. For every new language added respectively requested by the web-app user, add the corresponding language java resource bundle within this directory.

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

Also, in this case, you never have to reference a servlet name in any HTML template, you always point to the root-path "/", no matter what!

A few words about existing stylesheets:

  - `web/css/base.css`: Base styles, you don't want to change this in most cases.

  - `web/css/style.css`: Adjust your general web-application style here.

  - `web/css/refs.css`: Add here styles that reference images, fonts, etc. per url-references, e.g.: `url('/img/...');`. This is necessary, so beetRoot can translate resource URL's for a servlet 
  context correctly.

  - `web/css/jquery-ui.min.css`: Better tooltips.

  - `web/css/default.css`: Your default web-application styles and designs.

  - `web/css/theme-dark.css`: The default dark theme; you can add your own themes by naming it `theme-yourname.css` and HTTP-request it through the users' settings handler.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- HTTPS -->
## HTTPS

If you run beetRoot as a stand-alone server and you want to run it with the HTTPS protocol, there's a prepared and self-signed keystore file: `ssl/beetroot.jks` that is valid forever.

If you configure beetRoot to run with the HTTPS protocol (configuration parameter `ws_https`), you can use this keystore file and it is specified by default in the configuration (`keystore`). 
Your browser will still complain, because it is not issued by a valid Certificate Authority (CA), but you can force the browser to still load the web-app by adding this exception. If you run 
beetRoot in productive mode, you have to acquire a valid certificate and store it this keystore or in a an own; Java supports the PKCS\#12 format and Java keystore can be opened with tools 
such as this one: https://keystore-explorer.org. The password for `ssl/beetroot.jks` is **`beetroot`**.  

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap | Backlog

- Low Prio.: [Add SQLLite DB Connectivity](https://www.sqlite.org)

See also the [open issues](https://github.com/autumoswitzerland/autumo/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- LICENSE -->
## License

Distributed under the Apache License 2.0. See `LICENSE.md` for more information.

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
Copyright 2024, autumo Ltd., Switzerland


