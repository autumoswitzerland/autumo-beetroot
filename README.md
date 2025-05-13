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
[video-url]: https://youtu.be/X2_FVYiMnIE

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
    <img src="https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/beetroot.png" alt="Logo" width="200" height="200" />
  </a>

<h1 align="center">autumo beetRoot</h1>

  <p align="center">
    A Slim & Rapid Java Web Framework
    <br>
    <a href="https://github.com/autumoswitzerland/autumo/issues">Report Bug</a>
    ·
    <a href="https://github.com/autumoswitzerland/autumo/issues">Request Feature</a>
  </p>
</div>



<!-- WHAT IS BEETROOT -->
## What is beetRoot ?

beetRoot is a rapid Java web development and a complete and secure client-server 
framework that is ready to use, starts in less than a second and gives you a working 
initial setup for the current version, a transparent and clear way to configure the 
framework and its components, and the freedom to choose any web container or just use 
the optimized and embedded web container from the start. However, none of this stops 
you from customizing the `pom.xml` file and extend beetRoot as you wish.

If you know [CakePHP](https://cakePHP.org) for web development, you will like beetRoot.
It is based on the same principles and has a full CRUD generator that generates all views, 
the model specification and controllers (handlers in beetRoot's terminology) based on 
the database model! The client-server framework supports encrypted communication (SSL) 
as well as HTTP/HTTPS tunneling, provides an interface for downloading and uploading files
and can be extended with your own (distributed) modules.

[![autumo beetRoot 3.x - Quickstart](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-login.webp)][video-url]

[![autumo beetRoot 3.x - Quickstart](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-screen.webp)][video-url]
<p style="text-align: center;">
	<strong>
		<a href="https://youtu.be/X2_FVYiMnIE">autumo beetRoot 3.x - Quickstart Video</a>
	</strong>
</p>
<br />


![beetRoot Console](https://raw.githubusercontent.com/autumoswitzerland/autumo-beetroot/master/web/img/autumo-beetroot-console.webp)

Use the quickstart guide below, then go to http://localhost:8778 and log in:
<ul>
<li><strong>Default user</strong>: admin</li>
<li><strong>Default password</strong>: beetroot</li>
</ul>
<br />

When developing, take a look at the
[API Docs](https://products.autumo.ch/javadoc/autumo-beetroot/index.html).
<br />



<!-- beetRoot vs. Spring Boot -->
## autumo beetRoot vs. Spring Boot

### Advantages of autumo beetRoot over Spring Boot
| Feature/Aspect               | 🌰 **autumo beetRoot**                                     | 🌿 **Spring Boot**                                             |
|------------------------------|------------------------------------------------------------|----------------------------------------------------------------|
| **Startup Time**             | < 1 second                                                 | Several seconds (depending on config & dependencies)           |
| **Complexity**               | Very low — few annotations, no dependency injection magic  | High — requires understanding Spring internals, DI, beans, etc.|
| **Project Setup**            | Just unzip & run                                           | Maven/Gradle setup, lots of boilerplate                        |
| **Memory Usage**             | Low                                                        | Higher due to extensive runtime features                       |
| **UI & Template System**     | Built-in template engine with generated UI                 | Thymeleaf, FreeMarker, etc. require setup                      |
| **CRUD Automation**          | Auto-generates MVC artifacts from DB schema (PLANT)        | Manual or use of third-party generators                        |
| **Security (basic)**         | Built-in 2FA, CSRF, password encryption, role checks       | Requires Spring Security, setup is complex                     |
| **Deployment**               | Embedded lightweight server included                       | Uses embedded Tomcat/Jetty but with larger footprint           |
| **Learning Curve**           | Shallow — read code, understand fast                       | Steep — Spring ecosystem is large and deep                     |
| **Transparency**             | Explicit code, direct control                              | Convention over configuration, less transparent                |

### Use Cases Where autumo beetRoot Shines
- Small to mid-sized web apps or admin tools
- CRUD-heavy internal systems
- Environments with limited memory/CPU
- Teams that prefer simple, readable code over "magic"
- Replacing PHP-based admin panels with Java equivalents
- Web hosting environments without the need for massive frameworks

### Where Spring Boot Has Its Place
- Microservice architectures
- Advanced cloud-native features (e.g., Spring Cloud)
- Reactive programming
- Extensive ecosystem integration (Kafka, Elasticsearch, etc.)
- Enterprise-scale systems with multiple layers of abstraction

### Verdict
- If you're tired of Spring Boot's "annotation hell", slow startup, or complex configuration, and just want a clear, fast, and full-stack solution, 
then autumo beetRoot is a breath of fresh air.



<!-- QUICKSTART -->
## Quickstart

Enter the following commands into your terminal:

**Linux, macOS**

```NuShell
VERSION=3.1.4
PACKAGE=autumo-beetRoot-$VERSION

curl -LO https://github.com/autumoswitzerland/autumo-beetroot/releases/download/v$VERSION/$PACKAGE.zip

unzip $PACKAGE.zip
rm $PACKAGE.zip

$PACKAGE/bin/beetroot.sh start
```

**Windows**

```Batchfile
SET VERSION=3.1.4
SET PACKAGE=autumo-beetRoot-%VERSION%

curl -LO https://github.com/autumoswitzerland/autumo-beetroot/releases/download/v%VERSION%/%PACKAGE%.zip

tar -xf %PACKAGE%.zip
del %PACKAGE%.zip

%PACKAGE%\bin\beetroot.bat start
```



<!-- Documentation -->

## Documentation

🚀 beetRoot is already largely documented by its basic configuration. Take a look at the comments for each 
configuration key to get to know the possibilities and the extensive features of beetRoot: 
[cfg/beetroot.cfg](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/cfg/beetroot.cfg).

- [Distributions](doc/distributions.md)
- [Developing with beetRoot](doc/development.md)
- [Running](doc/running.md)
- [Configuration &amp; Passwords](doc/configuration.md)
- [Database Setup](doc/database.md)
- [PLANT: The CRUD Generator &amp; Columns Configuration](doc/plant.md)
- [Default HTML Templates](doc/templates.md)
- [Routing](doc/routing.md)
- [Handlers](doc/handlers.md)
- [User, Roles &amp; Authorization](doc/authorization.md)
- [CRUD Hooks](doc/hooks.md)
- [Referential integrity](doc/references.md)
- [Language Management](doc/translations.md)
- [JSON REST API](doc/json.md)
- [Dispatchers (Distributed Modules)](doc/dispatchers.md)
- [Logging](doc/logging.md)
- [Mailing](doc/mailing.md)
- [Web App Design &amp; JavaScript](doc/design.md)
- [HTTPS](doc/https.md)
- [Migration Guides](doc/migration.md)



<!-- BUILT WITH -->
## Built With

* [NanoHTTPD](http://nanohttpd.org)
* [Apache commons](https://commons.apache.org)
* [SLF4j](https://www.slf4j.org)
* [Log4j2](https://logging.apache.org/log4j/2.x)
* [H2 Database](https://h2database.com/)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail)
* [Checker Framework Qualifiers](https://checkerframework.org)
* [Google ZXing Java SE Extensions](https://github.com/zxing)
* [JQuery](https://jquery.com)
* ...and some more; see [THIRDPARTYLICENSES.html](https://htmlpreview.github.io/?https://github.com/autumoswitzerland/autumo-beetroot/blob/master/THIRDPARTYLICENSES.html)



<!-- Links -->
## Links

- [Quickstart Guide][video-url]
- [Website](https://beetroot.autumo.ch)
- [Products](https://products.autumo.ch)
- [Email](mailto:autumo.switzerland@gmail.com)



<!-- Features -->
## Features List

- Features to add, edit, view, list and delete entities
- Full CRUD generator **PLANT** for views, models and handlers
- One-to-many database relationships are fully applied in MVC layers
- Many-to-many relationship handling can be easily applied in MVC layers, including drag-and-drop assignments in the UI
- Easy-to-understand HTML template engine
- User roles and access control at controller level and within templates
- URL routing with language support
- Standard CSRF mechanism as well as obfuscated CRUD IDs within HTTP requests
- 2-factor authentication
- Argon2/PBKPD2 password encryption
- Password reset mechanism
- Extensible user settings
- Bean support with transient and unique fields
- User sessions are saved when servers are stopped
- Entities can be deployed via JSON REST API
- Language management (template translations and/or separate templates for each language)
- Tested on Apache Tomcat 9, Eclipse Jetty 10 and Oracle Weblogic 14
- SMS and phone call interfaces
- Mailing including mail templates
- Database connection pooling (HikariCP, with internal and external JNDI data sources)
- Supported databases: H2, MySQL, MariaDB, PostgreSQL and Oracle
- File upload and download
- Full MIME type control
- Dark theme and theme support
- File caching (resources and templates)
- HTTPS protocol and TLS for mail if configured
- Logging implementations other than log4j2 are supported
- Optimized console logging with colored sections (if required)
- Runs standalone as well as in common servlet containers like Apache Tomcat and Jetty on the URL root path as well as behind a servlet path without any changes to HTML templates etc.
- Secure client-server communication when beetRoot is installed in a servlet container separate from the beetRoot server and such communication is required to control backend processes
- Hierarchical resource loader; e.g. request German language, if not found, use configured default language, then use no language at all; “search until you find something useful” is the
algorithm for everything. Also, load resources from the file system (first), then as a resource inside packages (JAR, WAR) if they were not found before.
- And more stuff...

Enjoy!



<!-- DONATE -->
## Donate

Your donation helps to develop autumo beetRoot further. Thank you!

[![paypal](https://products.autumo.ch/img/DonateWithPayPal.png)](https://www.paypal.com/donate/?hosted_button_id=WWDWJG7Z4WJZC)

<br>
<br>
Copyright 2024, autumo Ltd., Switzerland

<p align="right"><a href="#top">&uarr;</a></p>

