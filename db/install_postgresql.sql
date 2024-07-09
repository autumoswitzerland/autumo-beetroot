-----------------------------------------------------------------------------
-- (c) 2024 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_postgresql.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 03-Jul-2024  Michael Gasche                  -
-----------------------------------------------------------------------------



-- create database

-- CREATE user beetroot;
-- CREATE DATABASE beetroot OWNER beetroot;
-- \connect beetroot;


DROP TABLE IF EXISTS users_roles CASCADE;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS properties;

DROP SEQUENCE IF EXISTS roles_seq;
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

CREATE SEQUENCE roles_seq;
CREATE TABLE roles (
    id INT CHECK (id > 0) DEFAULT NEXTVAL ('roles_seq') PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024) default '',
    permissions VARCHAR(1024) default '',
    created TIMESTAMP(0) DEFAULT NOW(),
    modified TIMESTAMP(0) DEFAULT NULL,
    unique(name)
);
CREATE TABLE users_roles (
	user_id INT NOT NULL CHECK (user_id > 0),
    role_id INT NOT NULL CHECK (role_id > 0),
    created TIMESTAMP(0) DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);
CREATE INDEX idx_user_id ON users_roles(user_id);
CREATE INDEX idx_role_id ON users_roles(role_id);

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



--
-- Initial data
--


-- USERS
-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot' for admin
-- By default, the extended roles are used (own role table), the role
-- attribute in the user is obsolete!
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'admin', 'beetroot', 'beetroot@autumo.ch', 'NONE', 'theme=dark', '', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', NOW(), NOW());
-- initial password is 'beetroot' for operator
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'operator', 'beetroot', 'beetroot-op@autumo.ch', 'NONE', 'theme=default', '', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', NOW(), NOW());
-- initial password is 'beetroot' for controller
INSERT INTO users (id, username, password, email, lasttoken, settings, role, lang, two_fa, secretkey, created, modified) VALUES
(nextval('users_seq'), 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', 'NONE', 'theme=default', '', 'en', '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', NOW(), NOW());

-- ROLES
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(nextval('roles_seq'), 'Administrator', 'All privileges', '', NOW(), NOW());
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(nextval('roles_seq'), 'Operator', 'Task surveillance and management', '', NOW(), NOW());
INSERT INTO roles (id, name, description, permissions, created, modified) VALUES
(nextval('roles_seq'), 'Controller', 'Task surveillance', '', NOW(), NOW());

-- USERS_ROLES
INSERT INTO users_roles (user_id, role_id, created) VALUES
(1, 1, NOW());
INSERT INTO users_roles (user_id, role_id, created) VALUES
(2, 2, NOW());
INSERT INTO users_roles (user_id, role_id, created) VALUES
(3, 3, NOW());

-- TASKS (sample data)
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

-- SETTINGS
INSERT INTO properties (id, name, value) values
(nextval('properties_seq'),'web.json.api.key', 'abcedfabcedfabcedfabcedfabcedfab');
INSERT INTO properties (id, name, value) values
(nextval('properties_seq'),'security.2fa.code.email', 'No');
INSERT INTO properties (id, name, value) values
(nextval('properties_seq'),'log.size', '100');
INSERT INTO properties (id, name, value) values
(nextval('properties_seq'),'log.refresh.time', '60');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (nextval('properties_seq'),'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (nextval('properties_seq'),'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (nextval('properties_seq'),'mail.mailer', 'beetroot.web-mailer@autumo.ch');



-- GRANTS and future grants!
-- Step 1: Grant CRUD privileges on all existing tables
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.' || quote_ident(r.tablename) || ' TO beetroot';
    END LOOP;
END $$;
-- Step 2: Grant necessary privileges on all existing sequences
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public') LOOP
        EXECUTE 'GRANT USAGE, SELECT, UPDATE ON SEQUENCE public.' || quote_ident(r.sequence_name) || ' TO beetroot';
    END LOOP;
END $$;
-- Step 3: Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO beetroot;
-- Step 4: Set default privileges for future sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO beetroot;



-- Step 1: Grant CRUD privileges on all existing tables
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.' || quote_ident(r.tablename) || ' TO beetroot';
    END LOOP;
END $$;
-- Step 2: Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO beetroot;

