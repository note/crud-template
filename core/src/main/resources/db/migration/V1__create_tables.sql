CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id       UUID NOT NULL,
  email    VARCHAR(255) NOT NULL,
  phone    VARCHAR(30) NOT NULL,
  password CHAR(60) NOT NULL,
  PRIMARY KEY(id),
  UNIQUE (email)
);

CREATE TABLE bookmarks (
  id          UUID NOT NULL,
  user_id     uuid REFERENCES users (id) NOT NULL,
  url         TEXT NOT NULL,
  description TEXT /* TODO: not null or make it fail doobie test? */
)
