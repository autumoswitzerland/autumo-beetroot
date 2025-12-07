# Configuration and Passwords

Take a look at `cfg/beetroot.cfg`. Each configuration parameter is documented there.  
You can run beetRoot with **all passwords encrypted** if desired; simply define whether the passwords used in the configuration file should be stored in encrypted form.  
The same applies to passwords in the beetRoot database tables, such as the `users` table or any other table containing a `password` column.

There are two configuration variables controlling this:

- `admin_pw_encoded`  
- `db_pw_encoded`  
(values: `yes` / `no`)

For security reasons, you should first change the `secret_key_seed` and then generate new passwords using the `pwencoder.sh` / `pwencoder.bat` tool.  
After doing so, you must update the initial encrypted password of the beetRoot `admin` user in the database; otherwise, you will no longer be able to log in.

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
