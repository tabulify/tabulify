
ALTER TABLE consultant DROP CONSTRAINT consultant_fk_consultant_status;
ALTER TABLE consultant DROP CONSTRAINT consultant_fk_recruiter;

ALTER TABLE client DROP CONSTRAINT client_fk_address;
ALTER TABLE client DROP CONSTRAINT client_uk_billing_address;

ALTER TABLE recruiter DROP CONSTRAINT recruiter_fk_client;

ALTER TABLE project DROP CONSTRAINT project_fk_client;

ALTER TABLE project_consultant DROP CONSTRAINT project_consultant_fk_project;
ALTER TABLE project_consultant DROP CONSTRAINT project_consultant_fk_consultant;

ALTER TABLE billable DROP CONSTRAINT billable_fk_consultant;
ALTER TABLE billable DROP CONSTRAINT billable_fk_project;

DROP TABLE address;
DROP TABLE consultant_status;
DROP TABLE consultant;
DROP TABLE client;
DROP TABLE recruiter;
DROP TABLE project;
DROP TABLE project_consultant;
DROP TABLE billable;