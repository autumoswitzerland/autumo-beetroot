-----------------------------------------------------------------------------
-- (c) 2024 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_oracle.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 07-Jul-2024  Michael Gasche                  -
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


ALTER TABLE users_roles DROP CONSTRAINT fk_user;
ALTER TABLE users_roles DROP CONSTRAINT fk_role;

DROP SEQUENCE roles_seq;
DROP SEQUENCE users_seq;
DROP SEQUENCE tasks_seq;
DROP SEQUENCE properties_seq;

DROP INDEX idx_role_id;
DROP INDEX idx_user_id;

DROP TABLE roles;
DROP TABLE users;
DROP TABLE users_roles CASCADE CONSTRAINTS;
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

CREATE TABLE roles (
    id NUMBER(10) CHECK (id > 0) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024) default '',
    permissions VARCHAR(1024) default '',
    created TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(name)
);
-- Generate ID using sequence and trigger
CREATE SEQUENCE roles_seq START WITH 1 INCREMENT BY 1;
CREATE OR REPLACE TRIGGER roles_seq_tr
	BEFORE INSERT ON roles FOR EACH ROW
	WHEN (NEW.id IS NULL)
BEGIN
	SELECT roles_seq.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/

CREATE TABLE users_roles (
    user_id NUMBER(10) NOT NULL,
    role_id NUMBER(10) NOT NULL,
    created TIMESTAMP(0) DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);
CREATE INDEX idx_user_id on users_roles (user_id);
CREATE INDEX idx_role_id on users_roles (role_id);

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



--
-- Initial data
--


-- USERS
-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot' for admin
-- By default, the extended roles are used (own role table), the role
-- attribute in the user is obsolete!
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'admin', 'beetroot', 'beetroot@autumo.ch', 'NONE', 'theme=dark', ' ', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- initial password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'operator', 'beetroot', 'beetroot-op@autumo.ch', 'NONE', 'theme=default', ' ', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- initial password is 'beetroot' for controller
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(users_seq.NEXTVAL, 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', 'NONE', 'theme=default', ' ', 'en', '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ROLES
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(roles_seq.NEXTVAL, 'Administrator', 'All privileges', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(roles_seq.NEXTVAL, 'Operator', 'Task surveillance and management', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(roles_seq.NEXTVAL, 'Controller', 'Task surveillance', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- USERS_ROLES
INSERT INTO users_roles (user_id, role_id, created) VALUES
(1, 1, CURRENT_TIMESTAMP);
INSERT INTO users_roles (user_id, role_id, created) VALUES
(2, 2, CURRENT_TIMESTAMP);
INSERT INTO users_roles (user_id, role_id, created) VALUES
(3, 3, CURRENT_TIMESTAMP);

-- TASKS (sample data)
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
INSERT INTO properties (id, name, value) values
(properties_seq.NEXTVAL,'security.2fa.code.email', 'No');
INSERT INTO properties (id, name, value) values
(properties_seq.NEXTVAL,'log.size', '100');
INSERT INTO properties (id, name, value) values
(properties_seq.NEXTVAL,'log.refresh.time', '60');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (properties_seq.NEXTVAL,'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (properties_seq.NEXTVAL,'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (properties_seq.NEXTVAL,'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

