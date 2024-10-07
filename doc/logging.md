# Logging

beetRoot uses [SLF4j](https://slf4j.org) with [log4j2](https://logging.apache.org/log4j). For the standalone and Tomcat wep-app version, the log4j2 
implementation is used and the default configuration `cfg/logging.xml` (standalone) and/or `logging.xml` (in Tomcat web application servlet directory) 
is read for that purpose. If you want to specify your own logging configuration, adjust it this way:

- Standalone: Define a runtime parameter in the shell/bash script when starting Java:

	`-Dlog4j2.configurationFile=file:<log-cfg-path>/myLogConfig.xml`

- Tomcat web application: Define your logfile in the `WEB-INF/web.xml`: parameter:

	`beetRootLogConfig`

- As for Jetty, they stand above all that "log framework soup" and they just simply use a SLF4j implementation that needs no further configuration. Hence, the library `slf4j.simple-x.y.z.jar` is packed into `beetroot-jetty.war`. The only concern is to add your package to the the Jetty basic logging configuration in `{JETTY_BASE}/resources/jetty-logging.properties`:

	```properties
		## Configure a level for specific logger
		ch.autumo.beetroot.LEVEL=INFO
	```

- Nothing changes in Weblogic and log4j2 is used.

Any web container-specific logging configuration points to the correct logging directories with container-specific environment variables, so you usually have nothing to change unless you want to change the logfile name and default destination.

**NOTE**: All logging levels are set to `INFO` in the beginning!


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
