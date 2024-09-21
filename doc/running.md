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

## Howto

All batch files and shell scripts are located in the `bin` directory.

1. Standalone usage:
	- Use `beetRoot.sh start` / `beetRoot.bat start`. The stand-alone beetRoot web-server reads the configuration `cfg/beetroot.cfg`. Most configuration parameters
are used for servlet-container  operation too. All Configuration parameters are explained in the configuration file itself.
	- Surf to http://localhost:8778 (8778 is the default port for the stand-alone server).
	- The standalone server with all its resources is found within the package `autumo-beetRoot-x.y.z.zip`.
	- Use `beetRoot.sh stop` / `beetRoot.bat stop` to properly stop the stand-alone server. CTRL-C is a valid stop signal for the server.

2. Servlet web container:
	- Throw `beetroot.war` into Tomcat `webapps/` and `beetroot-jetty.war` into Jetty's `webapps/` folder. Fire it up and surf to http://localhost:8080/beetroot. Configure your
containers as you wish. We recommend running the beetRoot application extracted so that any changes to the HTML templates and the model configuration 
(`columns.cfg` for each entity) can be made on the fly. Otherwise, Jetty is a practical test tool that resets the database states when using the internal 
H2 database when using a packed beetRoot version (WAR archive).
	- In Weblogic you most likely want to provide an open directory of beetRoot; use `beetroot-weblogic.zip` and unpack it into the `stage` directory of Weblogic. The Weblogic
package is prepared to use Weblogic's resources such as mail sessions and JNDI data sources; simply customize `mail_session_name` and `db_ds_ext_jndi` in `beetroot.cfg` to use 
Weblogic's mail sessions and data sources.
	- If you want to keep everything under control with an own web-container, use the package `autumo-beetRoot-web-x.y.z.zip`.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
