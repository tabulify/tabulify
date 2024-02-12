-- Default app should be on the front-end or in a pref object
ALTER TABLE cs_realms.realm drop column realm_default_app_id;

-- Defer for initial insertion
ALTER TABLE cs_realms.realm ALTER CONSTRAINT realm_organization_owner_user_fkey DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE cs_realms.realm_sequence ALTER CONSTRAINT realm_sequence_sequence_realm_id_fkey  DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE cs_realms.realm_app ALTER constraint realm_app_app_realm_id_app_user_id_fkey DEFERRABLE INITIALLY IMMEDIATE;
