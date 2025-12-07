# Running

## Modes

beetRoot can be run in two modes:

1. **As a standalone server that consists of**:

	- An administration interface for server commands and for executing distributed dispatcher modules.
	- A web-server that uses a [patched version](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java) of the [RouterNanoHTTPD](https://github.com/NanoHttpd/nanohttpd/blob/master/nanolets/src/main/java/org/nanohttpd/router/RouterNanoHTTPD.java) and an [updated version of the NanoHttpd project](https://github.com/autumoswitzerland/nanohttpd) through the provided library `nanohttpd-BEETROOT-a.b.c.jar`.
	- An optional file-server (storage / find & download).

2. **Inside a servlet container** such as Tomcat, WebLogic or Jetty, which can optionally interact with the standalone server; see [Dispatchers](dispatchers.md).

## Standalone


1. Start the standalone beetRoot server using:

	```bssh
	bin/beetRoot.sh start   # Linux/macOS
	bin/beetRoot.bat start  # Windows
	```

2. The server reads its configuration from `cfg/beetroot.cfg`. Most configuration parameters are also applicable when running beetRoot in a servlet container. All parameters are documented within the configuration file itself.

3. Open your browser and navigate to `http://localhost:8778` (8778 is the default port for the standalone server).

4. The standalone server and all its resources are included in the package `autumo-beetRoot-x.y.z.zip`.

5. Stop the standalone server properly using:

	```bssh
	bin/beetRoot.sh stop    # Linux/macOS
	bin/beetRoot.bat stop   # Windows
	```

	Pressing `CTRL-C` also works as a stop signal for the server.
	
All batch files and shell scripts are located in the `bin` directory.

## Servlet Web Containers

autumo BeetRoot, starting with version 3.2.0, uses the Servlet 6.1 API (part of [Jakarta EE 11](https://jakarta.ee/specifications/platform/11/) along with HTTP/1.1, both of which are stable and well-established standards. It requires Java 17 as a minimum version, a necessary update to eliminate several security vulnerabilities (CVEs) that were tied to Java 11 due to dependencies on certain libraries.

For the latest WebLogic version 15.1, the Servlet API 5.0 is still required, and the delivered WebLogic package exceptionally includes this version.  

**Note**: It is mandatory to run the beetRoot application extracted from its web container distribution archives (Tomcat and Jetty extract them automatically). This also allows you to make changes on the fly to HTML templates and model configurations (e.g., `columns.cfg` for each entity).

The following web containers are supported.

### Apache Tomcat

1. You can use version 10 or 11; we recommend [11.0.x](https://tomcat.apache.org/download-11.cgi).

2. Place `beetroot.war` into the `webapps/` folder. Start Tomcat, then open your browser and navigate to `http://localhost:8080/beetroot`.

### Eclipse Jetty

1. You can use the latest Jetty web container [12.1.x](https://jetty.org/download.html), as it supports all required servlet APIs. To set up Jetty for beetRoot, create a base directory at a location of your choice (e.g. `jetty-home-12.x.x/base`) and execute the following command inside that directory:

	```bash
	$ java -jar $JETTY_HOME/start.jar --add-modules=server,http,ee11-deploy
	```
	
	Make sure the environment variable `JETTY_HOME` is set; it should point to the `jetty-home-12.x.x` directory. Also, set the environment variable `JETTY_BASE` to point to the base directory you created. 

3. For proper [Log4j 2 logging](https://logging.apache.org/log4j/2.12.x/index.html) via [SLF4J](https://www.slf4j.org/), you unfortunately need to provide your own `log4j2.xml` file. Jetty does not automatically read Log4j2 configurations from standard locations within the web archive. Therefore, create this file in your base directory at `resources/log4j2.xml`: 

	```xml
	<?xml version="1.0" encoding="UTF-8"?>
	<Configuration status="error" name="BeetRootConfig">
		<Properties>
			<Property name="basePath">${sys:jetty.base}/logs</Property>
		</Properties>
		<Appenders>
			<Console name="console" target="SYSTEM_OUT">
				<PatternLayout pattern="%-5p %d{yyyyMMdd-HH:mm:ss.SSS} [%-26.26t] %-30.30c{1.1.1.*} : %.-1000m%ex%n" />
	        </Console>
			<RollingFile name="file"
				fileName="${basePath}/beetroot.log"
				filePattern="${basePath}/beetroot-%d{yyyyMMdd}.log">
				<PatternLayout pattern="%-5p %d{yyyyMMdd-HH:mm:ss.SSS} [%-26.26t] %-30.30c{1.1.1.*} : %.-1000m%ex%n" />
				<Policies>
					<TimeBasedTriggeringPolicy interval="1" modulate="true" />
					<SizeBasedTriggeringPolicy size="10MB" />
				</Policies>
				<!-- Max 10 files will be created everyday -->
				<DefaultRolloverStrategy max="10">
					<Delete basePath="${basePath}" maxDepth="10">
						<!-- Delete all files older than 30 days -->
						<IfLastModified age="30d" />
					</Delete>
				</DefaultRolloverStrategy>
			</RollingFile>
		</Appenders>
		<Loggers>
			<Logger name="ch.autumo.beetroot" level="info" additivity="false">
				<AppenderRef ref="file" />
			</Logger>
			<Root level="info" additivity="false">
				<AppenderRef ref="file" />
			</Root>
		</Loggers>
	</Configuration>
	```

4. You may want to adjust the logging configuration in `jetty-home-12.x.x/<base>/resources/java-logging.properties`:

	```properties
	## Set logging levels from: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
	org.eclipse.jetty.LEVEL=INFO
	## Configure a level for an arbitrary logger tree
	ch.autumo.LEVEL=INFO
	```

5. You also need to inform Jetty about the SLF4J bridge by adding or editing `jetty-home-12.x.x/<base>/resources/java-util-logging.properties`:

	```properties
	handlers=org.slf4j.bridge.SLF4JBridgeHandler
	.level=FINEST
	```

6. Place `beetroot-jetty.war` into the `webapps/` folder of your Jetty base directory. Start Jetty, then open your browser and navigate `http://localhost:8080/beetroot`.

For further instructions, see: [Jetty 12.1 Operations Guide](https://jetty.org/docs/jetty/12.1/operations-guide/begin/index.html).

### Oracle WebLogic

beetRoot now runs on latest WebLogic [15.x](https://docs.oracle.com/en/middleware/standalone/weblogic-server/15.1.1/), which supports Jakarta EE 9. If you are still using WebLogic [14.1](https://docs.oracle.com/en/middleware/standalone/weblogic-server/14.1.1.0/index.html), you must stick with beetRoot version [3.1.5](https://github.com/autumoswitzerland/autumo-beetroot/releases/tag/v3.1.5) (Servlet API 4.0). As mentioned earlier, the beetRoot package for WebLogic is specifically bundled with the Servlet API 5.0.

We strongly recommend using the [WLST](https://docs.oracle.com/en/middleware/standalone/weblogic-server/15.1.1/wlstc/reference.html) (WebLogic Scripting Tool) to deploy beetRoot. Applications must be deployed as an exploded WAR or in an unpacked directory, as beetRoot requires an explicit exploded deployment in WebLogic.

1. For the open directory of beetRoot use `beetroot-weblogic.zip` and we suggest to unpack it into the `stage` directory of WebLogic's admin server. 

2. Deployment with WLST (example):

	```
	connect('user','pass','t3://localhost:7001')
	deploy(
	    appName='beetroot',
	    path='/users/oracle/home15/user_projects/domains/base_domain/servers/AdminServer/stage/beetroot',
	    targets='AdminServer',
	    upload='false',  # No uploading necessary, the folder is already there locally.
	    stageMode='nostage',
	)
	startApplication('beetroot')
	```

With WebLogic, it makes sense to use its services to manage resources such as mail services and database connections, which are fully supported by beetRoot. You can configure mail sessions and data sources via JNDI. For example JNDI configurations, see [beetroot.cfg](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/cfg/beetroot.cfg).

**Note**: The WebLogic package is preconfigured to use WebLogic services, including mail sessions and JNDI data sources. Simply customize `mail_session_name` and `db_ds_ext_jndi` in `beetroot.cfg` as needed, and set up the corresponding services in WebLogic using WLST or the WebLogic Remote Console UI—which, in our opinion, doesn’t always work reliably 🫢.

For more information on installing Oracle WebLogic and deploying web applications, visit [Oracle](https://www.oracle.com/java/weblogic/).

### Additional Generic Web Package

If you want full control using your own web container, use the generic web package `autumo-beetRoot-web-x.y.z.zip` and customize it as needed.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
