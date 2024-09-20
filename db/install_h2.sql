-----------------------------------------------------------------------------
-- (c) 2024 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/install_h2.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 03-Jul-2024  Michael Gasche                  -
-----------------------------------------------------------------------------



-- H2 version 2.2.224 - MySQL mode


SET IGNORECASE='TRUE';

CREATE TABLE users (
    "id" INT AUTO_INCREMENT PRIMARY KEY,
    "username" VARCHAR(50) NOT NULL,
    "firstname" VARCHAR(50) DEFAULT '',
    "lastname" VARCHAR(50) DEFAULT '',
    "password" VARCHAR(1024) NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "phone" VARCHAR(15) DEFAULT '',
    "lasttoken" varchar(255) NOT NULL DEFAULT 'NONE',
    "settings" varchar(1024) DEFAULT '',
    "role" VARCHAR(20) NOT NULL DEFAULT 'Operator',
    "lang" VARCHAR(5) DEFAULT NULL,
    "two_fa" BOOLEAN DEFAULT false NOT NULL,
    "secretkey" VARCHAR(32) DEFAULT '',
    "created" TIMESTAMP(3) DEFAULT NOW(),
    "modified" TIMESTAMP(3) DEFAULT NOW(),
    unique("username"),
    unique("email")
);

CREATE TABLE roles (
    "id" INT AUTO_INCREMENT PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "description" VARCHAR(1024) DEFAULT '',
    "permissions" VARCHAR(1024) DEFAULT '',
    "created" TIMESTAMP(3) DEFAULT NOW(),
    "modified" TIMESTAMP(3) DEFAULT NOW(),
    unique("name")
);

CREATE TABLE users_roles (
    "user_id" INT NOT NULL,
    "role_id" INT NOT NULL,
    "created" TIMESTAMP(3) DEFAULT NOW(),
    PRIMARY KEY ("user_id", "role_id"),
    FOREIGN KEY ("user_id") REFERENCES users("id") ON DELETE CASCADE,
    FOREIGN KEY ("role_id") REFERENCES roles("id") ON DELETE CASCADE
);
CREATE INDEX "idx_user_id" ON users_roles("user_id");
CREATE INDEX "idx_role_id" ON users_roles("role_id");

CREATE TABLE tasks (
    "id" INT AUTO_INCREMENT PRIMARY KEY,
    "guid" VARCHAR(48) DEFAULT NULL,
    "name" VARCHAR(50) NOT NULL,
    "path" VARCHAR(255) NOT NULL,
    "minute" VARCHAR(128) NOT NULL,
    "hour" VARCHAR(128) NOT NULL,
    "dayofmonth" VARCHAR(128) NOT NULL,
    "monthofyear" VARCHAR(128) NOT NULL,
    "dayofweek" VARCHAR(128) NOT NULL,
    "active" BOOLEAN DEFAULT true NOT NULL,
    "laststatus" BOOLEAN DEFAULT true NOT NULL,
    "lastexecuted" TIMESTAMP(3) DEFAULT NULL,
    "created" TIMESTAMP(3) DEFAULT NOW(),
    "modified" TIMESTAMP(3) DEFAULT NOW(),
    unique("name")
);

CREATE TABLE properties (
    "id" INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  	"name" VARCHAR(255) NOT NULL,
  	"value" VARCHAR(2000) NULL,
    "created" TIMESTAMP(3) DEFAULT NOW(),
    "modified" TIMESTAMP(3) DEFAULT NOW(),
    unique("name")
);



--
-- Initial data
--


-- USERS
-- NOTE: Passwords can be encrypted in database; see 'beetroot.cfg'
-- initial password is 'beetroot'
-- By default, the extended roles are used (own role table), the role
-- attribute in the user is obsolete!
INSERT INTO users ("id", "username", "password", "email", "phone", "lasttoken", "settings", "role", "lang", "two_fa", "secretkey", "created", "modified") VALUES
(1, 'admin', 'beetroot', 'beetroot@autumo.ch', '', 'NONE', 'theme=dark', '', 'en', '0', 'LD6I2VCIXJOVKBEF6CAID5UWHWA32SQL', NOW(), NOW());
INSERT INTO users ("id", "username", "password", "email", "phone", "lasttoken", "settings", "role", "lang", "two_fa", "secretkey", "created", "modified") VALUES
(2, 'operator', 'beetroot', 'beetroot-op@autumo.ch', '', 'NONE', 'theme=default', '', 'de', '0', 'LERDNDDT2SONGR6NRBRQ2WL5JCPADSH2', NOW(), NOW());
INSERT INTO users ("id", "username", "password", "email", "phone", "lasttoken", "settings", "role", "lang", "two_fa", "secretkey", "created", "modified") VALUES
(3, 'controller', 'beetroot', 'beetroot-ctrl@autumo.ch', '', 'NONE', 'theme=default', '', null, '0', 'HC6TBZ75IQMGT5ZUOPTV4S43NJPCDNUV', NOW(), NOW());

-- ROLES
INSERT INTO roles ("id", "name", "description", "permissions", "created", "modified") VALUES
(1, 'Administrator', 'All privileges', '', NOW(), NOW());
INSERT INTO roles ("id", "name", "description", "permissions", "created", "modified") VALUES
(2, 'Operator', 'Task surveillance and management', '', NOW(), NOW());
INSERT INTO roles ("id", "name", "description", "permissions", "created", "modified") VALUES
(3, 'Controller', 'Task surveillance', '', NOW(), NOW());

-- USERS_ROLES
INSERT INTO users_roles ("user_id", "role_id", "created") VALUES
(1, 1, NOW());
INSERT INTO users_roles ("user_id", "role_id", "created") VALUES
(2, 2, NOW());
INSERT INTO users_roles ("user_id", "role_id", "created") VALUES
(3, 3, NOW());

-- TASKS (sample data)
-- See 'https://www.guru99.com/crontab-in-linux-with-examples.html' for understanding cron-like examples
INSERT INTO tasks ("id", "guid", "name", "path", "minute", "hour", "dayofmonth", "monthofyear", "dayofweek", "active", "laststatus", "lastexecuted", "created", "modified") VALUES
(1, 'NONE', 'Task 1', '/path/task1.config', '0', '7,17', '*', '*', '*', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks ("id", "guid", "name", "path", "minute", "hour", "dayofmonth", "monthofyear", "dayofweek", "active", "laststatus", "lastexecuted", "created", "modified") VALUES
(2, 'NONE', 'Task 2', '/path/task2.config', '*/5', '*', '*', '*', '*', '1', '0', NOW(), NOW(), NOW());
INSERT INTO tasks ("id", "guid", "name", "path", "minute", "hour", "dayofmonth", "monthofyear", "dayofweek", "active", "laststatus", "lastexecuted", "created", "modified") VALUES
(3, 'NONE', 'Task 3', '/path/task3.config', '0', '5', '0', '0', 'mon', '1', '1', NOW(), NOW(), NOW());
INSERT INTO tasks ("id", "guid", "name", "path", "minute", "hour", "dayofmonth", "monthofyear", "dayofweek", "active", "laststatus", "lastexecuted", "created", "modified") VALUES
(4, 'NONE', 'Task 4', '/path/task4.config', '*/3', '*', '*', '*', '*', '0', '1', NOW(), NOW(), NOW());
INSERT INTO tasks ("id", "guid", "name", "path", "minute", "hour", "dayofmonth", "monthofyear", "dayofweek", "active", "laststatus", "lastexecuted", "created", "modified") VALUES
(5, 'NONE', 'Task 5', '/path/task5.config', '*', '*', '*', 'feb,jun,sep', '*', '0', '0', NOW(), NOW(), NOW());

-- SETTINGS
INSERT INTO properties ("id", "name", "value") values
(1,'web.json.api.key', 'abcedfabcedfabcedfabcedfabcedfab');
INSERT INTO properties ("id", "name", "value") values
(2,'security.2fa.code.email', 'Off');
INSERT INTO properties ("id", "name", "value") values
(3,'security.2fa.code.sms', 'Off');
INSERT INTO properties ("id", "name", "value") values
(4,'log.size', '100');
INSERT INTO properties ("id", "name", "value") values
(5,'log.refresh.time', '60');
-- NOTE: some mail settings in the 'beetroot.cfg' can be overwritten here:
-- INSERT INTO properties (id, name, value) values 
-- (5,'mail.host', 'localhost');
-- INSERT INTO properties (id, name, value) values
-- (6,'mail.port', '2500');
-- INSERT INTO properties (id, name, value) values
-- (7,'mail.mailer', 'beetroot.web-mailer@autumo.ch');



COMMIT;

