# CRUD Generator **PLANT** &amp; Columns Configuration
<div id="top"></div>

Start the CRUD generator with the script `plant.sh` / `plant.bat` and follow the steps!

The generator reads the entities in the database configured in `cfg/beetroot.cfg`, from which you can select one or all! If you require a different configuration file (e.g. with a different another database connection), you can specify this configuration file as an argument in the above shell/batch scripts. PLANT supports the databases MySQL, MariaDB, H2, Oracle and PostgreSQL
Databases.

**NOTE**: Entities for beetRoot MUST be named in plural form in the database, e.g.: tasks, users, cities, properties, cars, etc.

The following standard transformation of the entity names takes place in the generated HTML files:

- tasks -> task (singular)
- properties -> property (singular)

We therefore recommend that you use English names for your database entities, even if you can of course adapt the names in the generated HTMLs and this is even necessary 
if you copy the copy the templates for other languages.

![beetRoot Console](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-plant.webp)

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

Customize them to your needs (examples can be found in the existing handlers) and more **IMPORTANT**: Move the Java sources to another package! All generated files will be overwritten the next time if you regenerate the sources for the same entity!

The HTML templates and the model configuration are usually moved/copied to a subdirectory for languages:

- web/html/`{entity-plural-name}`/en/*.html and columns.cfg
- web/html/`{entity-plural-name}`/de/*.html and columns.cfg
- web/html/`{entity-plural-name}`/fr/*.html and columns.cfg
- etc.

We recommend that you create a backup copy of the originally generated HTML templates and the model configuration. They serve as a fallback scenario if the user requests a language that is not available!

The model configuration `columns.cfg` lists the columns/fields and executes the following for each entity:

- It determines which columns are visible in which order in each view (Add, Edit, View/Single record, Index/List) and defines a value for the field name. See existing files for example entities `Task` and/or `User`, e.g.:

	index.html: `list.aDbfield=GUI Name for that Field`

	view.html: `view.aDbfield=GUI Name for that Field`

	edit.html: `edit.aDbfield=GUI Name for that Field`

	add.html: `add.aDbfield=GUI Name for that Field`

- It also defines which columns in the database are UNIQUE by defining them with the `unique` key, e.g.:

	`unique=name, path`

- If you want to load a value from a database field into the entity bean in order to use it in a handler, but do not want it to be displayed in the GUI, define the constant 'NO_SHOW'
as the GUI field name, for example:

	index.html: `list.secretDbField=NO_SHOW`

	view.html: `view.secretDbField=NO_SHOW`

	edit.html: `edit.secretDbField=NO_SHOW`

	add.html: `add.secretDbField=NO_SHOW`

- In addition, you can manually define transient values that are not read from the database or stored in the database, nor are they loaded within a bean, they are only delivered within the handler methods so that a different value can be used for these transient columns/fields, e.g.:

	`transient=status`

- Finally, you can use the `init` prefix to manually specify default values to be displayed in the `add.html` template when a new record is created, e.g:

	`init.threshold=3`<br>
	`init.counter=0`

Your TODOs are the following after you have created the artifacts:

- Add mandatory (DB: non-deletable) fields to the `add` handler: only the mandatory fields need a default value in the add handler, which are not available in the GUI! See all `Properties` handler for more information.

- Remove unwanted GUI fields from `columns.cfg` for the views `index`, `view`, `add` and `edit`.

- Also Remove unwanted `<col span..>` tags in the `index.html`; for example, if you have removed the standard fields `id`, `created` and `modified` from the `columns.cfg` file.

- Add links to entities in the menu or admin menu and override the `hasAccess` method for each handler if necessary.

- Add your routes to `cfg/routing.xml`, e.g.:

	```XML
	<!-- Products -->
	<Package name="planted.beetroot.handler.products">
	    <Route path="/:lang/products" handler="ProductsIndexHandler" name="products" />
	    <Route path="/:lang/products/index" handler="ProductsIndexHandler" name="products" />
	    <Route path="/:lang/products/view" handler="ProductsViewHandler" name="products" />
	    <Route path="/:lang/products/edit" handler="ProductsEditHandler" name="products" />
	    <Route path="/:lang/products/add" handler="ProductsAddHandler" name="products" />
	    <Route path="/:lang/products/delete" handler="ProductsDeleteHandler" name="products" />
	</Package>
	```
Make sure to adjust the package name correctly if you move your classes!    

**NOTE**: PLANT uses its own templates, which are stored in the `gen/` directory. Of course, you can also customize these templates in addition to the block templates so that they have the structure you want for your own web application.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
