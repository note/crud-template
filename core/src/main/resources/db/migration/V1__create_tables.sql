CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id       UUID DEFAULT uuid_generate_v4 (),
  email    VARCHAR(255),
  phone    VARCHAR(30),
  password CHAR(60),
  PRIMARY KEY(id),
  UNIQUE (email)
);

CREATE TABLE bookmarks (
  id          UUID DEFAULT uuid_generate_v4 (),
  user_id     uuid REFERENCES users (id),
  url         TEXT,
  description TEXT
)
