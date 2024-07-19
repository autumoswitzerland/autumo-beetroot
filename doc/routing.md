# Routing

The router defines which resources are served by the requested URL. The out-of-box router is the `BeetRootDefaultRouter` and it reads the configuration `cfg/routing.xml`; 
you simply add your own routes to this configuration. If you use the **PLANT** generator, it will output the necessary information to be used in the routing configuration.

Let's have a look at some example routes in `routing.xml`:

```XML
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

**Note**: Don't forget to change the package names in `cfg/routing.xml` if they change in your handler classes!

The requested URL's are translated to generated (or self-created) handlers which always must implement the method:

```Java
	public  String getResource();
```

For generated handlers this is usually not necessary, because they have a standard implementation (here for `index.html`):

```Java
	@Override
	public  String getResource() {
	    return "web/html/:lang/"+entity+"/index.html";
	}
```

If your handlers need customization, just overwrite this method. As you can see, here the translation of the requested URL takes place and points to the `web/html`-directory structure!

The language is replaced by the requested language, and if not found the resource lookup algorithm is executed. The `entity` name for a route is assigned through the construction of the handler. That's it, more or less!


**NOTE**: You never have to reference a servlet-name within any request URL not in the router, not even in your HTML templates when running beetRoot in a servlet container. Just specify the configuration parameter `web_html_ref_pre_url_part` when running beetRoot in a servlet container to specify the servlet name.

For example ('beetroot' is the default servlet name/url):

  - http://localhost:8080/beetroot/en/tasks/add -> routes to the resource `web/html/en/tasks/add.html` as specified by the corresponding handler.

beetRoot handles every pre-url-path / servlet-name-path by its own, if configured correctly!


**IMPORTANT**: If you are running beetRoot as a standalone server or in a servlet container where beetRoot is installed on the ROOT path, NEVER specify the parameter `web_html_ref_pre_url_part`!

When you define a HTML a-tag (e.g. `<a href="/{$lang}/tasks/index"....>`) or an image source or any other source (e.g. `<img src="/img/beetroot-100.png">`), you always point to the root-path
"/". Tough, you have to include the language placeholder `:lang` for HTML templates always.


<br>
<br>
<a href="../README.md">[Main Page]</a>
