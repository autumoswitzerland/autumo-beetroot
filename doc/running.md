# Running

## Modes

beetRoot can be run in two modes:

1. **As a standalone server that consists of**:
	- An administration interface for server commands and for executing distributed dispatcher modules.
	- A web-server that uses a [patched version](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java)
of the [RouterNanoHTTPD](https://github.com/NanoHttpd/nanohttpd/blob/master/nanolets/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java) and an [updated version 
of the NanoHttpd project](https://github.com/autumoswitzerland/nanohttpd) through the provided library `nanohttpd-BEETROOT-a.b.c.jar`.
	- An optional file-server (storage / find & download).

2. **Inside a servlet container** such as Tomcat, WebLogic or Jetty, which can optionally interact with the standalone server; see above.

## Servlet web containers

autumo BeetRoot, starting with version 3.2.0, uses the Servlet 5.0 API with HTTP/1.1, both stable and reliable standards. The following web containers are supported:

### Apache Tomcat

You must use version [10.1.x](https://tomcat.apache.org/download-10.cgi).

### Eclipse Jetty

You can use the latest Jetty web container (12.x), since it supports all standard APIs, but you need to set up Jetty for beetRoot like this:

```
$ java -jar $JETTY_HOME/start.jar --add-modules=server,http,ee9-deploy
```
	
For further instructions, see: [Jetty 12.1 Operations Guide](https://jetty.org/docs/jetty/12.1/operations-guide/begin/index.html).

After the setup, you may want to adjust the logging configuration in `jetty-home-12.x.x/base/resources/java-logging.properties`:

```
## Set logging levels from: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
org.eclipse.jetty.LEVEL=INFO
## Configure a level for an arbitrary logger tree
ch.autumo.LEVEL=INFO
```

You also may want to inform Jetty about the SLF4J bridge by adding or editing `jetty-home-12.x.x/base/resources/java-util-logging.properties`:

```
handlers=org.slf4j.bridge.SLF4JBridgeHandler
.level=FINEST
```

### Oracle WebLogic

For more information on installing Oracle WebLogic and deploying a web application, visit [Oracle](https://www.oracle.com/java/weblogic/).

Since BeetRoot now uses the Servlet API 5.0, you must use WebLogic 15.x, which supports Jakarta EE 9. If you are still using WebLogic 14.1, you need to stick with BeetRoot version 3.1.5 (Servlet API 4.0).

## Howto

All batch files and shell scripts are located in the `bin` directory.

1. Standalone usage:

	- Use `beetRoot.sh start` / `beetRoot.bat start`. The stand-alone beetRoot web-server reads the configuration `cfg/beetroot.cfg`. Most configuration parameters are used for servlet-container  operation too. All Configuration parameters are explained in the configuration file itself.
	- Surf to http://localhost:8778 (8778 is the default port for the stand-alone server).
	- The standalone server with all its resources is found within the package `autumo-beetRoot-x.y.z.zip`.
	- Use `beetRoot.sh stop` / `beetRoot.bat stop` to properly stop the stand-alone server. CTRL-C is a valid stop signal for the server.

2. Servlet web container:

	- Throw `beetroot.war` into Tomcat `webapps/` and `beetroot-jetty.war` into Jetty's `webapps/` folder. Fire it up and surf to http://localhost:8080/beetroot. Configure your containers as you wish. We recommend running the beetRoot application extracted so that any changes to the HTML templates and the model configuration (`columns.cfg` for each entity) can be made on the fly. Otherwise, Jetty is a practical test tool that resets the database states when using the internal H2 database when using a packed beetRoot version (WAR archive).
	- In Weblogic you most likely want to provide an open directory of beetRoot; use `beetroot-weblogic.zip` and unpack it into the `stage` directory of Weblogic. The Weblogic package is prepared to use Weblogic's resources such as mail sessions and JNDI data sources; simply customize `mail_session_name` and `db_ds_ext_jndi` in `beetroot.cfg` to use Weblogic's mail sessions and data sources.
	- If you want to keep everything under control with an own web-container, use the package `autumo-beetRoot-web-x.y.z.zip`.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
