## Database Setup

Every beetRoot package (standalone & web archive packages) comes with a [H2 database](https://h2database.com) filled with sample data and the configuration points to this database. 
If you want to connect to your own database, simply change the connections parameters in the configuration.

In addition to the default database connection setup, which uses a JDBC URL, user, and password, you can also configure an external JNDI data source within `cfg/beetroot.cfg`, or even specify a custom database driver.

Furthermore, you may define your own internal data source. In this case, the external JNDI data source must not be defined, and the internal data source properties with the prefix `db_ds_int_` must be uncommented.


**A word about the use of MySQL**: Due to the GPL license, we do not distribute the MySQL Connector for Java and do not create a dependency on it. Visit the Oracle MySQL website and download it yourself if you want to use this connector. Note that the MariaAB Connector for Java also works for MySQL databases up to version 5.5 of MySQL or even for higher versions! Here you can find more valuable information in this context: [MariaDB License FAQ](https://mariadb.com/kb/en/licensing-faq).


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
