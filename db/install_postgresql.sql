-----------------------------------------------------------------------------
-- (c) 2022 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_postgresql.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 01-Oct-2022  Michael Gasche                  -
-----------------------------------------------------------------------------



-- create database

-- CREATE user beetroot;
-- CREATE DATABASE beetroot OWNER beetroot;
-- \connect beetroot;



DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS properties;

DROP SEQUENCE IF EXISTS users_seq;
DROP SEQUENCE IF EXISTS tasks_seq;
DROP SEQUENCE IF EXISTS properties_seq;

CREATE SEQUENCE users_seq;

CREATE TABLE users (
    id INT CHECK (id > 0) DEFAULT NEXTVAL ('users_seq') PRIMARY KEY,
    username VARCHAR(50) not NULL,
    firstname VARCHAR(50) DEFAULT '',
    lastname VARCHAR(50) DEFAULT '',
    password VARCHAR(1024) not NULL,
    email VARCHAR(256) not NULL,
    lasttoken varchar(256) not NULL DEFAULT 'NONE',
    settings varchar(1024) DEFAULT '',
    role VARCHAR(20) not NULL DEFAULT 'Operator',
    lang VARCHAR(5) not NULL DEFAULT 'en',
    two_fa BOOLEAN default false NOT NULL,
    secretkey VARCHAR(32) default '',
    created TIMESTAMP(0) DEFAULT NOW(),
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(username),
    unique(email)
);

CREATE SEQUENCE tasks_seq;

CREATE TABLE tasks (
    id INT CHECK (id > 0) DEFAULT NEXTVAL ('tasks_seq') PRIMARY KEY,
    guid VARCHAR(48) DEFAULT NULL,
    name VARCHAR(50) not NULL,
    path VARCHAR(255) not NULL,
    runmode VARCHAR(16) not NULL DEFAULT 'Serial',
    minute VARCHAR(64) not NULL,
    hour VARCHAR(64) not NULL,
    dayofmonth VARCHAR(64) not NULL,
    monthofyear VARCHAR(64) not NULL,
    dayofweek VARCHAR(64) not NULL,
    active BOOLEAN DEFAULT true NOT NULL,
    laststatus BOOLEAN DEFAULT true NOT NULL,
    lastexecuted TIMESTAMP(0) DEFAULT NULL,
    created TIMESTAMP(0) DEFAULT NOW(),
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(name)
);

CREATE SEQUENCE properties_seq;

CREATE TABLE properties (
    id INT CHECK (id > 0) DEFAULT NEXTVAL ('properties_seq') PRIMARY KEY,
  	name VARCHAR(256) not NULL,
  	value VARCHAR(2000) NULL,
    created TIMESTAMP(0) DEFAULT NOW(),
    modified TIMESTAMP(0) DEFAULT NOW(),
    unique(name)
);


-- init data

-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot' for admin
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'admin', 'beetroot', 'beetroot@autumo.ch', 'NONE', 'theme=dark', 'Administrator', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', NOW(), NOW());
-- initial password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'operator', 'beetroot', 'beetroot-op@autumo.ch', 'NONE', 'theme=default', 'Operator', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', NOW(), NOW());
-- initial password is 'beetroot' for controller
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', 'NONE', 'theme=default', 'Controller', 'en', '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', NOW(), NOW());

-- sample data
-- See 'https://www.guru99.com/crontab-in-linux-with-examples.html' for understanding cron-like examples
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(nextval('tasks_seq'), 'NONE', 'Task 1', '/path/task1.config', '0', '7,17', '*', '*', '*', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(nextval('tasks_seq'), 'NONE', 'Task 2', '/path/task2.config', '*/5', '*', '*', '*', '*', '1', '0', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(nextval('tasks_seq'), 'NONE', 'Task 3', '/path/task3.config', '0', '5', '0', '0', 'mon', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(nextval('tasks_seq'), 'NONE', 'Task 4', '/path/task4.config', '*/3', '*', '*', '*', '*', '0', '1', NOW(), NOW(), NOW());
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(nextval('tasks_seq'), 'NONE', 'Task 5', '/path/task5.config', '*', '*', '*', 'feb,jun,sep', '*', '0', '0', NOW(), NOW(), NOW());

-- basic settings
INSERT INTO properties (id, name, value) values
(nextval('properties_seq'),'web.json.api.key', 'abcedfabcedfabcedfabcedfabcedfab');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (nextval('properties_seq'),'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (nextval('properties_seq'),'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (nextval('properties_seq'),'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

