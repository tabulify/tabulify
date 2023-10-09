CREATE keyspace IF NOT EXISTS backend WITH replication = {'class':'SimpleStrategy','replication_factor':1};
USE backend;

/*
 * User Registration
 */
CREATE TABLE IF NOT EXISTS users (
    userid uuid,
    firstname text,
    lastname text,
    email text,
    created_date timestamp,
    PRIMARY KEY (userid)
) WITH comment = 'User Registration';


/**
 * Login
 */
CREATE TABLE IF NOT EXISTS usercredentials (
    email text,
    password text,
    userid uuid,
    PRIMARY KEY (email)
) WITH comment = 'Login';


