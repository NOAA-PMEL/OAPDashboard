-- Table: users

-- DROP TABLE users;

CREATE TABLE users
(
  db_id serial NOT NULL,
  create_time time with time zone NOT NULL DEFAULT now(),
  mod_time timestamp with time zone NOT NULL DEFAULT now(),
  username character varying(128) NOT NULL,
  first_name character varying(128) NOT NULL,
  last_name character varying(128) NOT NULL,
  email character varying(128),
  CONSTRAINT users_pk PRIMARY KEY (db_id),
  CONSTRAINT users_unq_username UNIQUE (username)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE users
  OWNER TO oapweb;
