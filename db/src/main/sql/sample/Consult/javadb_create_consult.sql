
CREATE TABLE address (
	address_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
	line1 VARCHAR(50) NOT NULL,
	line2 VARCHAR(50),
	city VARCHAR(50) NOT NULL,
	region VARCHAR(50) NOT NULL,
	country VARCHAR(50) NOT NULL,
	postal_code VARCHAR(50) NOT NULL,
	CONSTRAINT address_pk PRIMARY KEY ( address_id )
);

CREATE TABLE consultant_status (
	status_id CHAR NOT NULL,
	description VARCHAR(50) NOT NULL,
	CONSTRAINT consultant_status_pk PRIMARY KEY ( status_id )
);

CREATE TABLE consultant (
	consultant_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
	status_id CHAR NOT NULL,
	email VARCHAR(50) NOT NULL,
	password VARCHAR(50) NOT NULL,
	hourly_rate DECIMAL(6,2) NOT NULL,
	billable_hourly_rate DECIMAL(6,2) NOT NULL,
	hire_date DATE,
	recruiter_id INTEGER,
	resume LONG VARCHAR,
	CONSTRAINT consultant_pk PRIMARY KEY ( consultant_id )
);

CREATE TABLE client (
	client_name VARCHAR(50) NOT NULL,
	client_department_number SMALLINT NOT NULL, 
	billing_address INTEGER NOT NULL,
	contact_email VARCHAR(50),
	contact_password VARCHAR(50),
	CONSTRAINT client_pk PRIMARY KEY ( client_name, client_department_number )
);

CREATE TABLE recruiter (
	recruiter_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
	email VARCHAR(50) NOT NULL,
	password VARCHAR(50) NOT NULL,
	client_name VARCHAR(50),
	client_department_number SMALLINT,
	CONSTRAINT recruiter_pk PRIMARY KEY ( recruiter_id )
);

CREATE TABLE project (
	client_name VARCHAR(50) NOT NULL,
	client_department_number SMALLINT NOT NULL,
	project_name VARCHAR(50) NOT NULL,
	contact_email VARCHAR(50),
	contact_password VARCHAR(50),
	CONSTRAINT project_pk PRIMARY KEY ( client_name, client_department_number, project_name )
);

CREATE TABLE project_consultant (
	client_name VARCHAR(50) NOT NULL,
	client_department_number SMALLINT NOT NULL,
	project_name VARCHAR(50) NOT NULL,
	consultant_id INTEGER NOT NULL,
	CONSTRAINT project_consultant_pk PRIMARY KEY ( client_name, client_department_number, project_name, consultant_id )
);

CREATE TABLE billable (
	billable_id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
	consultant_id INTEGER NOT NULL,
	client_name VARCHAR(50) NOT NULL,
	client_department_number SMALLINT NOT NULL,
	project_name VARCHAR(50) NOT NULL,
	start_date TIMESTAMP,
	end_date TIMESTAMP,
	hours SMALLINT NOT NULL,
	hourly_rate DECIMAL(6,2) NOT NULL,
	billable_hourly_rate DECIMAL(6,2) NOT NULL,
	description VARCHAR(50),
	artifacts CLOB, 
	CONSTRAINT billable_pk PRIMARY KEY ( billable_id )
);

ALTER TABLE consultant ADD CONSTRAINT consultant_fk_consultant_status FOREIGN KEY ( status_id ) REFERENCES consultant_status ( status_id );
ALTER TABLE consultant ADD CONSTRAINT consultant_fk_recruiter FOREIGN KEY ( recruiter_id ) REFERENCES recruiter ( recruiter_id );

ALTER TABLE client ADD CONSTRAINT client_fk_address FOREIGN KEY ( billing_address ) REFERENCES address ( address_id );
ALTER TABLE client ADD CONSTRAINT client_uk_billing_address UNIQUE ( billing_address );

ALTER TABLE recruiter ADD CONSTRAINT recruiter_fk_client FOREIGN KEY ( client_name, client_department_number ) REFERENCES client ( client_name, client_department_number );

ALTER TABLE project ADD CONSTRAINT project_fk_client FOREIGN KEY ( client_name, client_department_number ) REFERENCES client ( client_name, client_department_number );

ALTER TABLE project_consultant ADD CONSTRAINT project_consultant_fk_project FOREIGN KEY ( client_name, client_department_number, project_name ) REFERENCES project ( client_name, client_department_number, project_name );
ALTER TABLE project_consultant ADD CONSTRAINT project_consultant_fk_consultant FOREIGN KEY ( consultant_id ) REFERENCES consultant ( consultant_id );

ALTER TABLE billable ADD CONSTRAINT billable_fk_consultant FOREIGN KEY ( consultant_id ) REFERENCES consultant ( consultant_id );
ALTER TABLE billable ADD CONSTRAINT billable_fk_project FOREIGN KEY ( client_name, client_department_number, project_name ) REFERENCES project ( client_name, client_department_number, project_name );
