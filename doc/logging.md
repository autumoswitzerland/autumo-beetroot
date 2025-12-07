# Logging

beetRoot uses [SLF4j](https://slf4j.org) with [log4j2](https://logging.apache.org/log4j/2.12.x/index.html). 

- **Standalone Web Application Server**:  
  The default configuration `cfg/logging.xml` is used, which you can customize as needed. If you want to provide your own `log4j2` configuration, specify a runtime parameter in the shell/bash script when starting beetRoot:
   
    `-Dlog4j2.configurationFile=file:<log-cfg-path>/myLogConfig.xml`

- **Tomcat with beetRoot**:  
  The default configuration `WEB-INF/log4j2.xml` within the web archive is used. When deployed by Tomcat, you can customize it as needed or specify your log file in `WEB-INF/web.xml` using the following parameter:
  
    `beetRootLogConfig`


- **Jetty with beetRoot:**  
  Jetty searches for a `log4j2.xml` file, which should be located in the `resources` directory of the Jetty base directory. Any additional logging configurations should be specified within these files:
  
    - `jetty-home-12.x.x/<base>/resources/java-logging.properties`
    - `jetty-home-12.x.x/<base>/resources/java-util-logging.properties`

  For detailed logging configuration for Jetty see [Running](running.md).

- **WebLogic with beetRoot:**  
  WebLogic also looks for the `log4j2.xml` file. In this case, the file is already correctly placed in the WebLogic distribution at `WEB-INF/log4j2.xml` and is properly read when using an open-directory deployment (staging), which is mandatory for WebLogic deployment. No further logging configuration is required for WebLogic.

**NOTE:** All logging levels are set to `INFO` by default.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
