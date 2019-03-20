DROP TABLE IF EXISTS public.entity;
CREATE TABLE public.entity
(
  name character varying not null,
  id serial NOT NULL,
  count integer,
  CONSTRAINT entity_pkey PRIMARY KEY (id)
);
ALTER TABLE public.entity
  OWNER TO slimuser;


DROP TABLE IF EXISTS public.entity_with_types;
CREATE TABLE public.entity_with_types
(
  id serial NOT NULL,
  f_string character varying,
  f_byte1 smallint,
  f_byte2 smallint,
  f_short1 smallint,
  f_short2 smallint,
  f_int1 integer,
  f_int2 integer,
  f_long1 bigint,
  f_long2 bigint,
  f_float1 real,
  f_float2 real,
  f_double1 double precision,
  f_double2 double precision,
  f_big_decimal numeric,
  f_byte_array bytea,
  f_timestamp timestamp without time zone,
  f_instant timestamp without time zone,
  f_zoned_date_time timestamp with time zone,
  f_local_date date,
  f_local_date_time timestamp without time zone,
  f_json1 json,
  f_json2 json,
  CONSTRAINT entity_with_types_pkey PRIMARY KEY (id)
);
ALTER TABLE public.entity_with_types
  OWNER TO slimuser;

DROP TABLE IF EXISTS public.entity_fkey;
CREATE TABLE public.entity_fkey
(
  id serial NOT NULL,
  name character varying,
  entity_id integer,
  CONSTRAINT entity_fkey_pkey PRIMARY KEY (id)
);