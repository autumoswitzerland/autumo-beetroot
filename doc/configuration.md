# Configuration and Passwords

Take a look at `cfg/beetroot.cfg`. Each configuration parameter is explained. You can run beetRoot with ALL passwords encrypted if you want; define whether passwords used in the configuration file should be encrypted. The same applies to passwords stored in the beetRoot database table `users` and other tables consisting of `password` columns.

There are two configuration variables for this: `admin_pw_encoded` & `db_pw_encoded` (yes/no).

For security reasons, you should first change the secret key seed (`secret_key_seed`) and then generate new passwords with the tool `pwencoder.sh/pwencoder.bat`.
If you do this, you will need to change the initial encrypted password for the beetRoot user `admin` in the database to regain access!

**NOTE**: All passwords are **`beetroot`** in the beginning!


Furthermore, the configuration offers wide possibilities of customization for your app, such as:

- Buffer sizes
- Server ports
- File server
- SSL keystore (for internal server communication and HTTPS)
- Communication encryption
- Protocol (HTTP/HTTPS)
- Session storage
- [Dispatchers](dispatchers.md); for own distributed modules, if the web part runs in a web container and has to interact with the standalone server)
- Web server configurations
- Certain default web settings
- Default web view (in case of certain redirects)
- Web application languages
- Password encryption (see above)
- Auto-update of modification time-stamps
- DB access and DB type (connected through JDBC)
- Supported databases: MySQL, MariaDB, Java H2, Oracle, PostgreSQL
- Mail configuration inclusive TLS; some configuration parameters can be overwritten by values in the standard DB table `properties`
- ...and much more.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
