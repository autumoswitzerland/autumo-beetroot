-----------------------------------------------------------------------------
-- (c) 2022 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-products
-- FILE:        db/install_beetroot.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 06-Feb-2022  Michael Gasche                  -
-----------------------------------------------------------------------------


-- create database

create database ifacexweb default character set utf8 default collate utf8_bin;
GRANT ALL PRIVILEGES ON beetroot.* to beetroot@'%' IDENTIFIED BY 'beetroot';
GRANT ALL PRIVILEGES ON beetroot.* to beetroot@'localhost' IDENTIFIED BY 'beetroot';


-- create tables

CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) not NULL,
    firstname VARCHAR(50) default '',
    lastname VARCHAR(50) default '',
    password VARCHAR(255) not NULL,
    email VARCHAR(255) not NULL,
    lasttoken varchar(255) not NULL default 'NONE',
    role VARCHAR(20) not NULL default 'Operator',
    lang VARCHAR(5) not NULL default 'en',
    created DATETIME DEFAULT NOW(),
    modified DATETIME DEFAULT NOW(),
    unique(username),
    unique(email)
);

CREATE TABLE tasks (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    guid VARCHAR(48) DEFAULT NULL,
    name VARCHAR(50) not NULL,
    path VARCHAR(255) not NULL,
    minute VARCHAR(128) not NULL,
    hour VARCHAR(128) not NULL,
    dayofmonth VARCHAR(128) not NULL,
    monthofyear VARCHAR(128) not NULL,
    dayofweek VARCHAR(128) not NULL,
    active BOOLEAN default true NOT NULL,
    laststatus BOOLEAN default true NOT NULL,
    lastexecuted DATETIME DEFAULT NULL,
    created DATETIME DEFAULT NOW(),
    modified DATETIME DEFAULT NOW(),
    unique(name)
);

CREATE TABLE properties (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  	name VARCHAR(255) not NULL,
  	value VARCHAR(2000) NULL,
    created DATETIME DEFAULT NOW(),
    modified DATETIME DEFAULT NOW(),
    unique(name)
);


-- init data

-- password is 'beetroot' for admin
INSERT INTO users (id, username, password, email, lasttoken, role, lang, created, modified) VALUES
(1, 'admin', 'GCkOkyNLBDYWY9OGy8zybw==', 'beetroot@autumo.ch', 'NONE', 'Administrator', 'en',  '2022-02-02 20:00:00', '2022-02-02 20:00:00');


-- sample data

-- password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, role, lang, created, modified) VALUES
(2, 'operator', 'GCkOkyNLBDYWY9OGy8zybw==', 'beetroot-op@autumo.ch', 'NONE', 'Operator', 'de',  '2022-02-02 20:00:00', '2022-02-02 20:00:00');

-- See 'https://www.guru99.com/crontab-in-linux-with-examples.html' for understanding cron-like examples
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(1, 'NONE', 'Task 1', '/path/task1.config', '0', '7,17', '*', '*', '*', '1', '1', '2022-02-02 20:00:00', '2022-01-20 20:00:00', '2022-01-20 20:00:00');
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(2, 'NONE', 'Task 2', '/path/task2.config', '*/5', '*', '*', '*', '*', '1', '0', '2022-02-02 20:00:00', '2022-01-20 20:00:00', '2022-01-20 20:00:00');
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(3, 'NONE', 'Task 3', '/path/task3.config', '0', '5', '0', '0', 'mon', '1', '1', '2022-02-02 20:00:00', '2022-01-20 20:00:00', '2022-01-20 20:00:00');
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(4, 'NONE', 'Task 4', '/path/task4.config', '*/3', '*', '*', '*', '*', '0', '1', '2022-02-02 20:00:00', '2022-01-20 20:00:00', '2022-01-20 20:00:00');
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(5, 'NONE', 'Task 5', '/path/task5.config', '*', '*', '*', 'feb,jun,sep', '*', '0', '0', '2022-02-02 20:00:00', '2022-01-20 20:00:00', '2022-01-20 20:00:00');

INSERT INTO properties (id, name, value) values 
(1,'mail.host', 'localhost');
INSERT INTO properties (id, name, value) values
(2,'mail.port', '2500');
INSERT INTO properties (id, name, value) values
(3,'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

