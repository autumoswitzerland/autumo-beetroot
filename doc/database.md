## Database Setup

Every beetRoot package (standalone & web archive packages) come with a [H2 database](https://h2database.com) filled with sample data and the configuration points to this database. 
If you want to connect to your own database, simply change the connections parameters in the configuration.

To setup a new database scheme use the SQL-Script `db/install_<database-type>.sql` and customize it to your needs (initial data).

A word about the use of MySQL: Due to the GPL license, we do not distribute the MySQL Connector for Java and do not create a dependency on it. Visit the Oracle MySQL website and download it yourself if you want to use this connector. Note that the MariaAB Connector for Java also works for MySQL databases up to version 5.5 of MySQL or even for higher versions! Here you can find more valuable information in this context: [MariaDB License FAQ](https://mariadb.com/kb/en/licensing-faq).


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
