-----------------------------------------------------------------------------
-- (c) 2023 by autumo GmbH
-----------------------------------------------------------------------------
-- PROJECT:     autumo-beetroot
-- FILE:        db/test/install_h2_test.sql
-----------------------------------------------------------------------------
-- WHEN         WHO                             DESCRIPTION
-- 29-Nov-2023  Michael Gasche                  -
-----------------------------------------------------------------------------




-- Products

CREATE TABLE products (
	id int(11) NOT NULL auto_increment,
	name varchar(64) NOT NULL,
	int_sec_key varchar(32) NOT NULL,
	email_notes varchar(4000),
    create_user TINYINT(1) NOT NULL DEFAULT 0,
	created DATETIME NULL,
	modified DATETIME NULL,
    UNIQUE (name),
	PRIMARY KEY (id)
);

INSERT INTO products (id,name,int_sec_key,email_notes,create_user,created,modified) 
  VALUES (1,'ifaceX','abcdabcdabcdabcdabcdabcdabcdabcd','Some information.',1,NOW(),NOW());
INSERT INTO products (id,name,int_sec_key,email_notes,create_user,created,modified) 
  VALUES (2,'QTools','ef89ef89ef89ef89ef89ef89ef89ef89','',0,NOW(),NOW());



-- Variants

CREATE TABLE variants (
	id int(11) NOT NULL auto_increment,
	product_id int(11) NOT NULL,
	identifier varchar(32) NOT NULL,
	license_rt_type varchar(32) NOT NULL,
	description varchar(512),
	created DATETIME NULL,
	modified DATETIME NULL,
	FOREIGN KEY (product_id) REFERENCES products(id),	
	PRIMARY KEY (id)
);

INSERT INTO variants (id,product_id,identifier,description,license_rt_type,created,modified) 
  VALUES (1,1,'01','Basic Edition - 1 Interface Processor','runtime-01',NOW(),NOW());
INSERT INTO variants (id,product_id,identifier,description,license_rt_type,created,modified)
  VALUES (2,1,'05','Advanced Edition - 5 Interface Processors','runtime-05',NOW(),NOW());
INSERT INTO variants (id,product_id,identifier,description,license_rt_type,created,modified)
  VALUES (3,1,'10','Professional Edition - 10 Interface Processors','runtime-10',NOW(),NOW());



COMMIT;

