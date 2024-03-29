-----------------------------------------------------------------------------
-- (c) 2022 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_h2.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 12-Sep-2023  Michael Gasche                  -
-----------------------------------------------------------------------------



CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) not NULL,
    firstname VARCHAR(50) default '',
    lastname VARCHAR(50) default '',
    password VARCHAR(1024) not NULL,
    email VARCHAR(255) not NULL,
    lasttoken varchar(255) not NULL default 'NONE',
    settings varchar(1024) default '',
    role VARCHAR(20) not NULL default 'Operator',
    lang VARCHAR(5) not NULL default 'en',
    two_fa BOOLEAN default false NOT NULL,
    secretkey VARCHAR(32) default '',
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

-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot' for admin
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(1, 'admin', 'beetroot', 'beetroot@autumo.ch', 'NONE', 'theme=dark', 'Administrator', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', NOW(), NOW());
-- initial password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(2, 'operator', 'beetroot', 'beetroot-op@autumo.ch', 'NONE', 'theme=default', 'Operator', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', NOW(), NOW());
-- initial password is 'beetroot' for controller
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(3, 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', 'NONE', 'theme=default', 'Controller', 'en', '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', NOW(), NOW());

-- sample data
-- See 'https://www.guru99.com/crontab-in-linux-with-examples.html' for understanding cron-like examples
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(1, 'NONE', 'Task 1', '/path/task1.config', '0', '7,17', '*', '*', '*', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(2, 'NONE', 'Task 2', '/path/task2.config', '*/5', '*', '*', '*', '*', '1', '0', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(3, 'NONE', 'Task 3', '/path/task3.config', '0', '5', '0', '0', 'mon', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(4, 'NONE', 'Task 4', '/path/task4.config', '*/3', '*', '*', '*', '*', '0', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(5, 'NONE', 'Task 5', '/path/task5.config', '*', '*', '*', 'feb,jun,sep', '*', '0', '0', NOW(), NOW(), NOW());

-- basic settings
INSERT INTO properties (id, name, value) values
(1,'web.json.api.key', 'abcedfabcedfabcedfabcedfabcedfab');
INSERT INTO properties (id, name, value) values
(2,'security.2fa.code.email', 'No');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (2,'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (3,'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (4,'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

