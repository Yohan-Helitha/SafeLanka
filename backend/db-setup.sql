-- Run once as the PostgreSQL superuser when using a native (non-Docker) install:
--   psql -U postgres -f backend/db-setup.sql
CREATE USER disaster WITH PASSWORD 'change-me';
CREATE DATABASE disaster OWNER disaster;
CREATE DATABASE disaster_test OWNER disaster;   -- optional, for tests without Docker
