-----------------------------------------------------------------------------
-- (c) 2022 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_oracle.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 01-Oct-2022  Michael Gasche                  -
-----------------------------------------------------------------------------



-- create database

-- CREATE USER beetroot IDENTIFIED BY ????;
-- GRANT create session TO beetroot;
-- GRANT create table TO beetroot;
-- GRANT create any trigger TO beetroot;
-- GRANT create sequence TO beetroot;
--
-- GRANT UNLIMITED TABLESPACE TO beetroot;
--   or better:
-- ALTER USER beetroot QUOTA 100M ON ????



DROP SEQUENCE users_seq;
DROP SEQUENCE tasks_seq;
DROP SEQUENCE properties_seq;

DROP TABLE users;
DROP TABLE tasks;
DROP TABLE properties;

CREATE TABLE users (
    id NUMBER(10) CHECK (id > 0) PRIMARY KEY,
    username VARCHAR2(50) not NULL,
    firstname VARCHAR2(50) DEFAULT '',
    lastname VARCHAR2(50) DEFAULT '',
    password VARCHAR2(1024) not NULL,
    email VARCHAR2(256) not NULL,
    lasttoken varchar2(256) DEFAULT 'NONE' not NULL,
    settings varchar2(1024) DEFAULT '',
    role VARCHAR2(20) DEFAULT 'Operator' not NULL,
    lang VARCHAR2(5) DEFAULT 'en' not NULL,
    two_fa NUMBER(1) DEFAULT 0 NOT NULL,
    secretkey VARCHAR2(32) DEFAULT '',
    created TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(username),
    unique(email)
);


-- Generate ID using sequence and trigger
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER users_seq_tr
 BEFORE INSERT ON users FOR EACH ROW
 WHEN (NEW.id IS NULL)
BEGIN
 SELECT users_seq.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/


CREATE TABLE tasks (
    id NUMBER(10) CHECK (id > 0) PRIMARY KEY,
    guid VARCHAR2(48) DEFAULT NULL,
    name VARCHAR2(50) not NULL,
    path VARCHAR2(255) not NULL,
    minute VARCHAR2(64) not NULL,
    hour VARCHAR2(64) not NULL,
    dayofmonth VARCHAR2(64) not NULL,
    monthofyear VARCHAR2(64) not NULL,
    dayofweek VARCHAR2(64) not NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    laststatus NUMBER(1) DEFAULT 1 NOT NULL,
    lastexecuted TIMESTAMP(0) DEFAULT NULL,
    created TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(name)
);

-- Generate ID using sequence and trigger
CREATE SEQUENCE tasks_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER tasks_seq_tr
 BEFORE INSERT ON tasks FOR EACH ROW
 WHEN (NEW.id IS NULL)
BEGIN
 SELECT tasks_seq.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/

CREATE TABLE properties (
    id NUMBER(10) CHECK (id > 0) PRIMARY KEY,
  	name VARCHAR2(256) not NULL,
  	value VARCHAR2(2000) NULL,
    created TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    modified TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    unique(name)
);

-- Generate ID using sequence and trigger
CREATE SEQUENCE properties_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER properties_seq_tr
 BEFORE INSERT ON properties FOR EACH ROW
 WHEN (NEW.id IS NULL)
BEGIN
 SELECT properties_seq.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/


-- init data

-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot' for admin
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'admin', 'beetroot', 'beetroot@autumo.ch', 'NONE', 'theme=dark', 'Administrator', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- initial password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'operator', 'beetroot', 'beetroot-op@autumo.ch', 'NONE', 'theme=default', 'Operator', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- initial password is 'beetroot' for controller
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', 'NONE', 'theme=default', 'Controller', 'en', '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- sample data
-- See 'https://www.guru99.com/crontab-in-linux-with-examples.html' for understanding cron-like examples
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(tasks_seq.NEXTVAL, 'NONE', 'Task 1', '/path/task1.config', '0', '7,17', '*', '*', '*', '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(tasks_seq.NEXTVAL, 'NONE', 'Task 2', '/path/task2.config', '*/5', '*', '*', '*', '*', '1', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(tasks_seq.NEXTVAL, 'NONE', 'Task 3', '/path/task3.config', '0', '5', '0', '0', 'mon', '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(tasks_seq.NEXTVAL, 'NONE', 'Task 4', '/path/task4.config', '*/3', '*', '*', '*', '*', '0', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tasks (id, guid, name, path, minute, hour, dayofmonth, monthofyear, dayofweek, active, laststatus, lastexecuted, created, modified) VALUES
(tasks_seq.NEXTVAL, 'NONE', 'Task 5', '/path/task5.config', '*', '*', '*', 'feb,jun,sep', '*', '0', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- basic settings
INSERT INTO properties (id, name, value) values
(properties_seq.NEXTVAL,'web.json.api.key', 'abcedfabcedfabcedfabcedfabcedfab');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (properties_seq.NEXTVAL,'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (properties_seq.NEXTVAL,'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (properties_seq.NEXTVAL,'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

