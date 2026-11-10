# Logging

beetRoot uses [SLF4j](https://slf4j.org) with [log4j2](https://logging.apache.org/log4j/2.12.x/index.html). 

- **Standalone Web Application Server**:  
  The default configuration `cfg/logging.xml` is used, which you can customize as needed. If you want to provide your own `log4j2` configuration, specify a runtime parameter in the shell/bash script when starting beetRoot:
   
    `-Dlog4j2.configurationFile=file:<log-cfg-path>/myLogConfig.xml`

- **Tomcat with beetRoot**:  
  The default configuration `logging.xml` in the root of the web application is used. When deployed by Tomcat, you can customize it as needed or specify another log file in `WEB-INF/web.xml` using the following parameter:
  
    `beetRootLogConfig`


- **Jetty with beetRoot:**  
  Jetty searches for a `log4j2.xml` file, which should be located in the `resources` directory of the Jetty base directory. Any additional logging configurations should be specified within these files:
  
    - `jetty-home-12.x.x/<base>/resources/java-logging.properties`
    - `jetty-home-12.x.x/<base>/resources/java-util-logging.properties`

  For detailed logging configuration for Jetty see [Running](running.md).

- **WebLogic with beetRoot:**  
  WebLogic also looks for the `log4j2.xml` file. In this case, the file is already correctly placed in the WebLogic distribution at `WEB-INF/log4j2.xml` and is properly read when using an open-directory deployment (staging), which is mandatory for WebLogic deployment. No further logging configuration is required for WebLogic.

**NOTE:** All logging levels are set to `INFO` by default.

**Live log tail**: The system log page streams the in-memory log live via a
WebSocket action (`/system/log/tail`) when WebSockets are enabled; see
[WebSockets](websockets.md). Without WebSockets the page reloads periodically,
as before.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
